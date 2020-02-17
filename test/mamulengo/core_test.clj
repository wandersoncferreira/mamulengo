(ns mamulengo.core-test
  (:require [mamulengo.core :as sut]
            [clojure.test :refer [deftest is testing]]))

(def schema-planets
  {:body/name {:db/cardinality :db.cardinality/one}
   :body/diameter {:db/cardinality :db.cardinality/one}})

(deftest insert-into-database
  (testing "The database should insert all non-duplicated data."
    (let [cfg {:durable-layer :h2
               :durable-conf {:dbtype "h2:mem"
                              :dbname "test_mamulengo"}}]
      (sut/connect! cfg)
      (sut/transact-schema! schema-planets)
      (sut/transact! [{:db/id -1
                       :body/name "Earth"
                       :body/diameter 12740}
                      {:db/id -2
                       :body/name "Mars"
                       :body/diameter 67000}
                      {:db/id -3
                       :body/name "Venus"
                       :body/diameter 12100}])

      (is (= 12740 (-> (sut/query! '[:find ?e ?d
                                     :in $ ?n
                                     :where
                                     [?e :body/name ?n]
                                     [?e :body/diameter ?d]]
                                   ["Earth"])
                       first
                       second)))
      (sut/disconnect!))))
