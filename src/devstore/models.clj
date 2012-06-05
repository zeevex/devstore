(ns devstore.models
  (:require [devstore.models
             [payment-processor :as proc]
             [offer             :as offer]
             [hostname          :as hostname]
             [purchase          :as purchase]]))

(defn initialize
  []
  (proc/init!)
  (offer/init!)
  (hostname/init!)
  (purchase/init!))
