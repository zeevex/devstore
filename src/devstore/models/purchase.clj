(ns devstore.models.purchase
  (:use [clj-time [coerce :only [to-long]]])
  (:require [clj-time.core :as time]))

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
  (let [id (str "devstore-" (to-long (time/now)))]
    (dosync
     (if-not (find @purchases id)
       ;; reserve the new invoice name with a nil valued entry
       (do (alter purchases merge [id {:created-at (time/now)}])
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

(defn set-status
  "Set STATUS for purchase identified by INVOICE."
  [invoice status]
  (dosync (alter purchases assoc-in [invoice :status] status)))

(defn find-by-invoice
  "Locate a purchase by its INVOICE.
Returns a map containing :purchase, :ipn, and :pdt maps."
  [invoice]
  (when-let [[_ entry] (find @purchases invoice)]
    entry))

(defn find-by-cart-id
  "Locate purchases for a cart, identified by its ID.
Purchases older than CUTOFF-TIME are omitted if it's non-nil.

Returns a sequence of purchases."
  [id & [cutoff-time]]
  (letfn [(selectively [[_ {:keys [purchase created-at] :as entry}]]
            (and (= id (:id purchase))
                 (or (nil? cutoff-time)
                     (time/after? created-at cutoff-time))
                 ;; ignore purchases we never did anything with,
                 ;; cancelled purchases will still show
                 (some #(% entry) [:ipn :pdt :status])))]
    (filter selectively @purchases)))
