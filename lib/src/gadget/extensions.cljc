(ns gadget.extensions
  (:require [gadget.datafy :as datafy]
            [gadget.core :as gadget]
            [gadget.std :refer [pad]]
            [clojure.string :as str]))

;; JWTs

(defmethod gadget/render [:inline :jwt] [_ {:keys [raw]}]
  [:span {}
   [:strong {} "JWT: "]
   [:gadget/string (pr-str (str (first (str/split raw #"\.")) "..."))]])

(defn- base64json [s]
  #?(:cljs (-> s js/atob js/JSON.parse (js->clj :keywordize-keys true))))

(defrecord JWT [header data sig]
  gadget/Browsable
  (entries [jwt]
    (sort-by (gadget/key-order [:header :data :signature]) jwt)))

(defn parse-jwt [token]
  (let [[header data sig] (str/split token #"\.")]
    (JWT. (base64json header) (base64json data) sig)))

(defmethod datafy/datafy :jwt [token]
  (parse-jwt token))

(def re-jwt #"^[A-Za-z0-9-_=]{4,}\.[A-Za-z0-9-_=]{4,}\.?[A-Za-z0-9-_.+/=]*$")

(datafy/add-type-inference
 (fn [v]
   (when (and (string? v)
              (re-find re-jwt v)
              (try ;; The regex inference is pretty weak. If it can't be parsed,
                   ;; it's likely not a JWT
                (parse-jwt v)
                (catch :default e
                  nil)))
     :jwt)))

;; Dates

(def supports-intl? #?(:cljs (and js/window.Intl js/window.Intl.DateTimeFormat)))
(def date-key-order [:iso :locale-date-string :time :timezone :year :month :date :timestamp])

(defrecord Instant [timestamp iso locale-date-string year month date time timezone]
  gadget/Browsable
  (entries [m]
    (sort-by (gadget/key-order date-key-order) m)))

(defmethod datafy/datafy :date [date]
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
    supports-intl? (assoc :timezone #?(:cljs (.. js/Intl DateTimeFormat resolvedOptions -timeZone)))
    :always map->Instant))

;; JavaScript Objects

(defmethod datafy/datafy :object [obj]
  (->> #?(:cljs (js/Object.keys obj))
       (map (fn [k] [(symbol k) (aget obj k)]))
       (into {})))
