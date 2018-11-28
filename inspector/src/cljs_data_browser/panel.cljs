(ns cljs-data-browser.panel
  (:require [cljs-data-browser.actions :as actions]
            [cljs-data-browser.ui :as ui]))

(set! *warn-on-infer* true)

(def console (.. js/chrome -extension getBackgroundPage -console))

(defn log [& args]
  (apply console.log args))

(defmethod actions/exec-action :default [payload]
  (js/chrome.tabs.query
   #js {:active true :currentWindow true}
   (fn [tabs]
     (js/chrome.tabs.sendMessage (.-id (aget tabs 0)) payload))))

(set! js/window.receiveMessage
      (fn [message]
        (ui/render (.. message -request -message))))
