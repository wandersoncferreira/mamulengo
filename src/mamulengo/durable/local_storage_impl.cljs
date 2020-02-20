(ns mamulengo.durable.local-storage-impl
  (:require [mamulengo.durability :refer [create-system-tables!
                                          get-system-schema!
                                          get-schema-at!
                                          setup-clients-schema!
                                          retrieve-all-facts!
                                          datoms-since!
                                          datoms-as-of!
                                          store!]]
            [mamulengo.specs.system-table :as st]
            [hodgepodge.core :refer [local-storage clear!]]
            [cljs-time.core :as t]
            [cljs-time.coerce :as tc]
            [clojure.edn :as edn]
            [datascript.core :as ds]
            [clojure.spec.alpha :as s]))

(defn get-last-id [table]
  (letfn [(last-id [table]
            (apply max (map :id table)))]
    (if (empty? table)
      0
      (last-id table))))

(defn clear []
  (clear! local-storage))

(defn read-storage
  "Read the local storage and spec the content to verify if anything anomalous is in there.

  :table-name    Name of the system table to be read from local storage."
  [table-name]
  (let [specs {:table-tx ::st/table-tx
               :table-schema ::st/table-schema
               :table-mamulengo ::st/table-mamulengo}
        tb (get local-storage table-name)
        valid? (s/valid? (get specs table-name) tb)]
    (if (or valid? (empty? tb))
      tb
      (throw (ex-info "Your durable storage is corrupted"
                      (s/explain-data (get specs table-name) tb))))))

(defmethod create-system-tables! :local-storage
  [_]
  (when-not (read-storage :table-tx)
    (assoc! local-storage :table-tx []))

  (when-not (read-storage :table-schema)
    (assoc! local-storage :table-schema []))

  (when-not (read-storage :table-mamulengo)
    (assoc! local-storage :table-mamulengo [])))

(defmethod get-system-schema! :local-storage
  [_]
  (let [table-schema (read-storage :table-schema)
        last-id (get-last-id table-schema)
        last-schema (:nippy (first (filter #(= last-id (:id %)) table-schema)))]
    (if-not (empty? last-schema)
      (edn/read-string last-schema)
      {})))

(defmethod setup-clients-schema! :local-storage
  [{:keys [durable-schema] :as conf}]
  (let [table-schema (read-storage :table-schema)
        last-id (get-last-id table-schema)
        last-schema (get-system-schema! conf)]
    (when (and durable-schema
               (not= last-schema durable-schema))
      (assoc! local-storage :table-schema (conj table-schema
                                                {:id (inc last-id)
                                                 :instant (js/Date.)
                                                 :nippy (pr-str durable-schema)})))))

(defmethod retrieve-all-facts! :local-storage
  [_]
  (into [] (mapcat #(edn/read-string {:readers ds/data-readers} %)
                   (map :datoms (get local-storage :table-mamulengo)))))

(defmethod store! :local-storage
  [{:keys [tempids tx-meta tx-data]}]
  (let [table-tx (read-storage :table-tx)
        last-id-tx (get-last-id table-tx)
        next-id-tx (inc last-id-tx)
        table-mamulengo (read-storage :table-mamulengo)
        last-id-mamulengo (get-last-id table-mamulengo)
        instant (js/Date.)]
    
    (assoc! local-storage :table-tx (conj table-tx
                                          {:id next-id-tx
                                           :tx tempids
                                           :instant instant
                                           :meta (if tx-meta (pr-str tx-meta) nil)}))
    
    (assoc! local-storage :table-mamulengo (conj table-mamulengo
                                                 {:id (inc last-id-mamulengo)
                                                  :datoms (pr-str tx-data)
                                                  :tx next-id-tx}))
    instant))

(defn- compare-dates
  "Verify if the instant1 happens after or at the same time as instant2"
  [inst1 inst2]
  (let [date1 (tc/from-date inst1)
        date2 (tc/from-date inst2)]
    (or (t/after? date1 date2)
        (t/equal? date1 date2))))

(defn pos-opposite-status
  [status list-statuses]
  (let [st-ops (if (true? status) false status)
        idx (.indexOf list-statuses st-ops)]
    (when (>= idx 0)
      idx)))

(defn vec-remove
  "remove elem in coll"
  [pos coll]
  (if pos
    (vec (concat (subvec coll 0 pos) (subvec coll (inc pos))))
    coll))

(defn bring-back-consistent-database
  "There is a problem when some datoms has been added and retracted from different databases across time.

  This function manages to bring back consistency to the loaded database by removing datoms that cancel each other
  The following example was happening and it should not!:

  (ds/conn-from-datoms [#datascript/Datom [1 :maker/name 'BMW' 536870913 true]
                        #datascript/Datom [1 :maker/name 'BMW' 536870914 false]])
  #datascript/DB{:datoms [[1 :maker/name BMW 536870913] [1 :maker/name BMW 536870914]]}

  The correct answer here is none. The datascript/DB should be empty after loading those datoms.
  "
  [datoms]
  (->> datoms
       (group-by (juxt first second #(nth % 2)))
       (map (fn [[_ v]]
              (if (= (count v) 1)
                v
                (loop [original v
                       accepted []]
                  (if (empty? original)
                    accepted
                    (let [status (last (first original))
                          idx (pos-opposite-status status (map last (rest original)))
                          new-original (vec-remove idx (vec (rest original)))]
                      (recur new-original (if (nil? idx) (cons (first original) accepted) accepted))))))))
       (filter not-empty)
       (apply concat)
       (into [])))

(defmethod datoms-as-of! :local-storage
  [{:keys [instant]}]
  (let [table-tx (read-storage :table-tx)
        desired-txs (filter #(= true (compare-dates instant (:instant %))) table-tx)
        desired-ids (set (map :id desired-txs))
        table-mamulengo (read-storage :table-mamulengo)]
    (->> table-mamulengo
         (filter #(contains? desired-ids (:tx %)))
         (map :datoms)
         (map #(edn/read-string {:readers ds/data-readers} %))
         (apply concat)
         bring-back-consistent-database)))

(defmethod get-schema-at! :local-storage
  [{:keys [instant]}]
  (let [table-schema (read-storage :table-schema)
        desired-schema (last (filter #(= true (compare-dates instant (:instant %))) table-schema))
        schema (:nippy desired-schema)]
    (if-not (empty? schema)
      (edn/read-string schema)
      {})))

(defmethod datoms-since! :local-storage
  [{:keys [instant]}]
  (let [table-tx (read-storage :table-tx)
        desired-txs (filter #(= true (compare-dates (:instant %) instant)) table-tx)
        desired-ids (set (map :id desired-txs))
        table-mamulengo (read-storage :table-mamulengo)]
    (->> table-mamulengo
         (filter #(contains? desired-ids (:tx %)))
         (map :datoms)
         (map #(edn/read-string {:readers ds/data-readers} %))
         (apply concat)
         bring-back-consistent-database)))
