(ns mamulengo.config
  #?@(:clj
      [(:require [mount.core :refer [defstate] :as mount])]
      :cljs
      [(:require-macros [mount.core :refer [defstate]])
       (:require [mount.core :as mount])]))

(defstate mamulengo-cfg
  :start (mount/args)
  :stop (mount/with-args {}))
