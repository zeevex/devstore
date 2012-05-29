(ns devstore.models
  (:require [devstore.models.payment-processor :as proc]
            [devstore.models.offer :as offer]))

(defn initialize
  []
  (proc/init!)
  (offer/init!))
