(ns gadget.browsable)

(defprotocol Browsable
  :extend-via-metadata true
  (entries [data] "Return a sorted seq of key value pairs for browsing"))

(extend-type #?(:cljs cljs.core/PersistentVector
                :clj clojure.lang.PersistentVector)
  Browsable
  (entries [v] (map-indexed vector v)))

(extend-type #?(:cljs cljs.core/List
                :clj clojure.lang.PersistentList)
  Browsable
  (entries [l] (map-indexed vector l)))

(extend-type #?(:cljs cljs.core/PersistentHashSet
                :clj clojure.lang.PersistentHashSet)
  Browsable
  (entries [s] (->> s
                    (sort-by pr-str)
                    (map-indexed (comp #(with-meta % {:synthetic-key? true}) vector)))))

(def lazy-sample 1000)

;; TODO: Make it possible to browse other sections of the seq than [0-1000]
(extend-type #?(:cljs cljs.core/LazySeq
                :clj clojure.lang.LazySeq)
  Browsable
  (entries [s] (->> s
                    (take lazy-sample)
                    (map-indexed vector))))

(extend-type #?(:cljs cljs.core/IndexedSeq
                :clj clojure.lang.IndexedSeq)
  Browsable
  (entries [s] (map-indexed vector s)))
