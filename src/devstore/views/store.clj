(ns devstore.views.store
  (:require [devstore.views.common :as common]
            [devstore.models
             [payment-processor :as proc]
             [offer             :as offer]
             [hostname          :as host]
             [purchase          :as purchase]]
            [noir
             [response :as resp]
             [request  :as req]]
            [net.cgrand.enlive-html :as html]
            [clj-time.core          :as time])
  (:use noir.core
        [hiccup core page form element]
        [clojure
         [walk :only [walk]]
         [pprint :only [pprint]]]))

(defn replace-keys
  "Replace keys of map M with those in REP using F as the transform.
Key k in M is replaced with (F (REP k)).

Values are not modified."
  [f rep m]
  (walk (fn [[k v]] [(f (rep k)) v]) identity m))

(defn- response-params-for
  "Returns map of purchase completion response parameters."
  [offer invoice processor]
  (let [response {:return        (host/return-url-for offer {:action "complete", :invoice invoice})
                  :cancel_return (host/return-url-for offer {:action "cancel",   :invoice invoice})
                  :notify_url    (host/notify-url-for offer)}]
    ;; replace any response entries with the corresponding URLs
    (walk (fn [[k v]] (when v [k (response k)])) identity (:response offer))))

(defn- form-params-for
  "Returns map of all non-item form parameters."
  [offer processor]
  (let [invoice         (purchase/new-invoice-id)
        cart-params     {:cmd      "_cart"
                         :upload   1
                         :business (:opaque-id processor)
                         :invoice  invoice
                         :rm       0}
        offer-params    (select-keys offer [:brand :currency_code])
        response-params (response-params-for offer invoice processor)]
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

(html/defsnippet history-entry "devstore/views/cart.html" [:tr#history-entry]
  [[_ {:keys [purchase ipn pdt status]}]]
  [:#history-invoice] (html/content (:invoice purchase))
  [:#history-pdt]     (html/content (if (nil? pdt) "" "Y"))
  [:#history-ipn]     (html/content (if (nil? ipn) "" "Y"))
  [:#history-cancel]  (html/content (if (= status "cancel") "Y" "")))

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

(html/defsnippet response-entry "devstore/views/params.html" [:tr#response-entry]
  [[param value]]
  [:#response-param] (html/content (name param))
  [:#response-value] (html/content (name value)))

(html/defsnippet purchase-completion "devstore/views/cart.html" [:#purchase-status]
  [status]
  [:#purchase-completion] (html/content status))

(defn- purchase-status
  [params]
  (if-let [[_ action] (find params :action)]
    (case action
      "cancel"   (purchase-completion "cancelled")
      "complete" (purchase-completion "completed")
      "")))

(html/defsnippet input-elements "devstore/views/cart.html" [:input#business]
  [params items]
  [:#business] (html/clone-for [[k v] (input-params params items)]
                               (html/set-attr :name (name k) :id (name k) :value v)))

(html/deftemplate cart "devstore/views/cart.html"
  [items formparams processor params history]
  [:#invoicenum]      (html/content (str (formparams :invoice)))
  [:#history-entry]   (html/substitute (map history-entry history))
  [:#cartitem]        (html/substitute (map cart-item items))
  [:#subtotal]        (html/substitute (subtotal (subtotals items)))
  [:#cancelform]      (html/set-attr :action (host/our-host-url))
  [:#purchaseform]    (html/set-attr :action (:api-url processor))
  [:#purchaseinputs]  (html/substitute (input-elements formparams items))
  [:#purchase-status] (html/substitute (purchase-status params)))

(defpage "/store/:id" {id :id :as params}
  (println "GET /store/:id " params)
  (common/layout
   (if-let [offer (offer/find-by-id id)]
     (let [items      (:items offer)
           processor  (proc/current-processor)
           formparams (form-params-for offer processor)]
       (purchase/create (assoc formparams :id id))
       (let [history (purchase/find-by-cart-id id (time/minus (time/now) (time/hours 2)))]
         (render-template
          (cart items formparams processor params history)))))))

(defpage [:post "/store/:id"] {id :id :as params}
  (println "POST /store/:id " params)
  (render "/store/:id" params))

(defpage "/notify/purchase/complete/:id" {:keys [id invoice] :as params}
  (purchase/set-status invoice "complete")
  (let [pdt-status (proc/pdt-from-params params)]
    (purchase/add-pdt (assoc pdt-status :id id)))
  (render "/store/:id" params))

(defpage [:post "/notify/purchase/cancel/:id"] {:keys [id invoice] :as params}
  (purchase/set-status invoice "cancel")
  (render "/store/:id" params))

(defpage [:post "/notify/ipn/:id"] {id :id :as params}
  (println "NOTIFY /notify/ipn/:id" params)
  (purchase/add-ipn params)
  "OK")

(defpage [:post "/options"] {:keys [processor hostname] :as params}
  (when-not (= "0" processor)
    (proc/set-current-processor processor))
  (when-not (or (nil? hostname)
                (= (host/current-hostname) hostname))
    (host/set-current-hostname hostname))
  (resp/redirect (get-in (req/ring-request) [:headers "referer"] "/")))
