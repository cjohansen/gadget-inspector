(ns gadget.extensions
  (:require [gadget.core :as gadget]
            [clojure.string :as str]))

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
    {:header (base64json header)
     :data (base64json data)
     :signature sig}))
