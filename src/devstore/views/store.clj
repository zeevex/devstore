(ns devstore.views.store
  (:require [devstore.views.common :as common]
            [noir.response :as resp]
            [noir.request :as req]
            [net.cgrand.enlive-html :as html])
  (:use noir.core
        [hiccup core page form element]
        [clj-time [core :only [now]]
                  [coerce :only [to-long]]]
        [clojure [walk :only [walk]]]))

(def offers
  "All offers available in the store."
  [{:id "1"
    :title "One Item Buy Now, USD"
    :brand "anonymous_brand"
    :currency_code "USD"
    :items [{:item-name     "Bit O'Mead"
             :item-sku      "123456"
             :item-price    2.0
             :item-quantity 1}]
    :options {:return        true
              :cancel_return true
              :notify_url    false}}

   {:id "2"
    :title "Two Item Cart, Tokens, No Return/Cancel/Notify URL"
    :brand "anonymous_brand"
    :currency_code "ZXT"
    :items [{:item-name     "Seven Milli-league Boots"
             :item-sku      "123456"
             :item-price    400.0
             :item-quantity 2}
            {:item-name     "Gloves of Autobahn Driving"
             :item-sku      "98765"
             :item-price    50.0
             :item-quantity 1}]
    :options {:return        true
              :cancel_return false
              :notify_url    true}}
   ])

(def payment-processors
  "Pre-defined payment processors for the store to use."
  [{:id         1
    :name       "ZXEngine Pow"
    :site-url   "http://zxengine.dev/"
    :api-url    "http://zxengine.dev/cgi-bin/webscr"
    :auth-token "2cf98ae35a308fee5f7d"
    :opaque-id  "50F626"}])

(defn- find-by-id [maps id]
  (some #(and (= id (% :id)) %) maps))

(defn replace-keys
  "Replace keys of map M with those in REP using F as the transform.
Key k in M is replaced with (F (REP k)).

Values are not modified."
  [f rep m]
  (walk (fn [[k v]] [(f (rep k)) v]) identity m))

(defn- current-processor-id
  "The ID of the current payment-processor."
  []
  ;; FIXME: store and fetch from session
  1)

(defn- our-host-url
  "The host URL of the current request."
  []
  (let [request (req/ring-request)]
    (str (name (request :scheme)) "://"
         (get-in request [:headers "host"]))))

(defn- our-url
  "The URL of the current request."
  []
  (let [request (req/ring-request)]
    (str (name (request :scheme)) "://"
         (get-in request [:headers "host"])
         (request :uri)
         (and (request :query-string)
              (str "?" (request :query-string))))))

(defn- opt-params-for
  [offer processor]
  (let [url (our-url)
        options {:return        url
                 :cancel_return url
                 :notify_url    url}]
    ;; replace true :options values with corresponding entry here
    (walk (fn [[k v]] (when v [k (options k)])) identity (offer :options))))

(defn- form-params-for
  [offer processor]
  (let [cart-params  {:cmd      "_cart"
                      :upload   1
                      :business (processor :opaque-id)
                      :invoice  (to-long (now))
                      :rm       0}
        offer-params (select-keys offer [:brand :currency_code])
        opt-params   (opt-params-for offer processor)]
    (merge cart-params offer-params opt-params)))

(defn- subtotals [offer-items]
  (letfn [(add-item [a item]
            (let [price             (get item :item-price 0)
                  subtotal-price    (get a :subtotal-price 0)
                  quantity          (get item :item-quantity 1)
                  subtotal-quantity (get a :subtotal-quantity 0)
                  tax               (get item :item-tax 0)
                  subtotal-tax      (get a :subtotal-tax 0)
                  shipping          (get item :item-shipping 0)
                  subtotal-shipping (get a :subtotal-shipping 0)]
              (-> a
                  (assoc :subtotal-price    (+ (* quantity price) subtotal-price))
                  (assoc :subtotal-quantity (+ quantity subtotal-quantity))
                  (assoc :subtotal-tax      (+ tax subtotal-tax))
                  (assoc :subtotal-shipping (+ shipping subtotal-shipping)))))]
    (reduce add-item {} offer-items)))

(defn input-params
  "Returns a seq of input field pairs [k v] for a cart.
PARAMS should be a hash of general params to be included in the input field.
ITEMS should be a list of item hashes.

Elements in ITEMS are encoded based on their position/index in the list."
  [params items]
  (letfn [(key-for-index [k idx]
            ;; "item_number" vs. "item_number_1".
            ;; the former is the only item in a single-item purchase.
            ;; the latter is the first item in a multi-item purchase.
            (if (= 1 (count items))
              (str k)
              (str k "_" idx)))
          (inputs-for-item [idx item]   ; encode based on idx
            (let [keys {:item-name     "item_name"
                        :item-sku      "item_number"
                        :item-price    "amount"
                        :item-quantity "quantity"}]
              ;; adjust idx since multi-item cart numbering begins at 1, not 0
              (replace-keys #(key-for-index % (inc idx)) keys item)))]
    (apply merge params (map-indexed inputs-for-item items))))


;; The views

(defpage "/" []
  (resp/redirect "/store/"))

(defpartial list-offer [{:keys [id title]}]
  [:li {:id id}
   (link-to (str "/store/" id)
            title)])

(defpage "/store/" []
  (common/layout
   [:h2 "Pick a Cart"]
   [:ul#offers
    (map list-offer offers)]))

(defn render-template [s]
  (apply str s))

(html/defsnippet cart-item "devstore/views/cart.html" [:tr#cartitem]
  [{:keys [item-name item-sku item-quantity item-price item-tax item-shipping]}]
  [:#item-name]     (html/content item-name)
  [:#item-sku]      (html/content item-sku)
  [:#item-quantity] (html/content (str item-quantity))
  [:#item-price]    (html/content (str item-price))
  [:#item-total]    (html/content (str (* item-quantity item-price)))
  [:#item-tax]      (html/content (str item-tax))
  [:#item-shipping] (html/content (str item-shipping)))

(html/defsnippet subtotal "devstore/views/cart.html" [:tr#subtotal]
  [{:keys [subtotal-quantity subtotal-price subtotal-tax subtotal-shipping]}]
  [:#subtotal-quantity] (html/content (str subtotal-quantity))
  [:#subtotal-price]    (html/content (str subtotal-price))
  [:#subtotal-tax]      (html/content (str subtotal-tax))
  [:#subtotal-shipping] (html/content (str subtotal-shipping)))

(html/defsnippet input-elements "devstore/views/cart.html" [:input#business]
  [params items]
  [:#business] (html/clone-for [[k v] (input-params params items)]
                               (html/set-attr :name (name k) :id (name k) :value v)))

(html/deftemplate cart "devstore/views/cart.html"
  [items formparams processor]
  [:h3]              (html/content (str "Invoice #: " (formparams :invoice)))
  [:#cartitem]       (html/substitute (map cart-item items))
  [:#subtotal]       (html/substitute (subtotal (subtotals items)))
  [:#cancelform]     (html/set-attr :action (our-host-url))
  [:#purchaseform]   (html/set-attr :action (processor :api-url))
  [:#purchaseinputs] (html/substitute (input-elements formparams items)))

(defpage "/store/:id" {id :id}
  (common/layout
   (if-let [offer (find-by-id offers id)]
     (let [items      (get offer :items)
           processor  (find-by-id payment-processors (current-processor-id))
           formparams (form-params-for offer processor)]
       (render-template
        (cart items formparams processor))))))
