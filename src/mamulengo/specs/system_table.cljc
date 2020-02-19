(ns mamulengo.specs.system-table
  (:require [clojure.spec.alpha :as s]))


(s/def ::id integer?)
(s/def ::instant inst?)
(s/def ::nippy string?)
(s/def ::tx integer?)
(s/def ::meta (s/or :str string? :null nil?))
(s/def ::datoms string?)

(s/def ::schema
  (s/keys :req-un [::id
                   ::instant
                   ::nippy]))

(s/def ::table-schema
  (s/coll-of ::schema))

(s/def ::txs
  (s/keys :req-un [::id
                   ::tx
                   ::instant
                   ::meta]))

(s/def ::table-tx
  (s/coll-of ::txs))

(s/def ::mamulengo
  (s/keys :req-un [::id
                   ::datoms
                   ::tx]))

(s/def ::table-mamulengo
  (s/coll-of ::mamulengo))
