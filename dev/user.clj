(ns user
  (:require [mount.core :as mount]
            [clojure.tools.namespace.repl :as tn]
            [mamulengo.database :as database]
            [mamulengo.config :as config]))

(defn start [config]
  (mount/in-cljc-mode)
  (-> (mount.core/only #{#'config/mamulengo-cfg
                         #'database/ds-state
                         })
      (mount.core/with-args config)
      (mount.core/start)))

(defn stop []
  (mount/stop))

(defn refresh []
  (stop)
  (tn/refresh))

(defn go [config]
  (start config)
  :ready)

(defn reset []
  (stop)
  (tn/refresh :after 'user/go))
