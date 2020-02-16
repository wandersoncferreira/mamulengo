(ns mamulengo.durability
  (:require [potemkin :refer [import-vars]]
            [mount.core :refer [defstate]]
            [mamulengo.durable.h2-impl :refer :all]
            [mamulengo.config :as config]))

(import-vars
 [mamulengo.durable.interface
  create-system-tables!
  setup-clients-schema!
  retrieve-all-facts!
  get-system-schema!
  store!
  ])

(defn- start-durable-layer []
  (create-system-tables! config/mamulengo-cfg)
  (setup-clients-schema! config/mamulengo-cfg)
  (:durable-conf config/mamulengo-cfg))

(defstate durable-layer
  :start (start-durable-layer)
  :stop identity)
