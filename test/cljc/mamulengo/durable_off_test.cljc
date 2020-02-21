(ns mamulengo.durable-off-test
  #?@(:cljs
      [(:require-macros [cljs.test :refer [deftest is testing]])
       (:require [mamulengo.core :as m]
                 [mount.core :as mount])]
      :clj
      [(:require [clojure.test :refer [deftest is testing]]
                 [mamulengo.core :as m]
                 [mount.core :as mount])]))

(def data [{:maker/name "BMW"
            :maker/country "Germany"}
           {:maker/name "Fusca"
            :maker/country "Germany"}
           {:maker/name "Corolla"
            :maker/country "South Korea"}])

(deftest test-remove-durable-layer
  (m/connect! {:durable-layer :off}))

(deftest test-use-default-setup
  (testing "The cljs should choose the local-storage"
    #?@(:cljs
        [(m/connect!)
         (is (= :local-storage (:durable-storage (mount/args))))]
        :clj
        [(m/connect!)
         (is (= :h2 (:durable-storage (mount/args))))])
    (m/disconnect!)))
