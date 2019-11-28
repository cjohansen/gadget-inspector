(ns cljs-data-browser.panel
  (:require [cljs-data-browser.actions :as actions]
            [cljs-data-browser.ui :as ui]))

(set! *warn-on-infer* true)

(def console (.. js/window -console))

(defn log [& args]
  (apply console.log args))

(defmethod actions/exec-action :default [payload]
   (let [msg #js {"id" "cljs-data-browser-2"
                  "tabId" js/browser.devtools.inspectedWindow.tabId
                  "payload" payload}]
        (js/browser.runtime.sendMessage msg)))

(set! js/window.receiveMessage
      (fn [message]
        (ui/render (.. message -request -message))))
