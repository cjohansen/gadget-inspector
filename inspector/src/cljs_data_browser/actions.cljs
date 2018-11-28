(ns cljs-data-browser.actions)

(defmulti exec-action (fn [action args] action))
