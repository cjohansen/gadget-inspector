(ns gadget.actions)

(defmulti exec-action (fn [store action args] action))

(defmethod exec-action :set-path [store _ [label path]]
  (swap! store (fn [state]
                 (let [idx (.indexOf (map :label (:atoms state)) label)]
                   (assoc-in state [:atoms idx :path] path)))))

(defmethod exec-action :default [store action args]
  (prn "Unsupported action" action))
