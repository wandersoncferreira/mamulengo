(ns mamulengo.safeguards-test
  (:require [clojure.test :refer [deftest is testing]]
            [mamulengo.core :as m]))

(deftest test-core-methods
  (testing "You should be warned if you try to use any core method without connecting before."
    (is (= "Mamulengo is disconnected" (m/transact! {:planet/name "Earth"})))
    (is (= "Mamulengo is disconnected" (m/query! '[:find ?e
                                                   :where
                                                   [?e :planet/name _]])))
    (is (= "Mamulengo is disconnected" (m/as-of! #inst "2020-02-22")))
    (is (= "Mamulengo is disconnected" (m/since! #inst "2020-02-22")))))
