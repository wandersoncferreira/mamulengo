(ns mamulengo.database
  #?@(:clj
      [(:require [mamulengo.durability :as du]
                 [mamulengo.durable.h2-impl :refer :all]
                 [mamulengo.durable.pg-impl :refer :all]
                 [mount.core :refer [defstate] :as mount]
                 [datascript.core :as ds]
                 [mamulengo.config :as config])]
      :cljs
      [(:require-macros [mount.core :refer [defstate]])
       (:require [mamulengo.config :as config]
                 [mount.core :as mount]
                 [datascript.core :as ds]
                 [mamulengo.durability :as du]
                 [mamulengo.durable.local-storage-impl])]))

(declare listen-tx!)

(defn- setup-durability-layer [conf]
  (du/create-system-tables! conf)
  {:facts (du/retrieve-all-facts! conf)
   :schema (du/get-system-schema! conf)})

(defn- start-datascript []
  (let [{:keys [durable-layer] :as conf} @config/mamulengo-cfg]
    ;; turn off durability layer
    (if (= durable-layer :off)
      (let [conn (ds/create-conn {})]
        {:conn conn
         :sync (atom @conn)
         :listener (ds/listen! conn listen-tx!)})

      (let [{:keys [facts schema]} (setup-durability-layer conf)
            conn (ds/conn-from-datoms facts schema)]
        {:conn conn
         :sync (atom @conn)
         :listener (ds/listen! conn listen-tx!)}))))

(defn- stop-datascript
  [{:keys [conn listener]}]
  (ds/unlisten! conn listener))

(defstate ds-state
  :start (start-datascript)
  :stop (stop-datascript @ds-state))

(defn- listen-tx!
  [{:keys [db-before db-after tx-data tempids tx-metada]}]

  (if (= (:durable-layer @config/mamulengo-cfg) :off)
    (reset! (:sync @ds-state) db-after)

    (when (not= db-after db-before)
      (let [{:keys [durable-conf durable-storage]} @config/mamulengo-cfg
            stored (du/store! {:durable-storage durable-storage
                               :durable-conf durable-conf
                               :tempids (:db/current-tx tempids)
                               :tx-data tx-data
                               :tx-meta tx-metada})]
        (if stored
          (reset! (:sync @ds-state) db-after)
          (reset! (:conn @ds-state) db-before))))))

;;; TODO: add schema when only datascript is present in the system
(defn transact-schema!
  [tx]
  (let [config-tx (assoc @config/mamulengo-cfg :durable-schema tx)]
    (when-not (= (:durable-layer @config/mamulengo-cfg) :off)
      (du/setup-clients-schema! config-tx))))

(defn transact!
  ([tx] (transact! tx nil))
  ([tx metadata]
   (let [tx-seq (if (map? tx) (list tx) tx)]
     (ds/transact! (:conn @ds-state) tx-seq metadata))))

(defn query!
  [query inputs]
  (apply ds/q query (cons @(:sync @ds-state) inputs)))
