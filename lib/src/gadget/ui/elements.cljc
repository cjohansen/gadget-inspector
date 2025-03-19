(ns gadget.ui.elements
  (:require [clojure.string :as str]))

(defn string-value [s]
  [:code.string s])

(defn number-value [n]
  [:code.number n])

(defn keyword-value [kw]
  [:code.keyword kw])

(defn boolean-value [b]
  [:code.boolean b])

(defn symbol-value [b]
  [:code.symbol b])

(declare value)

(defn literal-value [opt]
  [:span
   [:code [:strong (:prefix opt)]]
   " "
   (value (:value opt))])

(defn value [opt]
  (case (:type opt)
    :string (string-value (:value opt))
    :number (number-value (:value opt))
    :keyword (keyword-value (:value opt))
    :boolean (boolean-value (:value opt))
    :symbol (symbol-value (:value opt))
    :literal (literal-value opt)
    (:value opt)))

(defn inline-collection [{:keys [brackets xs]}]
  [:span
   [:strong (first brackets)]
   (interpose " " (map value xs))
   [:strong (second brackets)]])

(defn inline-map [{:keys [brackets xs]}]
  [:span
   [:strong (first brackets)]
   (->> (for [kv xs]
          (interpose " " (map value kv)))
        (interpose [:strong ", "]))
   [:strong (second brackets)]])

(defn tuple [opt]
  [:span.tuple
   [:code [:strong (:prefix opt) " "]]
   [:strong "["]
   (->> (:values opt)
        (map (fn [tv]
               [(if (:actions tv) :a.tuple-item :span.tuple-item)
                (when (:actions tv)
                  {:onClick (:actions tv)})
                (value tv)])))
   [:strong "]"]])

(defn link [v]
  [:a.link (dissoc v :text) [:code (:text v)]])

(defn button [{:keys [text title actions]}]
  [:a.button {:onClick actions
              :title title} text])
