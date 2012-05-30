(ns devstore.server
  (:require [noir.server :as server]
            [devstore.models :as models])
   (:use [ring.middleware.session.cookie :only [cookie-store]]))

(server/load-views-ns 'devstore.views)

(defn -main [& m]
  (let [mode (keyword (or (first m) :dev))
        port (Integer. (get (System/getenv) "PORT" "8080"))]
    (models/initialize)
    (server/start port {:mode mode
                        :ns 'devstore
                        :session-store (cookie-store)})))

