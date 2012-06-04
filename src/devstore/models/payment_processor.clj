(ns devstore.models.payment-processor
  (:require [noir.session :as session]
            [clj-http.client :as client])
  (:use [clojure.string :only [split split-lines]]))

(defn init!
  []
  true)

(def payment-processors
  "Pre-defined payment processors for the store to use."
  [{:id         1
    :name       "Local Pow ZXEngine"
    :site-url   "http://zxengine.dev/"
    :api-url    "http://zxengine.dev/cgi-bin/webscr"
    :auth-token "2cf98ae35a308fee5f7d"
    :opaque-id  "50F626"}
   {:id         2
    :name       "Sandbox - strawmann"
    :site-url   "http://sandbox.zeevex.com/"
    :api-url    "http://sandbox.zeevex.com/cgi-bin/webscr"
    :auth-token "437880eaa7e92be19bba"
    :opaque-id  "50F626"}])

(def ^:private default-processor-id
  "The default payment processor's id."
  1)

(defn- current-processor-id
  "The ID of the current payment-processor."
  []
  (Integer/parseInt (session/get :processor (str default-processor-id))))

(defn set-current-processor
  [id]
  (session/put! :processor id))

(defn current-processor
  "The current payment processor."
  []
  (some #(and (= (current-processor-id) (:id %)) %) payment-processors))

(defn all
  "All payment processors."
  []
  payment-processors)

(defn- decode-pdt-response-line
  "Decode a line of the PDT response into a [KEY VALUE] pair."
  [line]
  (let [[key & v] (split line #"=")
        value (if (nil? v)
                ""
                (java.net.URLDecoder/decode (first v) "utf-8"))]
    [key value]))

(defn fetch-pdt-status
  "Fetch the PDT completion status for transaction TX at PROCESSOR."
  [processor tx]
  (when-let [response (client/post (:api-url processor)
                                   {:query-params
                                    {:cmd "_notify_synch"
                                     :at (:auth-token processor)
                                     :tx tx}})]
    (when (= (get-in response [:headers "status"]) "200")
      ;; first line of body is SUCCESS, followed by key/value pairs
      (let [lines (split-lines (:body response))]
        (when (= "SUCCESS" (first lines))
          (into {} (map decode-pdt-response-line (rest lines))))))))

(defn pdt-from-params
  "Fetch the PDT completion status from PARAMS."
  [params]
  (when-let [tx (or (:tx params) (:txn params))]
    (fetch-pdt-status (current-processor) tx)))
