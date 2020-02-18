(ns mamulengo.core
  #?@(:clj
      [(:require [mamulengo.config]
                 [mamulengo.database :as database]
                 [mount.core :as mount])]
      :cljs
      [(:require [mamulengo.config]
                 [mamulengo.database :as database]
                 [mount.core :as mount])]))

(defn connect! [config]
  (mount/in-cljc-mode)
  (-> (mount/with-args config)
      (mount/start)))

(defn disconnect! []
  (mount/stop))

(def query! database/query!)
(def transact! database/transact!)
(def transact-schema! database/transact-schema!)
