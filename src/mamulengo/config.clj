(ns mamulengo.config
  (:require [mount.core :refer [defstate]]))

(defstate mamulengo-cfg
  :start (mount.core/args)
  :stop #(into {}))
