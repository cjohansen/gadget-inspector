(ns gadget.inspector
  (:require [gadget.core :as g]
            [gadget.extensions]
            [cheshire.core :as json]
            [clj-http.client :as client]
            [clj-event-source.core.async :as event-source]
            [clojure.core.async :refer [<! go-loop close!]]))

(defn user-agent []
  (format "%s %s PID %s"
          (System/getProperty "java.vm.name")
          (System/getProperty "java.version")
          (.pid (java.lang.ProcessHandle/current))))

(defn host []
  (.getHostName (java.net.InetAddress/getLocalHost)))

(def ^:dynamic *remote-inspector* "http://localhost:7117")

(def ^:private client-id (atom nil))

(defn relay-actions [url]
  (let [event-stream (event-source/connect url)]
    (go-loop []
      (when-let [msg (<! event-stream)]
        (when (#{:message :event} (:kind msg))
          (g/action (-> msg :content :data)))
        (recur)))))

(defn- ensure-connection []
  (when *remote-inspector*
    (if-let [cid @client-id]
      cid
      (try
        (prn "===>" (str *remote-inspector* "/clients"))
        (reset! client-id (-> (client/post (str *remote-inspector* "/clients")
                                           {:headers {"content-type" "application/json"}
                                            :body (json/generate-string
                                                    {:userAgent (user-agent) :host (host)})})
                              :body
                              (json/parse-string keyword)
                              :id))
        (relay-actions (str *remote-inspector* "/events/" @client-id))
        (catch Exception e
          nil)))))

(defmethod g/render-data :default [data-fn]
  (when-let [client-id (ensure-connection)]
    (prn "===>" (str *remote-inspector* "/events/" client-id))
    (client/post
     (str *remote-inspector* "/events/" client-id)
     {:body (data-fn)})
    client-id))

(comment

  (g/inspect
   "Some Clojure data 21"
   {;;:prone (java.util.Date.)
    :propertyId "1b8c12ae-7896-3fcc-bfde-dbe9505d04ec"
    :lol "HAHA"
    ;;:date (java.util.Date.)
    })

  (def res
    (client/post (str *remote-inspector* "/clients")
                 {:headers {"content-type" "application/json"}
                  :body (format "{\"userAgent\": \"%s\", \"host\": \"%s\"}" (user-agent) (host))}))

  (def hmm (atom nil))

  (reset! hmm {:id 32})


  (json/parse-string "{\"a\": 23}" keyword)

  (client/post "https://www.vg.no/clients"
               {:headers {"content-type" "application/json"}
                :body (format "{\"userAgent\": \"%s\", \"host\": \"%s\"}" (user-agent) (host))})

  res
  (def client-id (get (json/parse-string (:body res)) "id"))



  (.getHostName (java.net.InetAddress/getLocalHost))
  (.pid (java.lang.ProcessHandle/current))
  (System/getProperty "java.vm.name")
  (System/getProperty "java.vm.version")
  (System/getProperty "java.vm.vendor")
  (System/getProperty "java.home")
  (System/getProperty "java.vendor")
  (System/getProperty "java.version")
  (System/getProperty "java.specification.vendor")
  (System/getProperty "java.specification.version")
  (System/getProperty "java.specification.name"))




(comment

  (require '[clj-event-source.core.async :as event-source]
           '[clojure.core.async :refer [<! go-loop close!]])

  (let [event-stream (event-source/connect "http://localhost:9119/event-stream")]
    (go-loop []
      (if-let [msg (<! event-stream)]
        (do
          (case (:kind msg)
            :error (println "Encountered error" (:content msg))
            :message (let [{:keys [id data]} (:content msg)]
                       (println "Message without event" id data))
            :event (let [{:keys [id event data]} (:content msg)]
                     (println "Message with event" id event data)))
          (recur))
        (println "Event source disconnected")))
    (Thread/sleep 60000)
    (close! event-stream))

  )
