(ns mamulengo.durable.interface)

(defmulti create-system-tables! :durable-layer)

(defmulti setup-clients-schema! :durable-layer)

(defmulti retrieve-all-facts! :durable-layer)

(defmulti get-system-schema! :durable-layer)

(defmulti store! :durable-layer)
