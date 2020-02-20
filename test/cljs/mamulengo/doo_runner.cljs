(ns mamulengo.doo-runner
  (:require [doo.runner :refer-macros [doo-tests]]
            mamulengo.local-storage-test
            mamulengo.durable-off-test
            mamulengo.time-travel-test))

(enable-console-print!)

(doo-tests 'mamulengo.local-storage-test
           'mamulengo.durable-off-test
           'mamulengo.time-travel-test)
