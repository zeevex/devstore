(ns devstore.views.common
  (:require [devstore.models.payment-processor :as proc])
  (:use [noir.core :only [defpartial]]
        [hiccup
         [page    :only [include-css html5]]
         [element :only [link-to]]
         form]))

(defn- submit-on-change
  "Have form auto-submit on change."
  [form]
  (assoc-in form [1 :onchange] "this.form.submit()"))

(defpartial select-processor [& content]
  (form-to [:post "/processor"]
           content
           (submit-on-change
            (drop-down "processor"
                       (map #(vector (:name %) (:id %)) (proc/all))
                       (:id (proc/current-processor))))))

(defpartial nav-bar []
  [:div.nav
   (select-processor
    [:small
     (link-to "/" "Home")
     "| Visit Payment Processor:"
     (let [processor (proc/current-processor)]
       (link-to (:site-url processor) (:name processor)))
     "| Change Payment Processor:"])
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
