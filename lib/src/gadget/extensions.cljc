(ns gadget.extensions
  (:require [gadget.core :as gadget]
            [clojure.string :as str]))

;; JWTs

(def re-jwt #"^[A-Za-z0-9-_=]{4,}\.[A-Za-z0-9-_=]{4,}\.?[A-Za-z0-9-_.+/=]*$")

(gadget/add-type-inference
 (fn [v]
   (when (and (string? v) (re-find re-jwt v))
     :jwt)))

(defmethod gadget/render [:inline :jwt] [_ {:keys [raw]}]
  [:span {}
   [:strong {} "JWT: "]
   [:gadget/string (pr-str (str (first (str/split raw #"\.")) "..."))]])

(defn- base64json [s]
  #?(:cljs (-> s js/atob JSON.parse (js->clj :keywordize-keys true))))

(defmethod gadget/datafy :jwt [token]
  (let [[header data sig] (str/split token #"\.")]
    (with-meta
      {:header (base64json header)
       :data (base64json data)
       :signature sig}
      {:gadget/sort (gadget/key-order [:header :data :signature])})))

;; Dates

(def supports-intl? #?(:cljs (and js/window.Intl js/window.Intl.DateTimeFormat)))

(defn- pad [n]
  (if (< n 10)
    (str "0" n)
    (str n)))

(def date-key-order [:iso :locale-date-string :time :timezone :year :month :date :timestamp])

(defmethod gadget/datafy :date [date]
  (with-meta
    (cond-> {:timestamp (.getTime date)
             :iso (.toISOString date)
             :locale-date-string #?(:cljs (.toLocaleDateString date "en-US" (clj->js {:weekday "long"
                                                                                      :year "numeric"
                                                                                      :month "long"
                                                                                      :day "numeric"}))
                                    :clj nil)
             :year (+ 1900 (.getYear date))
             :month (inc (.getMonth date))
             :date (.getDate date)
             :time (str (pad (.getHours date)) ":" (pad (.getMinutes date)) ":" (pad (.getSeconds date)))}
      supports-intl? (assoc :timezone #?(:cljs (.. js/Intl DateTimeFormat resolvedOptions -timeZone))))
    {:gadget/sort (gadget/key-order date-key-order)}))
