(ns ^:figwheel-hooks gadget.devcards
  (:require [devcards.core :as devcards]
            [gadget.devcards.browser-cards]
            [gadget.devcards.elements-cards]
            [gadget.devcards.content-cards]))

(enable-console-print!)

(defn render []
  (devcards/start-devcard-ui!))

(defn ^:after-load render-on-relaod []
  (render))

(render)
