(ns cljs-data-browser.remote-inspector
  (:require [cljs-data-browser.actions :as actions]
            [cljs-data-browser.ui :as ui]
            [clojure.string :as str]))

(set! *warn-on-infer* true)
(defonce app-id (-> js/location.pathname (str/split #"/") (nth 2)))

(defmethod actions/exec-action :default [payload]
  (js/fetch (str "/actions/" app-id) #js {:method "POST" :body payload}))

(def source (js/EventSource. (str "/events/" app-id)))

(.addEventListener source "event" #(ui/render (.-data %)))
