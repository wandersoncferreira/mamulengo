(ns mamulengo.durable-off-test
  #?@(:cljs
      [(:require-macros [cljs.test :refer [deftest is testing use-fixtures]])
       (:require [mamulengo.core :as m]
                 [mamulengo.durable.local-storage-impl :refer [clear]]
                 [mount.core :as mount])]
      :clj
      [(:require [clojure.test :refer [deftest is testing use-fixtures]]
                 [mamulengo.core :as m]
                 [mamulengo.durable.h2-impl :refer [clear-h2]]
                 [mount.core :as mount])]))

(def conf #?(:clj {:durable-storage :h2
                   :durable-conf {:dbtype "h2:mem"
                                  :dbname "mamulengo"}}
             :cljs {:durable-storage :local-storage}))

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
                         (clear-h2 conf)
                         (m/disconnect!))))

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
         (is (= :h2 (:durable-storage (mount/args))))])))
