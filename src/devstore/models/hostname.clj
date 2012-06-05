(ns devstore.models.hostname
  (:require [noir [core    :as core]
                  [request :as req]
                  [session :as session]]))

(defn init!
  []
  true)

(defn- request-hostname
  "Hostname from the request's Host header."
  [& [request]]
  (get-in (req/ring-request) [:headers "host"]))

(defn- request-scheme
  "Scheme for the request."
  [& [request]]
  (name (:scheme (req/ring-request))))

(defn current-hostname
  []
  (session/get :hostname (request-hostname)))

(defn set-current-hostname
  [hostname]
  (if (= "" hostname)
    (session/remove! :hostname)
    (session/put!    :hostname hostname)))

(defn our-host-url
  "The host URL of the current request."
  []
  (str (request-scheme) "://"
       (request-hostname)))

(defn return-url-for
  "The return url for OFFER under ACTION."
  [offer action]
  (str (our-host-url)
       (core/url-for "/store/:id" {:id (:id offer)})
       "?action=" action))

(defn notify-url-for
  "The notify return (IPN) url for OFFER."
  [offer]
  (str (our-host-url)
       (core/url-for "/notify/ipn/:id" {:id (:id offer)})))
