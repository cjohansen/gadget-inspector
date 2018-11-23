(ns gadget.inspector
  (:require [clojure.walk :as walk]
            [gadget.core :as g]))

(defmethod g/render-data :default [data]
  (when js/cljs_atom_browser
    (js/cljs_atom_browser data)))

(def inspect g/inspect)
(def create-atom g/create-atom)

(defonce listener
  (js/window.addEventListener
   "message"
   (fn [event]
     (when (= (.. event -data -id) "cljs-atom-browser-action")
       (g/action (.. event -data -message))))))
