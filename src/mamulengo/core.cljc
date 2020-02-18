(ns mamulengo.core
  #?@(:clj
      [(:require [mamulengo.config]
                 [mamulengo.specs.config :as config]
                 [mamulengo.database :as database]
                 [mount.core :as mount]
                 [clojure.spec.alpha :as s])]
      :cljs
      [(:require [mamulengo.config]
                 [mamulengo.specs.config :as config]
                 [mamulengo.database :as database]
                 [clojure.spec.alpha :as s]
                 [mount.core :as mount])]))

(defn connect!
  ([config]
   (connect! config {}))
  ([config schema]
   {:pre [(s/valid? ::config/config config)]}
   (mount/in-cljc-mode)
   (-> (mount/with-args (assoc config :durable-schema schema))
       (mount/start))))

(defn disconnect! []
  (mount/stop))

(def
  transact! database/transact!)

(def query! database/query!)

(def get-database! database/get-database!)
