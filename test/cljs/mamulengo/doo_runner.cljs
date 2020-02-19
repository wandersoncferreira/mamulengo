(ns mamulengo.doo-runner
  (:require [doo.runner :refer-macros [doo-tests]]
            mamulengo.local-storage-test))

(enable-console-print!)

(doo-tests 'mamulengo.local-storage-test)
