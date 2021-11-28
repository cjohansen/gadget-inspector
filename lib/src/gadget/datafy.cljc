(ns gadget.datafy
  (:require [clojure.datafy :as datafy]
            [gadget.browsable :as browsable]
            [gadget.std :refer [date?]]))

(def type-fns (atom nil))

(defn add-type-inference [f]
  (swap! type-fns conj f))

(defn symbolic-type [v]
  (cond
    (string? v) :string
    (keyword? v) :keyword
    (number? v) :number
    (boolean? v) :boolean
    (map? v) :map
    (vector? v) :vector
    (list? v) :list
    (nil? v) :nil
    (set? v) :set
    (symbol? v) :symbol
    (seq? v) :seq
    (uuid? v) :uuid
    (date? v) :date
    :default :object))

(defn synthetic-type [value]
  (loop [[f & fs] @type-fns]
    (or (when f (f value))
        (if-let [fs (seq fs)]
          (recur fs)
          (symbolic-type value)))))

(defmulti datafy (fn [data] (synthetic-type data)))

(defmethod datafy :default [data]
  (datafy/datafy data))

(defn nav-in [data path]
  (if-let [p (first path)]
    (let [data (datafy data)
          navigable (if (satisfies? browsable/Browsable data)
                      (into {} (browsable/entries data))
                      data)]
      (recur (datafy/nav data p (get navigable p)) (rest path)))
    data))
