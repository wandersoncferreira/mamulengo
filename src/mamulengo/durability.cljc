(ns mamulengo.durability)

(defmulti create-system-tables!
  "Create mamulengo tables to enable durability.

  Right now you should create at least 3 tables:
  :table-tx         Stores metadata information about the transaction e.g. instant, id.
  :table-schema     Stores all the versions of the schema used by clients code.
  :table-mamulengo  Stores information about the datoms of the transactions.

  All the details about the field necessary are documented as specs in the
  `mamulengo.specs-system-table` namespace."
  :durable-storage)

(defmulti setup-clients-schema!
  "When start a new connection, stores the new schema if necessary.

  It requires a comparison with the previous stored schema in the durable
  storage to decide if we must keep the provided schema or ignore.
  "
  :durable-storage)

(defmulti retrieve-all-facts!
  "Simply select all the datoms from the mamulengo table."
  :durable-storage)

(defmulti get-system-schema!
  "Return a map of the last schema stored."
  :durable-storage)

(defmulti get-schema-at!
  "Return a map of the schema stored at a specific instant of time."
  :durable-storage)

(defmulti store!
  "Write the transaction to the durable storage.

  It is important that the returned value of this function be the
  `timestamp` saved in the metadata of `:table-tx`. This will be used
  to enrich the default return value of datascript."
  :durable-storage)

(defmulti datoms-as-of!
  "Return a set of datoms that their transaction instant is less than or equal to `instant`.

  :instant      java.util.Date object of the time you request to investigate."
  :durable-storage)

(defmulti datoms-since!
  "Return a set of datoms that their transaction instant is grater than or equal to `instant`.

  :instant      java.util.Date object of the time you request to investigate."
  :durable-storage)
