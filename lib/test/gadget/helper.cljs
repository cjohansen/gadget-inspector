(ns gadget.helper
  (:require [gadget.core :as gadget]))

(defn prepped-keys [data]
  (->> {:ref (atom data)
        :label "My data"}
       gadget/prepare-data
       :hiccup
       second
       :data
       (map :k)))

(defn prepped-vals [data]
  (->> {:ref (atom data)
        :label "My data"}
       gadget/prepare-data
       :hiccup
       second
       :data
       (map :v)))
