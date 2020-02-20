(ns mamulengo.durable-off-test
  #?@(:cljs
      [(:require-macros [cljs.test :refer [deftest is]])
       (:require [mamulengo.core :as m])]
      :clj
      [(:require [clojure.test :refer [deftest is]]
                 [mamulengo.core :as m])]))

(def data [{:maker/name "BMW"
            :maker/country "Germany"}
           {:maker/name "Fusca"
            :maker/country "Germany"}
           {:maker/name "Corolla"
            :maker/country "South Korea"}])

; (deftest test-remove-durable-layer
;   (m/connect! {:durable-layer :off})
;   (let [db-old (:db-after (m/transact! data))]
;     (m/transact! {:maker/name "Maverick" :maker/country "USA"})

;     (println (m/query! '[:find ?n
;                          :where
;                          [?e :maker/name ?n]]
;                        db-old))))
