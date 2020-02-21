# mamulengo [![Clojars Project](https://img.shields.io/clojars/v/mamulengo.svg)](https://clojars.org/mamulengo) [![Build Status](https://travis-ci.org/wandersoncferreira/mamulengo.svg?branch=master)](https://travis-ci.org/wandersoncferreira/mamulengo)

<img src="https://github.com/wandersoncferreira/mamulengo/blob/master/doc/mamulengo_fuzue.jpg" width=260 align="right"/>

A lightweight database based on
[datascript](https://github.com/tonsky/datascript) and a
pluggable storage for data durability. In fact, `mamulengo`
has probably poor performance when compared to other
solutions and its purpose is small-sized applications that
need an embedded database to get its business moving.

We use [datascript](https://github.com/tonsky/datascript) as
the main database, but write every transaction to a durable
storage. Therefore, because of this simple addition we are
able to provide two features to improve datascript and
enable it to be a full-fledged immutable database:
i) durability and ii) time-travel feature as in Datomic.

Datomic doesn't fit for small projects so well, therefore
many projects who could benefit from it, can't. This library
aims to provide a feel for Datalog/Datomic to newcomers.

Mamulengo supports [PostgreSQL](https://www.postgresql.org/)
and [H2](https://www.h2database.com/html/main.html) for
Clojure and [Local Storage](https://funcool.github.io/hodgepodge/) for ClojureScript.

## Release and Dependency Information

This project follows the version scheme MAJOR.MINOR.COMMITS
where MAJOR and MINOR provide some relative indication of
the size of the change, but do not follow semantic
versioning. In general, all changes endeavor to be
non-breaking (by moving to new names rather than by breaking
existing names). COMMITS is an ever-increasing counter of
commits since the beginning of this repository.


## Installation

Leinigen/Boot

```clj
[mamulengo "1.0.57"]
```

Clojure CLI/deps.edn
```clj
mamulengo {:mvn/version "1.0.57"}
```


## Usage

```clj
(require '[mamulengo.core :as m])

(def cfg {:durable-storage :postgresql
          :durable-conf {:dbtype "postgresql"
                         :dbname "mamulengo"
                         :password "test"
                         :user "test"}})

;;; let's define a schema
(def schema-planets
  {:body/name {:db/cardinality :db.cardinality/one
               :db/unique :db.unique/identity}
   :body/diameter {:db/cardinality :db.cardinality/one
                   :db/unique :db.unique/identity}})

(m/connect! cfg schema-planets)


;;; now you are ready to save your data!!
(m/transact! [{:db/id -1
                     :body/name "Earth"
                     :body/diameter 12740}
                    {:db/id -2
                     :body/name "Pluto"
                     :body/diameter 80}
                    {:db/id -3
                     :body/name "Venus"
                     :body/diameter 12100}])

;;; you should also retrieve it back!
(m/query! '[:find (pull ?e [*])
            :in $ ?n
            :where
            [?e :body/name ?n]]
            "Pluto")

```

## Time Travel

All the current durable storages has support for
time-travel. The API behaves similarly with Datomic one, you
can pass an old Database/Connection object to the query
function. 

```clj

(def db-as-of (m/as-of! #inst "2020-02-18T11:46:31.505-00:00"))

;;; more things happening...

(m/query! '[:find ?name
            :where [?e :body/name ?name]]
            db-as-of)

;;; more things...
(def db-since (m/since! #inst "2020-02-18T11:46:31.505-00:00"))


```

You can only capture a database using a timestamp, an option to use the transaction id might be on the way.
Please, see either `time-travel-{pg,h2}-test` namespaces to
see some working examples.


## Why `mamulengo`?

In Brazilian culture we have a very famous event in the
northeast region called
[mamulengo](https://en.wikipedia.org/wiki/Mamulengo) which
is a puppet theater. In front of the public we have the
puppets that entertain and captivate the audience and behind
it we have the hands of the artist that brings movements and
voice to the puppets.

`Mamulengo` here has the same objectives, we want you to be
amazed by `datalog` query engines and all the bells and
whistle of immutable databases, but behind the scenes we manage the hard work to make this possible with no overhead
introduced to the audience.


## Ideas

The idea came from **Rodolfo Ferreira** about rethinking some
concepts on databases and reaching the conclusion that not all applications need an
insane amount of write/reads per second. Sometimes we only
need a SQLite to do our work and go home happily.

However, we still want the advantages of Datascript and
Immutable data sources.

The very initial code was inspired in  https://gitlab.com/kurtosys/lib/factoidic.


## Alternatives

I just discover the project called
[datahike](https://github.com/replikativ/datahike) which is
a lot more sophisticated than mamulengo. I never used this
project so I will not provide any comparison between both,
but `mamulengo` has the apparent advantage of leveraging
existing technologies and easier architecture to understand
how the whole system works.

Feel free to try both!!


## License

Copyright © 2020 Wanderson Ferreira

This program and the accompanying materials are made available under the
terms of the Eclipse Public License 2.0 which is available at
http://www.eclipse.org/legal/epl-2.0.

This Source Code may also be made available under the following Secondary
Licenses when the conditions for such availability set forth in the Eclipse
Public License, v. 2.0 are satisfied: GNU General Public License as published by
the Free Software Foundation, either version 2 of the License, or (at your
option) any later version, with the GNU Classpath Exception which is available
at https://www.gnu.org/software/classpath/license.html.
