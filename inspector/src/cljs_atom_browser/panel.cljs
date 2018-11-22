(ns cljs-atom-browser.panel
  (:require [cljs-atom-browser.ui :as ui]))

(set! *warn-on-infer* true)

(def console (.. js/chrome -extension getBackgroundPage -console))

(defn log [& args]
  (apply console.log args))

(set! js/window.receiveMessage
      (fn [message]
        (ui/render (.. message -request -message))))
