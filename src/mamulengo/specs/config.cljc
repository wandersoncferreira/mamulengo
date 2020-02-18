(ns mamulengo.specs.config
  (:require [clojure.spec.alpha :as s]))

(s/def ::durable-storage #{:h2 :postgresql :local-storage})
(s/def ::durable-layer #{:on :off})

(s/def ::dbtype string?)
(s/def ::dbname string?)

(s/def ::durable-conf (s/keys :req-un [::dbname
                                       ::dbtype]))

(s/def ::config
  (s/keys :req-un [::durable-storage]
          :opt-un [::durable-layer
                   ::durable-conf]))
