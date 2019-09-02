(ns gadget.std
  (:require [clojure.string :as str]
            #?(:cljs [cljs.core.async :refer [<! alts! chan put! timeout go-loop]]
               :clj [clojure.core.async :refer [<! alts! chan put! timeout go-loop]])))

(defn debounce [f ms]
  (let [c (chan)]
    (go-loop [args (<! c)]
      (let [[value port] (alts! [c (timeout ms)])]
        (if (= port c)
          (recur value)
          (do ;; or timed out
            (apply f args)
            (recur (<! c))))))
    (fn [& args]
      (put! c (or args [])))))

(defn date? [v]
  #?(:cljs (instance? js/Date v)))

(defn state-data [state label]
  (let [{:keys [ref data]} (get-in state [:data label])]
    (if ref @ref data)))

(defn pad [n]
  (if (< n 10)
    (str "0" n)
    (str n)))
