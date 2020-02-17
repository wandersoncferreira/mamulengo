(ns user
  (:require [mount.core :as mount]
            [clojure.tools.namespace.repl :as tn]
            [mamulengo.database :as database]
            [mamulengo.config :as config]
            [mamulengo.durability :as durable]))

(defn start [config]
  (-> (mount.core/only #{#'config/mamulengo-cfg
                         #'durable/durable-layer
                         #'database/ds-state
                         })
      (mount.core/with-args config)
      (mount.core/start)))

(defn stop []
  (mount/stop))

(defn refresh []
  (stop)
  (tn/refresh))

(defn go []
  (start)
  :ready)

(defn reset []
  (stop)
  (tn/refresh :after 'user/go))
