(ns mamulengo.time-travel-pg-test
  #?@(:cljs
      [(:require [cljs.test :refer-macros [deftest is testing use-fixtures]]

                 [mamulengo.core :as m]
                 [mamulengo.durable.local-storage-impl :refer [clear]])]
      :clj
      [(:require [clojure.test :refer [deftest is testing use-fixtures]]
                 [mamulengo.durable.pg-impl :refer [clear-pg]]
                 [mamulengo.core :as m])]))

(def conf #?(:cljs
             {:durable-storage :local-storage}
             :clj {:durable-storage :postgresql
                   :durable-conf {:dbtype "postgresql"
                                  :dbname "mamulengo"
                                  :password "test"
                                  :port 54329
                                  :user "test"}}))

#?(:cljs (use-fixtures
           :each
           {:before (fn []
                      #?(:cljs (clear))
                      (println "Start cljs testing!"))
            :after (fn []
                     #?(:cljs (clear))
                     (m/disconnect!)
                     (println "Clear local storage"))})
   :clj
   (use-fixtures :each (fn [f]
                         (f)
                         (clear-pg conf)
                         (m/disconnect!))))

(def schema1 {:maker/name {:db/cardinality :db.cardinality/one
                           :db/unique :db.unique/identity}
              :maker/country {:db/cardinality :db.cardinality/one
                              :db/unique :db.unique/identity}})

(def data [{:db/id -1
            :maker/name "BMW"
            :maker/country "Germany"}
           {:db/id -2
            :maker/name "Fusca"
            :maker/country "Brazil"}
           {:db/id -3
            :maker/name "Corolla"
            :maker/country "South Korea"}])

(deftest test-time-travel-as-of
  (testing "I should query a different database if I pass it to the query fn"
    (m/connect! conf schema1)
    (m/transact! data)

    (testing "with the current database, it should have 3 names in there."
      (is (= 3 (count (m/query! '[:find ?n
                                  :where
                                  [?e :maker/name ?n]])))))

    (testing "If I change the name of the BMW, the query should return the new name."
      (m/transact! {:maker/name "Volks" :maker/country "Germany"})
      (m/transact! {:maker/name "BMW" :maker/country "Germany"})
      (m/transact! {:maker/name "Mercedes" :maker/country "Germany"})

      (let [[timestamp2 db-after2] ((juxt :timestamp :db-after)
                                    (m/transact! {:maker/name "Brasilia"
                                                  :maker/country "Germany"}))
            db-old2 (m/as-of! timestamp2)]
        (is (= db-old2 db-after2))

        (m/transact! {:maker/name "Golf" :maker/country "Germany"})
        (is (= (m/query! '[:find ?n
                           :where
                           [?e :maker/name ?n]]
                         db-old2)
               #{["Brasilia"] ["Fusca"] ["Corolla"]}))))))

(deftest test-time-travel-since
  (testing "I should query a different database if I pass it to the query fn"
    (m/connect! conf schema1)
    (m/transact! data)
    (m/transact! {:maker/name "Volks" :maker/country "Argentina"})

    (testing "If I change the name of the BMW, the query should return the new name."
      (is (= 1 1))

      (let [timestamp (:timestamp (m/transact! {:maker/name "Brasilia"
                                                :maker/country "Spain"}))]

        (m/transact! {:maker/name "Golf" :maker/country "Canada"})
        (m/transact! {:maker/name "Jeep" :maker/country "Alaska"})

        (let [since-db (m/since! timestamp)]
          (is (= (m/query! '[:find ?n
                             :where
                             [?e :maker/name ?n]]
                           since-db))
              #{["Brasilia"] ["Golf"] ["Jeep"]}))))))
