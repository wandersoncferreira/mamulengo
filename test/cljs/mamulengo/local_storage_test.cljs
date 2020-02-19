(ns mamulengo.local-storage-test
  (:require [cljs.test :refer-macros [deftest is testing use-fixtures]]
            [mount.core :as mount]
            [mamulengo.core :as m]
            [mamulengo.durability :as du]
            [mamulengo.durable.local-storage-impl :refer [clear] :as impl]))

(use-fixtures
  :each
  {:before (fn [] (println "Start cljs testing!"))
   :after (fn []
            (clear)
            (m/disconnect!)
            (println "Clear local storage"))})

(def conf {:durable-storage :local-storage})

(def schema1 {:maker/name {:db/cardinality :db.cardinality/one}
              :maker/country {:db/cardinality :db.cardinality/one}})

(def schema2 {})

(def schema3 {:maker/name {:db/cardinality :db.cardinality/one
                           :db/unique :db.unique/identity}
              :maker/country {:db/cardinality :db.cardinality/one
                              :db/unique :db.unique/identity}})

(def data [{:maker/name "BMW"
            :maker/country "Germany"}
           {:maker/name "Fusca"
            :maker/country "Germany"}
           {:maker/name "Corolla"
            :maker/country "South Korea"}])

(deftest test-verify-correct-connection
  (testing "When no config is provided in cljs should be used Local Storage as default durable storage."
    (m/connect!)
    (is (= :local-storage (:durable-storage (mount/args))))))

(deftest test-transactions
  (m/connect!)
  (m/transact! data)

  (testing "We should be able to transact a list of data to db"
    (let [ret (m/query! '[:find ?name
                          :where
                          [?e :maker/name ?name]])]
      (is (= 3 (count ret))))

    (testing "also, the data could be a simple map"
      (m/transact! {:maker/name "Honda Civic"
                    :maker/country "Japan"})

      (is (= "Japan" (-> (m/query! '[:find ?c
                                     :in $ ?name
                                     :where
                                     [?e :maker/name ?name]
                                     [?e :maker/country ?c]]
                                   "Honda Civic")
                         ffirst))))))

(deftest test-schema-changes
  (testing "inserting a single schema into the database."
    (m/connect! conf schema1)
    (let [schema (du/get-system-schema! conf)]
      (is (= (type schema) cljs.core/PersistentArrayMap))
      (is (= schema1 schema))))

  (testing "reconnecting and capture the last schema saved."
    (m/connect! conf schema1)
    (m/disconnect!)
    (m/connect! conf schema1)
    (let [schema (du/get-system-schema! conf)]
      (is (= schema1 schema))))

  (testing "saving different versions of schema."
    (m/connect! conf schema1)
    (m/disconnect!)
    (m/connect! conf schema2)
    (is (= schema2 (du/get-system-schema! conf)))
    (m/disconnect!)
    (m/connect! conf schema3)
    (is (= schema3 (du/get-system-schema! conf)))))


(deftest test-whole-table-schema
  (testing "verify the whole schema table in the browser"
    (m/connect! conf schema1)
    (is (= 1 (count (impl/read-storage :table-schema))))
    (m/disconnect!)
    (m/connect! conf schema2)
    (is (= 2 (count (impl/read-storage :table-schema))))
    (m/disconnect!))
  (testing "If I pass the same schema as it had before, I should not add it to database again"
    (m/connect! conf schema2)
    (is (= 2 (count (impl/read-storage :table-schema))))
    (m/disconnect!))
  (testing "adding more schema should be fine."
    (m/connect! conf schema3)
    (is (= 3 (count (impl/read-storage :table-schema))))))


(deftest test-time-travel
  (testing "I should query a different database if I pass it to the query fn"
    (m/connect! conf schema1)
    (let [db-old (:db-after (m/transact! data))
          _ (m/transact! {:maker/name "Maverick"
                          :maker/country "USA"})]
      (testing "with the current database, it should have 4 names in there."
        (is (= 4 (count (m/query! '[:find ?n
                                    :where
                                    [?e :maker/name ?n]])))))
      (testing "but if I use the old database, the Maverick was not inserted yet."
        (let [ret (m/query! '[:find ?n
                              :where
                              [?e :maker/name ?n]]
                            db-old)]
          (is (= 3 (count ret)))
          (is (not (contains? ret ["Maverick"]))))))))
