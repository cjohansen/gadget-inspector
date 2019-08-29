(ns gadget.actions
  (:require [gadget.datafy :refer [nav-in]]
            [gadget.std :refer [state-data]]))

(defn to-clipboard [text]
  #?(:cljs
     (let [text-area (js/document.createElement "textarea")]
       (set! (.-textContent text-area) text)
       (js/document.body.appendChild text-area)
       (.select text-area)
       (js/document.execCommand "copy")
       (.blur text-area)
       (js/document.body.removeChild text-area))))

(defmulti exec-action (fn [store action args] action))

(defmethod exec-action :set-path [store _ [label path]]
  (swap! store assoc-in [:data label :path] path))

(defmethod exec-action :copy-to-clipboard [store _ [label path]]
  (to-clipboard (pr-str (nav-in (state-data @store label) path))))

(defmethod exec-action :set-window-size [store _ [{:keys [width height]}]]
  (swap! store assoc :window {:width width :height height}))

(defmethod exec-action :default [store action args]
  (prn "Unsupported action" action))
