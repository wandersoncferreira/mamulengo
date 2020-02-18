(defproject mamulengo "0.1.3"
  :description "Lightweight embedded database based on datascript for Clojure(Script)"
  :url "https://github.com/wandersoncferreira/mamulengo"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.10.0"]
                 [org.clojure/tools.namespace "0.3.1"]
                 [com.h2database/h2 "1.4.197"]
                 [org.postgresql/postgresql "42.2.5"]
                 [datascript "0.18.9"]
                 [potemkin "0.4.5"]
                 [mount "0.1.16"]
                 [seancorfield/next.jdbc "1.0.13"]
                 [com.taoensso/nippy "2.14.0"]
                 [org.clojure/test.check "0.10.0"]]

  :profiles {:dev {:source-paths ["src" "dev"]
                   :plugins [[refactor-nrepl "2.4.0"]
                             [cider/cider-nrepl "0.23.0"]]}}
  :repl-options {:init-ns user})
