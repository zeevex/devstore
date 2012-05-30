(ns devstore.views.store
  (:require [devstore.views.common :as common]
            [devstore.models.payment-processor :as proc]
            [devstore.models.offer :as offer]
            [noir.response :as resp]
            [noir.request :as req]
            [net.cgrand.enlive-html :as html])
  (:use noir.core
        [hiccup core page form element]
        [clj-time [core :only [now]]
                  [coerce :only [to-long]]]
        [clojure [walk :only [walk]]]))

(defn replace-keys
  "Replace keys of map M with those in REP using F as the transform.
Key k in M is replaced with (F (REP k)).

Values are not modified."
  [f rep m]
  (walk (fn [[k v]] [(f (rep k)) v]) identity m))

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

(defn- return-url-for
  "The return url for OFFER under ACTION."
  [offer action]
  (str (our-host-url)
       (url-for "/store/:id" {:id (:id offer)})
       "?action=" action))

(defn- response-params-for
  "Returns map of purchase completion response parameters."
  [offer processor]
  (let [url (our-url)
        response {:return        (return-url-for offer "complete")
                  :cancel_return (return-url-for offer "cancel")
                  :notify_url    url}]
    ;; replace any response entries with the corresponding URLs
    (walk (fn [[k v]] (when v [k (response k)])) identity (:response offer))))

(defn- form-params-for
  "Returns map of all non-item form parameters."
  [offer processor]
  (let [cart-params  {:cmd      "_cart"
                      :upload   1
                      :business (:opaque-id processor)
                      :invoice  (to-long (now))
                      :rm       0}
        offer-params (select-keys offer [:brand :currency_code])
        response-params (response-params-for offer processor)]
    (merge cart-params offer-params response-params)))

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
    (map list-offer (offer/all))]))

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
  [:#purchaseform]   (html/set-attr :action (:api-url processor))
  [:#purchaseinputs] (html/substitute (input-elements formparams items)))

(defpage "/store/:id" {id :id :as params}
  (println "GET /store/:id " params)
  (common/layout
   (if-let [offer (offer/find-by-id id)]
     (let [items      (:items offer)
           processor  (proc/current-processor)
           formparams (form-params-for offer processor)]
       (render-template
        (cart items formparams processor))))))

(defpage [:post "/store/:id"] {id :id :as params}
  (println "POST /store/:id " params)
  (render "/store/:id" params))

(defpage [:post "/processor"] {:as params}
  (proc/set-current-processor (:processor params))
  (resp/redirect (get-in (req/ring-request) [:headers "referer"] "/")))
