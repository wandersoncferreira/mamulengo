(ns mamulengo.internals
  #?@(:clj
      [(:require [mamulengo.durability :as du]
                 [mamulengo.durable.h2-impl :refer :all]
                 [mamulengo.durable.pg-impl :refer :all]
                 [mount.core :refer [defstate] :as mount]
                 [datascript.core :as ds]
                 [datascript.db :as db]
                 [mamulengo.config :as config]
                 [mamulengo.utils :refer [try-return] :as utils])]
      :cljs
      [(:require-macros [mount.core :refer [defstate]]
                        [mamulengo.utils :refer [try-return]])
       (:require [mamulengo.config :as config]
                 [mamulengo.utils :as utils]
                 [mount.core :as mount]
                 [datascript.core :as ds]
                 [datascript.db :as db]
                 [mamulengo.durability :as du]
                 [mamulengo.durable.local-storage-impl])]))

(declare listen-tx!)

(defn- setup-durability-layer [conf]
  (du/create-system-tables! conf)
  (du/setup-clients-schema! conf)
  {:facts (utils/bring-back-consistent-database (du/retrieve-all-facts! conf))
   :schema (du/get-system-schema! conf)})

(defn- start-datascript []
  (let [{:keys [durable-layer] :as conf} @config/mamulengo-cfg]
    ;; turn off durability layer
    (if (= durable-layer :off)
      (let [conn (ds/create-conn {})]
        {:conn conn
         :sync (atom @conn)
         :timestamp-last-tx (atom nil)
         :listener (ds/listen! conn listen-tx!)})

      (let [{:keys [facts schema]} (setup-durability-layer conf)
            conn (ds/conn-from-datoms facts schema)]
        {:conn conn
         :sync (atom @conn)
         :timestamp-last-tx (atom nil)
         :listener (ds/listen! conn listen-tx!)}))))

(defn- stop-datascript
  [{:keys [conn listener]}]
  (ds/unlisten! conn listener))

(defstate ds-state
  :start (start-datascript)
  :stop (stop-datascript @ds-state))

(defn- listen-tx!
  [{:keys [db-before db-after tx-data tempids tx-metada]}]
  (let [{:keys [durable-conf durable-storage durable-layer]} @config/mamulengo-cfg]
    (if (= durable-layer :off)
      (reset! (:sync @ds-state) db-after)

      (when (not= db-after db-before)
        (let [timestamp-stored (du/store! {:durable-storage durable-storage
                                           :durable-conf durable-conf
                                           :tempids (:db/current-tx tempids)
                                           :tx-data tx-data
                                           :tx-meta tx-metada})]
          (if timestamp-stored
            (do
              (reset! (:timestamp-last-tx @ds-state) timestamp-stored)
              (reset! (:sync @ds-state) db-after))
            (reset! (:conn @ds-state) db-before)))))))

(defn transact!
  ([tx] (transact! tx nil))
  ([tx metadata]
   (try-return

    ;; verify if mamulengo is connected
    (utils/check-state!)

    (let [tx-seq (if (map? tx) (list tx) tx)
          ret-tx (ds/transact! (:conn @ds-state) tx-seq metadata)]
      (assoc ret-tx :timestamp @(:timestamp-last-tx @ds-state))))))

(defn- get-conn [arg]
  (cond
    (ds/conn? arg) @arg
    (db/db? arg) @(ds/conn-from-db arg)
    :else @(:sync @ds-state)))

(defn- is-inputs? [arg]
  (if (or (ds/conn? arg) (db/db? arg))
    false
    true))

(defn query!
  [query & args]
  (try-return

   ;; verify if mamulengo is connected
   (utils/check-state!)

   (let [connection (get-conn (first args))
         inputs (if (is-inputs? (first args)) args (rest args))]
     (apply ds/q query (cons connection inputs)))))

(defn db-as-of!
  [instant]
  (try-return

   ;; verify if mamulengo is connected
   (utils/check-state!)

   (let [datoms (du/datoms-as-of! (assoc @config/mamulengo-cfg :instant instant))
         schema (du/get-schema-at! (assoc @config/mamulengo-cfg :instant instant))]
     @(ds/conn-from-datoms datoms schema))))

(defn db-since!
  [instant]
  (try-return

   ;; verify if mamulengo is connected
   (utils/check-state!)

   (let [datoms (du/datoms-since! (assoc @config/mamulengo-cfg :instant instant))
         schema (du/get-system-schema! (assoc @config/mamulengo-cfg :instant instant))]
     @(ds/conn-from-datoms datoms schema))))


(defn db-history
  []
  (try-return

   ;; verify if mamulengo is connected
   (utils/check-state!)

   (let [datoms (du/retrieve-all-facts! @config/mamulengo-cfg)
         schema (du/get-system-schema! @config/mamulengo-cfg)]
     @(ds/conn-from-datoms datoms schema))))
