(ns devstore.views.common
  (:require [devstore.models.payment-processor :as proc]
            [devstore.models.hostname          :as host])
  (:use [noir.core :only [defpartial]]
        [hiccup
         [page    :only [include-css html5]]
         [element :only [link-to]]
         form]))

(defn- submit-form-on-selection-change
  "Have FORM auto-submit on change."
  [form]
  (assoc-in form [1 :onchange] "this.form.submit()"))

(defpartial nav-bar
  []
  [:div.nav
   (form-to
    [:post "/options"]
    [:small
     (link-to "/" "Home")
     " | Visit Payment Processor:"
     (let [processor (proc/current-processor)]
       (link-to (:site-url processor) (:name processor)))
     " | "
     (submit-form-on-selection-change
      (drop-down "processor"
                 (into [["Choose Payment Processor" 0]]
                       (map #(vector (:name %) (:id %)) (proc/all)))))
     " | Our hostname (for IPNs): "
     (text-field "hostname" (host/current-hostname))])
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
