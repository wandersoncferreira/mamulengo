(ns mamulengo.core-test
  (:require [clojure.test :refer [deftest is testing use-fixtures]]
            [mamulengo.durable.h2-impl :refer [clear-h2]]
            [mamulengo.core :as m]))

(def conf {:durable-storage :h2
           :durable-conf {:dbtype "h2:mem"
                          :dbname "test_mamulengo"}})

(def schema-planets
  {:body/name {:db/cardinality :db.cardinality/one}
   :body/diameter {:db/cardinality :db.cardinality/one}})


(use-fixtures :each (fn [f]
                      (m/connect! conf schema-planets)
                      (f)
                      (clear-h2 conf)
                      (m/disconnect!)))

(deftest insert-into-database
  (testing "The database should insert all non-duplicated data."
    (m/transact! [{:db/id -1
                   :body/name "Earth"
                   :body/diameter 12740}
                  {:db/id -2
                   :body/name "Mars"
                   :body/diameter 67000}
                  {:db/id -3
                   :body/name "Venus"
                   :body/diameter 12100}])

    (is (= 12740 (-> (m/query! '[:find ?e ?d
                                 :in $ ?n
                                 :where
                                 [?e :body/name ?n]
                                 [?e :body/diameter ?d]]
                               "Earth")
                     first
                     second)))))
