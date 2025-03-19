(ns gadget.inflection
  (:require [clojure.string :as str]))

(def vowels #{"a" "e" "i" "o" "u"})

(defn inflect [w]
  (if (< (count w) 2)
    (str w "s")
    (let [[a b] (map str (take-last 2 w))
          ending (str/join [a b])]
      (cond
        (or (#{"s" "x" "o"} b)
            (#{"ss" "sh" "ch"} ending))
        (str w "es")

        (and (not (vowels a))
             (= "y" b))
        (str (str/join (drop-last 1 w)) "ies")

        :else (str w "s")))))

(comment
  (inflect "a")
  (inflect "ar")
  (inflect "fox")
  (inflect "potato")
  (inflect "cry")
  (inflect "key")
  (inflect "entity")
)
