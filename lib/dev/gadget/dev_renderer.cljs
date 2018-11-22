(ns gadget.dev-renderer
  (:require [cljs-atom-browser.ui :as ui]
            [gadget.core :as g]))

(defmethod g/render-data :default [data]
  (ui/render data))
