(ns mamulengo.specs.system-table
  (:require [clojure.spec.alpha :as s]))


(s/def ::id integer?)
(s/def ::instant inst?)
(s/def ::nippy string?)
(s/def ::tx integer?)
(s/def ::meta string?)
(s/def ::datoms string?)

(s/def ::table-schema
  (s/keys :req-un [::id
                   ::instant
                   ::nippy]))

(s/def ::table-tx
  (s/keys :req-un [::id
                   ::tx
                   ::instant
                   ::meta]))

(s/def ::table-mamulengo
  (s/keys :req-un [::id
                   ::datoms
                   ::tx]))
