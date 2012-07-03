(ns devstore.models.offer)

(defn init!
  []
  true)

(def offers
  "All offers available in the store."
  [{:id "1"
    :title "1 Item USD, IPN"
    :brand "anonymous_brand"
    :currency_code "USD"
    :items [{:item-name     "Bit O'Mead"
             :item-sku      "sku_0001"
             :item-price    2.0
             :item-quantity 1}]
    :response {:return        true
               :cancel_return true
               :notify_url    true}}

   {:id "2"
    :title "2 Item ZXT, No Return/Cancel/Notify URL"
    :brand "anonymous_brand"
    :currency_code "ZXT"
    :items [{:item-name     "Seven Milli-league Boots"
             :item-sku      "sku_0002"
             :item-price    400.0
             :item-quantity 2}
            {:item-name     "Gloves of Autobahn Driving"
             :item-sku      "sku_0003"
             :item-price    50.0
             :item-quantity 1}]
    :response {:return        false
               :cancel_return false
               :notify_url    false}}
   {:id "3"
    :title "3 Item ZXT, No IPN"
    :brand "anonymous_brand"
    :currency_code "ZXT"
    :items [{:item-name     "Elixir of Mentho-Lyptus"
             :item-sku      "sku_0004"
             :item-price    30.0
             :item-quantity 4}
            {:item-name     "Seven Milli-league Boots"
             :item-sku      "sku_0002"
             :item-price    400.0
             :item-quantity 1}
            {:item-name     "Bag of Holding Stuff"
             :item-sku      "sku_0005"
             :item-price    500.0
             :item-quantity 1}]
    :response {:return        true
               :cancel_return true
               :notify_url    false}}
   ])

(defn all
  "Returns list of all available offers."
  []
  offers)

(defn find-by-id
  "Find offer by ID."
  [id]
  (some #(and (= id (% :id)) %) offers))

