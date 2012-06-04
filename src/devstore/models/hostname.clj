(ns devstore.models.hostname
  (:require [noir [core    :as core]
                  [request :as req]
                  [session :as session]]))

(def ^:private default-hostname
  (str "localhost:" (get (System/getenv) "PORT" "8080")))

(defn current-hostname
  []
  (session/get :hostname default-hostname))

(defn set-current-hostname
  [hostname]
  (if (= "" hostname)
    (session/remove! :hostname)
    (session/put!    :hostname hostname)))

(defn our-public-url
  "The public URL for our service."
  []
  (str "http://" (current-hostname) "/"))

(defn our-host-url
  "The host URL of the current request."
  []
  (let [request (req/ring-request)]
    (str (name (:scheme request)) "://"
         (get-in request [:headers "host"]))))

(defn our-url
  "The URL of the current request."
  []
  (let [request (req/ring-request)]
    (str (name (:scheme request)) "://"
         (get-in request [:headers "host"])
         (request :uri)
         (and (:query-string request)
              (str "?" (:query-string request))))))

(defn return-url-for
  "The return url for OFFER under ACTION."
  [offer action]
  (str (our-host-url)
       (core/url-for "/store/:id" {:id (:id offer)})
       "?action=" action))

(defn notify-url-for
  "The notify return (IPN) url for OFFER."
  [offer]
  (str (our-public-url)
       (core/url-for "/notify/ipn/:id" {:id (:id offer)})))
