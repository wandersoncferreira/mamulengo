(ns mamulengo.core
  #?@(:clj
      [(:require [mamulengo.config :as config]
                 [mamulengo.specs.config :as specs-config]
                 [mamulengo.internals :as internals]
                 [mount.core :as mount]
                 [clojure.spec.alpha :as s])]
      :cljs
      [(:require [mamulengo.config :as config]
                 [mamulengo.specs.config :as specs-config]
                 [mamulengo.internals :as internals]
                 [clojure.spec.alpha :as s]
                 [mount.core :as mount])]))

(mount/in-cljc-mode)

(defn connect!
  "Connect to mamulengo. If you do not provide any configuration,
  a default will be used.

  - default config: ClojureScript uses Local Storage and Clojure uses H2 Database.

  :config       Map with specifications to the durable target object.
  :schema       Same as datascript schema maps."
  ([]
   (let [config #?(:clj {:durable-storage :h2
                         :durable-conf {:dbtype "h2:mem"
                                        :dbname "mamulengo"}}
                   :cljs {:durable-storage :local-storage})]
     (connect! config)))
  ([config]
   (connect! config {}))
  ([config schema]
   {:pre [(s/valid? ::specs-config/config config)]}
   (-> #{#'config/mamulengo-cfg #'internals/ds-state}
       (mount/only)
       (mount/with-args (assoc config :durable-schema schema))
       (mount/start))))

(defn disconnect! []
  (mount/stop))

(def transact! internals/transact!)

(def query! internals/query!)

(def as-of! internals/db-as-of!)

(def since! internals/db-since!)

(def history! internals/db-history)
