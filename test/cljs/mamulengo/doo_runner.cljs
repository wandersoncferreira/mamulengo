(ns mamulengo.doo-runner
  (:require [doo.runner :refer-macros [doo-tests]]
            mamulengo.local-storage-test
            mamulengo.durable-off-test
            mamulengo.time-travel-pg-test
            mamulengo.time-travel-h2-test))

(enable-console-print!)

(doo-tests 'mamulengo.local-storage-test
           'mamulengo.durable-off-test
           'mamulengo.time-travel-h2-test
           'mamulengo.time-travel-pg-test)
