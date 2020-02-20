(defproject mamulengo "1.0.47"
  :description "Lightweight embedded database based on datascript for Clojure(Script)"
  :url "https://github.com/wandersoncferreira/mamulengo"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.10.0"]
                 [com.h2database/h2 "1.4.197"]
                 [org.postgresql/postgresql "42.2.5"]
                 [hodgepodge "0.1.3"]
                 [datascript "0.18.9"]
                 [mount "0.1.16"]
                 [com.andrewmcveigh/cljs-time "0.5.2"]
                 [seancorfield/next.jdbc "1.0.13"]
                 [com.taoensso/nippy "2.14.0"]]
  :test-paths ["test/clj" "test/cljc"]
  :profiles {:dev {:source-paths ["src" "dev"]
                   :plugins [[refactor-nrepl "2.4.0"]
                             [lein-doo "0.1.11"]
                             [cider/cider-nrepl "0.23.0"]]
                   :jvm-opts ^:replace ["-server"]
                   :dependencies [[org.clojure/clojurescript "1.10.520"]
                                  [org.clojure/test.check "0.10.0"]
                                  [org.clojure/tools.namespace "0.3.1"]]}}
  :aliases {"all" ["with-profile" "dev"]
            "test-phantom" ["doo" "phantom" "test"]}
  :cljsbuild {:builds [{:id "test"
                        :source-paths ["src" "test/cljc" "test/cljs"]
                        :compiler {:output-to "target/out/test.js"
                                   :output-dir "target/out"
                                   :main mamulengo.doo-runner
                                   :optimizations :none
                                   :process-shim false}}]}
  :repl-options {:init-ns user})
