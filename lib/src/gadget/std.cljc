(ns gadget.std
  (:require [clojure.string :as str]))

(defn date? [v]
  #?(:cljs (instance? js/Date v)))

(defmulti get* (fn [data path] path))

(defn- base64json [s]
  #?(:cljs (-> s js/atob JSON.parse (js->clj :keywordize-keys true))))

(defmethod get* :gadget/JWT [data path]
  (when (string? data)
    (let [[header data sig] (str/split data #"\.")]
      {:header (base64json header)
       :data (base64json data)
       :signature sig})))

(def supports-intl? #?(:cljs (and js/window.Intl js/window.Intl.DateTimeFormat)))

(defn- pad [n]
  (if (< n 10)
    (str "0" n)
    (str n)))

(defmethod get* :gadget/inst [data path]
  (when (date? data)
    (cond-> {:timestamp (.getTime data)
             :iso (.toISOString data)
             :date #?(:cljs (.toLocaleDateString data "en-US" (clj->js {:weekday "long" :year "numeric" :month "long" :day "numeric"}))
                      :clj nil)
             :time (str (pad (.getHours data)) ":" (pad (.getMinutes data)) ":" (pad (.getSeconds data)))}
      supports-intl? (assoc :timezone #?(:cljs (.. js/Intl DateTimeFormat resolvedOptions -timeZone))))))

(defmethod get* :default [data path]
  (if (and (seq? data) (number? path))
    (nth data path)
    (get data path)))

(defn get-in* [data path]
  (if-let [p (first path)]
    (recur (get* data p) (rest path))
    data))

(defn state-data [state label]
  (let [{:keys [ref data]} (get-in state [:data label])]
    (if ref @ref data)))
