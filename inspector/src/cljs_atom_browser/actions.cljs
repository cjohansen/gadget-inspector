(ns cljs-atom-browser.actions)

(defmulti exec-action (fn [action args] action))
