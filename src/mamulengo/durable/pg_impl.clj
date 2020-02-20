(ns mamulengo.durable.pg-impl
  (:require [mamulengo.durability :refer [create-system-tables!
                                          get-system-schema!
                                          get-schema-at!
                                          retrieve-all-facts!
                                          setup-clients-schema!
                                          datoms-as-of!
                                          datoms-since!
                                          store!]]
            [mamulengo.utils :as utils]
            [next.jdbc :as jdbc]
            [taoensso.nippy :as nippy]
            [next.jdbc.sql :as sql]
            [clojure.edn :as edn]
            [datascript.core :as ds]))

(def ^:private sql-create-txs-table
  ["create table if not exists TX (
id serial PRIMARY KEY,
tx integer NOT NULL UNIQUE,
instant timestamp DEFAULT NOW() NOT NULL,
meta varchar(2048))"])

(def ^:private sql-create-fact-table
  ["create table if not exists MAMULENGO (
id serial PRIMARY KEY,
datoms varchar(4096),
tx integer NOT NULL UNIQUE,
FOREIGN KEY (tx) REFERENCES tx(id))"])

(def ^:private sql-create-schemas-table
  ["create table if not exists DBSCHEMA (
id serial PRIMARY KEY,
instant timestamp DEFAULT NOW() NOT NULL,
nippy bytea)"])

(def ^:private sql-get-last-schema
  ["select nippy from dbschema order by instant desc limit 1"])

(defn clear-pg [{:keys [durable-conf]}]
  (let [truncate-tx ["truncate table TX cascade;"]
        truncate-schema ["truncate table DBSCHEMA cascade;"]
        truncate-mamulengo ["truncate table MAMULENGO cascade;"]]
    (jdbc/with-transaction [tx (jdbc/get-datasource durable-conf)]
      (jdbc/execute! tx truncate-tx)
      (jdbc/execute! tx truncate-mamulengo)
      (jdbc/execute! tx truncate-schema))))

(defmethod create-system-tables! :postgresql
  [{:keys [durable-conf]}]
  (jdbc/with-transaction [tx (jdbc/get-datasource durable-conf)]
    (jdbc/execute! tx sql-create-txs-table)
    (jdbc/execute! tx sql-create-fact-table)
    (jdbc/execute! tx sql-create-schemas-table)))

(defmethod get-system-schema! :postgresql
  [{:keys [durable-conf]}]
  (some-> durable-conf
          (jdbc/execute! sql-get-last-schema)
          first
          :dbschema/nippy
          nippy/thaw))

(defmethod setup-clients-schema! :postgresql
  [{:keys [durable-conf durable-schema] :as conf}]
  (jdbc/with-transaction [tx durable-conf]
    (let [last-schema (get-system-schema! conf)]
      (when (and durable-schema
                 (not= durable-schema last-schema))
        (sql/insert! tx :dbschema {:nippy (nippy/freeze durable-schema)})))))

(defmethod retrieve-all-facts! :postgresql
  [{:keys [durable-conf]}]
  (let [facts (jdbc/execute! durable-conf ["select * from mamulengo"])
        xf (comp (map :mamulengo/datoms)
                 (mapcat #(edn/read-string {:readers ds/data-readers} %)))]
    (into [] xf facts)))

(defmethod store! :postgresql
  [{:keys [durable-conf tempids tx-meta tx-data]}]
  (jdbc/with-transaction [tx (jdbc/get-datasource durable-conf)]
    (let [tx-result (sql/insert! tx :tx {:tx tempids
                                         :meta (if tx-meta (pr-str tx-meta) nil)})
          tx-id (:tx/id tx-result)]
      (sql/insert! tx :mamulengo {:datoms (pr-str tx-data) :tx tx-id})
      (:tx/instant tx-result))))

(def ^:private sql-datoms-as-of
  "select datoms from mamulengo as m inner join tx as t on m.tx=t.id
where t.instant <= ?")

(def ^:private sql-datoms-since
  "select datoms from mamulengo as m inner join tx as t on m.tx=t.id
where t.instant >= ?")

(def ^:private sql-schema-at
  "select nippy from dbschema where instant <= ?")

(defmethod datoms-as-of! :postgresql
  [{:keys [durable-conf instant]}]
  (utils/sql-datoms-formatting durable-conf [sql-datoms-as-of instant]))

(defmethod datoms-since! :postgresql
  [{:keys [durable-conf instant]}]
  (utils/sql-datoms-formatting durable-conf [sql-datoms-since instant]))

(defmethod get-schema-at! :postgresql
  [{:keys [durable-conf instant]}]
  (jdbc/with-transaction [tx (jdbc/get-datasource durable-conf)]
    (let [schema (->> [sql-schema-at instant]
                      (jdbc/execute! tx)
                      last
                      :dbschema/nippy)]
      (if-not (empty? schema)
        (nippy/thaw schema)
        {}))))
