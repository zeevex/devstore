(ns devstore.models.payment-processor
  (:require [noir.session :as session]))

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
    :opaque-id  "50F626"}
   {:id         2
    :name       "Sandbox strawmann"
    :site-url   "http://sandbox.zeevex.com/"
    :api-url    "http://sandbox.zeevex.com/cgi-bin/webscr"
    :auth-token "437880eaa7e92be19bba"
    :opaque-id  "50F626"}])

(defn- default-processor-id
  "The default payment processor's id."
  []
  1)

(defn str->int
  [str]
  (when (re-matches (re-pattern "\\d+") str)
    (read-string str)))

(defn- current-processor-id
  "The ID of the current payment-processor."
  []
  (if-let [proc-id (str->int (session/get :processor ""))]
    proc-id
    (default-processor-id)))

(defn set-processor
  [id]
  (session/put! :processor id))

(defn current-processor
  "The current payment processor."
  []
  (some #(and (= (current-processor-id) (% :id)) %) payment-processors))

(defn all
  "All payment processors."
  []
  payment-processors)
