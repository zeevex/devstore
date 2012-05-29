(ns devstore.models
  (:require [devstore.models.payment-processor :as proc]))

(defn initialize []
  (proc/init!))
