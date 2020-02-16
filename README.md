# mamulengo

A lightweight database based on `datascript` and a pluggable
storage for data durability.


Bleedinng-edge PR.. Still wondering about APIs and code organization..

## Usage

FIXME


## Ideas

The idea came from **Rodolfo Ferreira** about rethinking some
ideas about database and that not all applications need an
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
