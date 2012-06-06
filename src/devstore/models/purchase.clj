(ns devstore.models.purchase
  (:use [clj-time
         [core :only [now]]
         [coerce :only [to-long]]]))

(defn init!
  []
  true)

(def purchases
  "All purchases issued so far, keyed by invoice number.
Each entry is itself a map with keys :purchase, :pdt, and :ipn."
  (ref (sorted-map)))

(defn new-invoice-id
  "Create a new invoice id."
  []
  ;; Go the extra mile to ensure uniqueness of invoice numbers as
  ;; everything is keyed off of them.
  (let [id (str "devstore-" (to-long (now)))]
    (dosync
     (if-not (find @purchases id)
       ;; reserve the new invoice name with a nil valued entry
       (do (alter purchases merge [id nil])
           id)
       (do (Thread/sleep 10)
           (recur))))))

(defn- track-by-invoice
  "Tracks ITEM by its :invoice key under KEY."
  [item key]
  (dosync (alter purchases update-in [(:invoice item) key] merge item)))

(defn create
  "New purchase, initialized from PURCHASE."
  [purchase]
  (track-by-invoice purchase :purchase))

(defn add-pdt
  "Track PDT with its purchase, identified by its invoice number."
  [pdt]
  (track-by-invoice pdt :pdt))

(defn add-ipn
  "Track IPN with its purchase, identified by its invoice number."
  [ipn]
  (track-by-invoice ipn :ipn))

(defn find-by-invoice
  "Locate a purchase by its INVOICE.
Returns a map containing :purchase, :ipn, and :pdt maps."
  [invoice]
  (when-let [[_ entry] (find @purchases invoice)]
    entry))

(defn find-by-cart-id
  "Locate purchases for a cart, identified by its ID.
Returns a sequence of purchases."
  [id]
  (filter (fn [[_ {purchase :purchase}]] (= id (:id purchase))) @purchases))
