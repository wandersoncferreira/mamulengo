(ns mamulengo.durable.local-storage-impl
  #?@(:cljs
      [(:require [mamulengo.durability :refer [create-system-tables!
                                               get-system-schema!
                                               setup-clients-schema!
                                               retrieve-all-facts!
                                               get-database!
                                               store!]]
                 [hodgepodge.core :refer [local-storage]]
                 [cljs-time.core :as t]
                 [cljs-time.coerce :as tc]
                 [clojure.edn :as edn]
                 [datascript.core :as ds])]))

(defn get-last-id [table]
  (letfn [(last-id [table]
            (apply max (map :id table)))]
    (if (empty? table)
      0
      (last-id table))))

(defmethod create-system-tables! :local-storage
  [_]
  (when-not (get local-storage :table-tx)
    (assoc! local-storage :table-tx []))

  (when-not (get local-storage :table-schema)
    (assoc! local-storage :table-schema []))

  (when-not (get local-storage :table-mamulengo)
    (assoc! local-storage :table-mamulengo [])))

(defmethod get-system-schema! :local-storage
  [_]
  (let [table-schema (get local-storage :table-schema)
        last-id (get-last-id table-schema)
        last-schema (:nippy (first (filter #(= last-id (:id %)) table-schema)))]
    (if-not (empty? last-schema)
      (edn/read-string last-schema)
      {})))

(defmethod setup-clients-schema! :local-storage
  [{:keys [durable-schema] :as conf}]
  (let [table-schema (get local-storage :table-schema)
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
  (let [table-tx (get local-storage :table-tx)
        last-id-tx (get-last-id table-tx)
        next-id-tx (inc last-id-tx)
        table-mamulengo (get local-storage :table-mamulengo)
        last-id-mamulengo (get-last-id table-mamulengo)]
    
    (assoc! local-storage :table-tx (conj table-tx
                                          {:id next-id-tx
                                           :tx tempids
                                           :instant (js/Date.)
                                           :meta (if tx-meta (pr-str tx-meta) nil)}))
    
    (assoc! local-storage :table-mamulengo (conj table-mamulengo
                                                 {:id (inc last-id-mamulengo)
                                                  :datoms (pr-str tx-data)
                                                  :tx next-id-tx}))
    next-id-tx))

(defn- compare-dates [inst1 inst2]
  (let [date1 (tc/from-date inst1)
        date2 (tc/from-date inst2)]
    (or (t/after? date1 date2)
        (t/equal? date1 date2))))

(defmethod get-database! :local-storage
  [{:keys [instant]}]
  (let [table-tx (get local-storage :table-tx)
        desired-tx (last (filter #(= true (compare-dates instant (:instant %))) table-tx))
        table-mamulengo (get local-storage :table-mamulengo)]
    (->> table-mamulengo
         (filter #(= (:id desired-tx) (:tx %)))
         first
         :datoms
         (edn/read-string {:readers ds/data-readers}))))
