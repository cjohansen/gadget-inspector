(ns gadget.ideas
  (:require [gadget.core :as gadget]
            ;;[gadget.ui :as ui]
            ))

;; (def re-jwt #"^[A-Za-z0-9-_=]{4,}\.[A-Za-z0-9-_=]{4,}\.?[A-Za-z0-9-_.+/=]*$")

;; (defn- base64json [s]
;;   #?(:cljs (-> s js/atob JSON.parse (js->clj :keywordize-keys true))))

;; (defrecord JWT [header data sig]
;;   Browsable
;;   (entries [jwt]
;;     (sort-by (gadget/key-order [:header :data :signature]) jwt))

;;   FullRenderable
;;   (render [v])

;;   InlineRenderable
;;   (render [v]
;;     (ui/prefixed-literal
;;      {:literal/prefix "JWT:"
;;       :literal/string (str (first (str/split raw #"\.")) "...")}))

;;   SummaryRenderable
;;   (render [v]))

;; (defn parse-jwt [token]
;;   (let [[header data sig] (str/split token #"\.")]
;;     (JWT. (base64json header) (base64json data) sig)))

;; (gadget/add-inspection-converter
;;  (fn [v]
;;    (when (and (string? v)
;;               (re-find re-jwt v))
;;      (try ;; The regex inference is pretty weak. If it can't be parsed,
;;        ;; it's likely not a JWT
;;        (parse-jwt v)
;;        (catch #?(:cljs :default
;;                  :clj Exception) e
;;          nil)))))
