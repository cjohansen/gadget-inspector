(ns gadget.inspector
  (:require [clojure.walk :as walk]
            [gadget.core :as g]))

(def ^:dynamic *remote-inspector*
  (when (and (not js/window.cljs_data_browser)
             (re-find #"^http://localhost(:|$)" js/location.origin))
    "http://localhost:7117"))

(def event-source nil)
(def client-id nil)

(defn ensure-event-source []
  (if (and (nil? event-source) *remote-inspector*)
    (-> (js/fetch (str *remote-inspector* "/clients")
                  #js {:method "POST"
                       :mode "cors"
                       :headers #js {"content-type" "application/json"}
                       :body (js/JSON.stringify #js {:userAgent js/navigator.userAgent
                                                     :host js/location.host})})
        (.then #(.json %))
        (.then #(js->clj % :keywordize-keys true))
        (.then
         (fn [{:keys [id]}]
           (def event-source
             (let [source (js/EventSource. (str *remote-inspector* "/events/" id))]
               (.addEventListener source "action" (fn [event] (g/action (.-data event))))
               source))
           (def client-id id)
           id)))
    (js/Promise.resolve client-id)))

(defmethod g/render-data :default [data]
  (let [inspector *remote-inspector*]
    (cond
      js/window.cljs_data_browser (js/cljs_data_browser data)
      inspector (-> (ensure-event-source)
                    (.then (fn [client-id]
                             (js/fetch (str inspector "/events/" client-id)
                                       (clj->js {:method "POST" :mode "cors" :body data}))))
                    (.catch (fn []
                              (def ^:dynamic *remote-inspector* nil)))))))

(def inspect g/inspect)
(def create-atom g/create-atom)

(defonce listener
  (js/window.addEventListener
   "message"
   (fn [event]
     (when (= (.. event -data -id) "cljs-data-browser-action")
       (g/action (.. event -data -message))))))
