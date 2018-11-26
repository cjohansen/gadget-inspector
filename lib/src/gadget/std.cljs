(ns gadget.std
  (:require [clojure.string :as str]))

(defmulti get* (fn [data path] path))

(defn- base64json [s]
  (-> s js/atob JSON.parse (js->clj :keywordize-keys true)))

(defmethod get* :gadget/JWT [data path]
  (when (string? data)
    (let [[header data sig] (str/split data #"\.")]
      {:header (base64json header)
       :data (base64json data)
       :signature sig})))

(defmethod get* :default [data path]
  (if (and (seq? data) (number? path))
    (nth data path)
    (get data path)))

(defn get-in* [data path]
  (if-let [p (first path)]
    (recur (get* data p) (rest path))
    data))

(defn state-data [state label]
  (let [{:keys [ref data]} (get-in state [:atoms label])]
    (if ref @ref data)))

