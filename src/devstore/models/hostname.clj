(ns devstore.models.hostname
  (:require [noir [core    :as core]
                  [request :as req]
                  [session :as session]]
            [clojure.string :as string]))

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

(defn- url-encoded-params
  [params]
  (letfn [(encode-pair [[k v]]
            (str (java.net.URLEncoder/encode (name k))
                 "="
                 (java.net.URLEncoder/encode (name v))))]
    (string/join "&" (map encode-pair params))))

(defn return-url-for
  "The return url for OFFER with PARAMS."
  [offer params]
  (str (our-host-url)
       (core/url-for (str "/notify/purchase/" (:action params) "/:id") {:id (:id offer)})
       "?"
       (url-encoded-params params)))

(defn notify-url-for
  "The notify return (IPN) url for OFFER."
  [offer]
  (str (our-host-url)
       (core/url-for "/notify/ipn/:id" {:id (:id offer)})))
