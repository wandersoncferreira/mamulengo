(ns mamulengo.durability)

(defmulti create-system-tables! :durable-storage)

(defmulti setup-clients-schema! :durable-storage)

(defmulti retrieve-all-facts! :durable-storage)

(defmulti get-system-schema! :durable-storage)

(defmulti get-schema-at! :durable-storage)

(defmulti store! :durable-storage)

(defmulti datoms-as-of! :durable-storage)

(defmulti datoms-since! :durable-storage)
