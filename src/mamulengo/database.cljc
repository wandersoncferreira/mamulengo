(ns mamulengo.database
  #?@(:clj
      [(:require [mamulengo.durability :as du]
                 [mamulengo.durable.h2-impl :refer :all]
                 [mamulengo.durable.pg-impl :refer :all]
                 [mount.core :refer [defstate]]
                 [datascript.core :as ds]
                 [mamulengo.config :as config])]
      :cljs
      [(:require-macros [mount.core :refer [defstate]])
       (:require [mamulengo.config :as config]
                 [datascript.core :as ds]
                 [mamulengo.durability :as du])]))

(declare listen-tx!)

(defn- recover-datascript
  "Recover the datascript session from durable storage."
  []
  (let [facts (du/retrieve-all-facts! config/mamulengo-cfg)
        schema (du/get-system-schema! config/mamulengo-cfg)]
    (ds/conn-from-datoms facts schema)))

(defn- start-datascript []
  (let [conn (recover-datascript)
        sync (atom @conn)]
    {:conn conn
     :sync sync
     :listener (ds/listen! conn listen-tx!)}))

(defn- stop-datascript
  [{:keys [conn listener]}]
  (ds/unlisten! conn listener))

(defstate ds-state
  :start (start-datascript)
  :stop (stop-datascript ds-state))

(defstate durable-layer
  :start (do
           (du/create-system-tables! config/mamulengo-cfg)
           (:durable-conf config/mamulengo-cfg))
  :stop identity)

(defn- listen-tx!
  [{:keys [db-before db-after tx-data tempids tx-metada]}]
  (when (not= db-after db-before)
    (let [{:keys [durable-conf durable-layer]} config/mamulengo-cfg
          stored (du/store! {:durable-layer durable-layer
                             :durable-conf durable-conf
                             :tempids (:db/current-tx tempids)
                             :tx-data tx-data
                             :tx-meta tx-metada})]
      (if stored
        (reset! (:sync ds-state) db-after)
        (reset! (:conn ds-state) db-before)))))

(defn transact-schema!
  [tx]
  (let [config-tx (assoc config/mamulengo-cfg :durable-schema tx)]
    (du/setup-clients-schema! config-tx)))

(defn transact!
  ([tx] (transact! tx nil))
  ([tx metadata]
   (let [tx-seq (if (map? tx) (list tx) tx)]
     (ds/transact! (:conn ds-state) tx-seq metadata))))

(defn query!
  [query inputs]
  (apply ds/q query (cons @(:sync ds-state) inputs)))
