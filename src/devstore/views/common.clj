(ns devstore.views.common
  (:use [noir.core :only [defpartial]]
        [hiccup
         [page    :only [include-css html5]]
         [element :only [link-to]]]))

(defpartial nav-bar []
  [:div.nav
   [:small
    (link-to "/" "Home")
    "|"
    ;; FIXME: link to pp's home page (zxengine.dev)
    (link-to "/" "Payment Processor")]
   [:p]])

(defpartial layout [& content]
  (html5
   [:head
    [:title "Zeevex Demo Store"]
    #_(include-css "/css/reset.css")]
   [:body
    [:div#wrapper
     (nav-bar)
     content]]))
