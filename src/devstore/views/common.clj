(ns devstore.views.common
  (:require [devstore.models.payment-processor :as proc])
  (:use [noir.core :only [defpartial]]
        [hiccup
         [page    :only [include-css html5]]
         [element :only [link-to]]
         form]))

(defn- submit-form-on-selection-change
  "Have FORM auto-submit on change."
  [form]
  (assoc-in form [1 :onchange] "this.form.submit()"))

;; Wraps CONTENT in a form for selecting the processor
(defpartial select-payment-processor
  [& content]
  (form-to [:post "/processor"]
           content
           (submit-form-on-selection-change
            (drop-down "processor"
                       (into ["Choose Payment Processor"]
                             (map #(vector (:name %) (:id %)) (proc/all)))))))

(defpartial nav-bar
  []
  [:div.nav
   (select-payment-processor
    [:small
     (link-to "/" "Home")
     " | Visit Payment Processor:"
     (let [processor (proc/current-processor)]
       (link-to (:site-url processor) (:name processor)))
     " | "])
   [:p]])

(defpartial layout
  [& content]
  (html5
   [:head
    [:title "Zeevex Demo Store"]
    #_(include-css "/css/reset.css")]
   [:body
    [:div#wrapper
     (nav-bar)
     content]]))
