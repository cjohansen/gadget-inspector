(ns cljs-data-browser.ui
  (:require [cljs-data-browser.inspector :refer [Inspector]]
            [cljs.reader :as reader]
            [dumdom.core :as q]))

(defn render [data]
  (q/render
   (Inspector (:data (reader/read-string data)))
   (js/document.getElementById "app")))
