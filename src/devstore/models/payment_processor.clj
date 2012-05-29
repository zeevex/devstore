(ns devstore.models.payment-processor)

(defn init!
  []
  true)

(def payment-processors
  "Pre-defined payment processors for the store to use."
  [{:id         1
    :name       "ZXEngine Pow"
    :site-url   "http://zxengine.dev/"
    :api-url    "http://zxengine.dev/cgi-bin/webscr"
    :auth-token "2cf98ae35a308fee5f7d"
    :opaque-id  "50F626"}])

(defn- current-processor-id
  "The ID of the current payment-processor."
  []
  ;; FIXME: store and fetch from session
  1)

(defn current-processor
  "The current payment processor."
  []
  (some #(and (= (current-processor-id) (% :id)) %) payment-processors))
