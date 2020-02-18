(ns mamulengo.pg-impl-test
  (:require  [clojure.test :refer [deftest is testing]]
             [mamulengo.core :as m]))

(def schema-planets
  {:body/name {:db/cardinality :db.cardinality/one
               :db/unique :db.unique/identity}
   :body/diameter {:db/cardinality :db.cardinality/one
                   :db/unique :db.unique/identity}})

(deftest insert-planets-in-pg
  (testing "The implementation of postgresql should work too"
    (let [cfg {:durable-storage :postgresql
               :durable-conf {:dbtype "postgresql"
                              :dbname "mamulengo"
                              :password "test"
                              :port 54329
                              :user "test"}}]
      (m/connect! cfg)
      (m/transact-schema! schema-planets)
      (m/transact! [{:db/id -1
                     :body/name "Earth"
                     :body/diameter 12740}
                    {:db/id -2
                     :body/name "Pluto"
                     :body/diameter 80}
                    {:db/id -3
                     :body/name "Venus"
                     :body/diameter 12100}])
      (is (=
           ((juxt :body/diameter
                  :body/name) (ffirst (m/query! '[:find (pull ?e [*])
                                                  :in $
                                                  :where
                                                  [?e :body/name "Pluto"]]
                                                nil)))
           [80 "Pluto"]))
      (m/disconnect!))))
