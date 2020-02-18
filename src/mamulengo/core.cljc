(ns mamulengo.core
  #?@(:clj
      [(:require [mount.core :refer [only with-args start stop in-cljc-mode]]
                 [mamulengo.database :as database]
                 [mamulengo.config :as config])]
      :cljs
      [(:require [mamulengo.config :as config]
                 [mount.core :refer [only with-args start stop in-cljc-mode]]
                 [mamulengo.database :as database])]))

(defn connect! [config]
  (in-cljc-mode)
  (-> (only #{#'config/mamulengo-cfg
              #'database/durable-layer
              #'database/ds-state
              })
      (with-args config)
      (start)))

(defn disconnect! []
  (stop))

(def query! database/query!)
(def transact! database/transact!)
(def transact-schema! database/transact-schema!)
