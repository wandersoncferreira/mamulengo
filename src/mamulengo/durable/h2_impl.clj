(ns mamulengo.durable.h2-impl
  (:require [clojure.edn :as edn]
            [datascript.core :as ds]
            [mamulengo.durability :refer [create-system-tables!
                                          get-system-schema!
                                          retrieve-all-facts!
                                          setup-clients-schema!
                                          store!]]
            [next.jdbc :as jdbc]
            [next.jdbc.sql :as sql]
            [taoensso.nippy :as nippy]))

(def ^:private sql-create-txs-table
  ["create table if not exists TX (
id integer AUTO_INCREMENT PRIMARY KEY,
tx integer NOT NULL UNIQUE,
instant timestamp DEFAULT NOW() NOT NULL,
meta varchar(2048),
)"])

(def ^:private sql-create-fact-table
  ["create table if not exists MAMULENGO (
id integer AUTO_INCREMENT PRIMARY KEY,
datoms varchar(4096),
tx integer NOT NULL UNIQUE,
FOREIGN KEY (tx) REFERENCES tx(id)
)"])

(def ^:private sql-create-schemas-table
  ["create table if not exists DBSCHEMA (
id integer PRIMARY KEY AUTO_INCREMENT,
instant timestamp DEFAULT NOW() NOT NULL,
nippy binary
)"])

(def ^:private sql-get-last-schema
  ["select top 1 nippy from dbschema order by instant desc"])

(defmethod create-system-tables! :h2
  [{:keys [durable-conf]}]
  (jdbc/with-transaction [tx (jdbc/get-datasource durable-conf)]
    (jdbc/execute! tx sql-create-txs-table)
    (jdbc/execute! tx sql-create-fact-table)
    (jdbc/execute! tx sql-create-schemas-table)))

(defmethod get-system-schema! :h2
  [{:keys [durable-conf]}]
  (some-> durable-conf
          (jdbc/execute! sql-get-last-schema)
          first
          :DBSCHEMA/NIPPY
          nippy/thaw))

(defmethod setup-clients-schema! :h2
  [{:keys [durable-conf durable-schema] :as conf}]
  (jdbc/with-transaction [tx durable-conf]
    (let [last-schema (get-system-schema! conf)]
      (when (and durable-schema
                 (not= durable-schema last-schema))
        (sql/insert! tx :dbschema {:nippy (nippy/freeze durable-schema)})))))

(defmethod retrieve-all-facts! :h2
  [{:keys [durable-conf]}]
  (let [facts (jdbc/execute! durable-conf ["select * from mamulengo"])
        xf (comp (map :MAMULENGO/DATOMS)
                 (mapcat #(edn/read-string {:readers ds/data-readers} %)))]
    (into [] xf facts)))

(defmethod store! :h2
  [{:keys [durable-conf tempids tx-meta tx-data]}]
  (jdbc/with-transaction [tx (jdbc/get-datasource durable-conf)]
    (let [tx-result (sql/insert! tx :tx {:tx tempids
                                         :meta (if tx-meta (pr-str tx-meta) nil)})
          tx-id (:TX/ID tx-result)]
      (sql/insert! tx :mamulengo {:datoms (pr-str tx-data) :tx tx-id})
      tx-id)))
