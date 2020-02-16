(ns mamulengo.core
  (:require [mount.core :as mount]
            [mamulengo.database :as database]
            [mamulengo.durability :as durability]
            [mamulengo.config :as config]))

(defn connect! [config]
  (-> (mount.core/only #{#'config/mamulengo-cfg
                         #'durability/durable-layer
                         #'database/ds-state})
      (mount.core/with-args config)
      (mount.core/start)))

(defn disconnect! []
  (mount/stop))

(def query! database/query!)
(def transact! database/transact!)



(comment

  (def cfg {:durable-layer :h2
            :durable-conf {:dbtype "h2"
                           :dbname "mamulengo"}
            :durable-schema {:empl/dept {:db/cardinality :db.cardinality/many}
                             :empl/salary {:db/cardinality :db.cardinality/many}}})

  (connect! cfg)
  (disconnect!)

  ;; testing my implementation
  (def tx [{:empl/salary 2032
            :empl/dept "TEH"}])

  (transact! tx)

  (query! '[:find ?e ?d ?s
            :in $
            :where
            [?e :empl/dept ?d]
            [?e :empl/salary ?s]]
          nil)
  )
