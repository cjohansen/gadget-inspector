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

(defn diffs-at [diff path]
  (loop [[p & path] path
         diff diff]
    (cond
      (or (:old diff) (:new diff))
      diff

      (nil? p)
      {:keys (->> diff
                  (mapcat (fn [[change ks]]
                            (map (fn [k] {k change}) (if (map? ks) (keys ks) ks))))
                  (apply merge))}

      :default
      (recur path (get-in diff [:changed p])))))
