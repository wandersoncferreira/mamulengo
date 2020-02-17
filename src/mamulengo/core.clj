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
(def transact-schema! database/transact-schema!)
