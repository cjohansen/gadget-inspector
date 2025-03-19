(ns gadget.server
  (:require [ring.middleware.content-type :refer [wrap-content-type]]
            [ring.middleware.resource :refer [wrap-resource]]
            [clojure.string :as str]
            [clojure.core.match :refer [match]]))

(def meta-data (atom {}))

(defn register-meta-data [client-id req respond raise]
  ;;(swap! meta-data merge client-id (:body req))
  (prn (:body req))
  (respond {:status 200}))

(let [req {:uri "/client/9b7df215-431e-4c50-bc1a-5fd59ed1380a"}]
  (drop 1 (str/split (:uri req) #"/")))

(defn handler [req respond raise]
  (prn (concat [(:request-method req)] (drop 1 (str/split (:uri req) #"/"))))
  (match (into [(:request-method req)] (drop 1 (str/split (:uri req) #"/")))
    [:post "client" client-id] (register-meta-data client-id req respond raise)
    :else (respond {:status 200
                    :body "Hello??"})))

(def app
  (-> #'handler
      (wrap-resource "public")
      wrap-content-type))
