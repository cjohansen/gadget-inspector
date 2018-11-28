(ns gadget.diff
  (:require [clojure.set :as set]))

(defn diff [old new]
  (if (and (map? old) (map? new))
    (let [new-ks (set (keys new))
          old-ks (set (keys old))]
      {:added (set/difference new-ks old-ks)
       :removed (set/difference old-ks new-ks)
       :changed (->> old-ks
                     (keep #(when (and (contains? new %)
                                       (not= (get old %) (get new %)))
                              [% (diff (get old %) (get new %))]))
                     (into {}))})
    {:old old :new new}))
