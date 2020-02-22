(ns mamulengo.utils
  #?@(:clj
      [(:require [datascript.core :as ds]
                 [next.jdbc :as jdbc]
                 [clojure.edn :as edn]
                 [mount.core :as mount]
                 [mamulengo.config :as config])]
      :cljs
      [(:require [mamulengo.config :as config]
                 [mount.core :as mount])]))

(defn check-state! []
  (when (empty? @config/mamulengo-cfg)
    (throw (ex-info "Mamulengo is disconnected" {:cause :disconnected}))))

(defmacro try-return
  "Executes body and returns exception as value"
  [& body]
  `(try
     ~@body
     (catch ~(if (:ns &env) 'js/Error 'Exception) e#
       (mount/stop)
       (if (= :disconnected (:cause (ex-data e#)))
         (ex-message e#)
         e#))))

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
  (letfn [(vec-remove [pos coll]
            (if pos
              (vec (concat (subvec coll 0 pos) (subvec coll (inc pos))))
              coll))
          (index-of [coll v]
            (letfn [(f [x] (= v x))]
              (first (keep-indexed #(if (f %2) %1 -1) coll))))
          (pos-opposite-status [status list-statuses]
            (let [st-ops (if (true? status) false status)
                  idx #?(:cljs (.indexOf list-statuses st-ops)
                         :clj (index-of list-statuses st-ops))]
              (when (>= idx 0)
                idx)))]
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
         (into []))))


#?(:clj (defn sql-datoms-formatting
          [sql-conf sql-query]
          (jdbc/with-transaction [tx (jdbc/get-datasource sql-conf)]
            (let [datoms (jdbc/execute! tx sql-query)]
              (->> datoms
                   (map :mamulengo/datoms)
                   (mapcat #(edn/read-string {:readers ds/data-readers} %))
                   bring-back-consistent-database)))))
