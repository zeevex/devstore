(ns devstore.models.offer)

(defn init!
  []
  true)

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
    :response {:return        true
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
    :response {:return        true
               :cancel_return false
               :notify_url    true}}
   ])

(defn all
  "Returns list of all available offers."
  []
  offers)

(defn find-by-id
  "Find offer by ID."
  [id]
  (some #(and (= id (% :id)) %) offers))

