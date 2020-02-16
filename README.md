# mamulengo

<img src="https://github.com/wandersoncferreira/mamulengo/blob/master/doc/mamulengo_fuzue.jpg" width=260 align="right"/>

A lightweight database based on `datascript` and a pluggable
storage for data durability. In fact, `mamulengo` has
probably poor performance when compared to other solutions
and its purpose is small-sized applications that need an
embedded database to get its business moving.


We use `datascript` as the main database, but write every
transaction to a durable storage. Therefore, because of this
simple addition we are able to provide two features to
improve datascript and enable it to be a full-fledged
immutable database: i)durability and ii) time-travel feature
as in Datomic.



Bleedinng-edge PR.. Still wondering about APIs and code organization..

## Usage

FIXME


## Ideas

The idea came from **Rodolfo Ferreira** about rethinking some
concepts on databases and reaching the conclusion that not all applications need an
insane amount of write/reads per second. Sometimes we only
need a SQLite to do our work and go home happily.

However, we still want the advantages of Datascript and
Immutable data sources.

The very initial code was inspired in  https://gitlab.com/kurtosys/lib/factoidic.


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
