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

(defmethod exec-action :assoc-state [store _ [k v]]
  (swap! store assoc-in (concat [:data] k) v))

(defn uninspect [store k]
  (swap! store update :data dissoc k)
  (swap! store update :config dissoc k))

(defmethod exec-action :uninspect [store _ [k]]
  (uninspect store k))

(defmethod exec-action :unsubscribe [store _ [k]]
  (remove-watch (get-in @store [:refs k]) :gadget/inspector)
  (swap! store update :refs dissoc k)
  (uninspect store k))

(defmethod exec-action :copy-to-clipboard [store _ [label path]]
  (to-clipboard (pr-str (nav-in (state-data @store label) path))))

(defmethod exec-action :set-window-size [store _ [{:keys [width height]}]]
  (swap! store assoc :window {:width width :height height}))

(defmethod exec-action :default [store action args]
  (prn "Unsupported action" action))
