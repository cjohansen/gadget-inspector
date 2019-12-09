(ns cljs-data-browser.panel
  (:require [cljs-data-browser.actions :as actions]
            [cljs-data-browser.ui :as ui]))

(set! *warn-on-infer* true)

(def console (.. js/window -console))

(defn log [& args]
  (apply console.log args))

(defmethod actions/exec-action :default [payload]
  (if (and (exists? js/chrome) js/chrome.tabs)
    (js/chrome.tabs.query
     #js {:active true :currentWindow true}
     (fn [tabs]
       (js/chrome.tabs.sendMessage (.-id (aget tabs 0)) payload)))
    (-> (str "window.postMessage({id: \"cljs-data-browser-action\",message: "
             (pr-str payload) "}, \"*\");")
        js/browser.devtools.inspectedWindow.eval
        (.then (fn [res])))))

(actions/exec-action "{:action :ping}")

(set! js/window.receiveMessage
      (fn [message]
        (ui/render (.. message -request -message))))
