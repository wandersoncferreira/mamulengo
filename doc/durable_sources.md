# Durable sources

The following options for durable sources are available now.

## PostgreSQL (Clojure)

```clj
(def cfg {:durable-storage :postgresql
               :durable-conf {:dbtype "postgresql"
                              :dbname "mamulengo"
                              :password "test"
                              :port 54329
                              :user "test"}})
```

## H2 Database (Clojure)

```clj
(def cfg {:durable-storage :h2
          :durable-conf {:dbtype "h2:mem"
                         :dbname "test_mamulengo"}})
```

## Local Storage (ClojureScript)

```clj
(def cfg {:durable-storage :local-storage})
```

Remember that you need to explicitly clear the local-storage
when you desire to tear-down the database.

## Opt-out (Clojure(Script))

If you want to opt-out a durable storage you can pass the
following keyword in the configuration map:

```clj
(def cfg {:durable-layer :off})
```
