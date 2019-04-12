(ns gadget.dev-renderer
  (:require [cljs-data-browser.ui :as ui]
            [gadget.core :as g]))

(defmethod g/render-data :default [data-fn]
  (ui/render (data-fn)))
