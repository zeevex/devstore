(defproject devstore "0.1.0-SNAPSHOT"
            :description "Developer demo store using the Zeevex APIs"
            :dependencies [[org.clojure/clojure "1.4.0"]
                           [noir "1.3.0-beta3"]
                           [enlive "1.0.0"]
                           [clj-http "0.4.2"]
                           [clj-time "0.4.2"]]
            :main devstore.server)
