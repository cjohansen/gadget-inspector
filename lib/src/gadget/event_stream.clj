(ns gadget.event-stream
  (:require [clojure.string :as str])
  (:import java.net.http.HttpClient
           java.net.http.HttpRequest
           java.net.http.HttpResponse
           java.net.http.HttpResponse$BodyHandlers
           java.nio.file.FileSystems
           java.util.concurrent.Flow$Subscription))

(defmulti prepare-event-data (fn [event data] event))

(defmethod prepare-event-data :default [_ data]
  data)

(def event-re #"event: *(.+)$")
(def data-re #"data: *(.+)$")

(defn prepare-message [message]
  (update message :data #(prepare-event-data (:event message) %)))

(defn consume-event-stream-line [next-message line & [opt]]
  (cond
    (empty? line)
    (cond-> {:next-message {}}
      (not (empty? next-message)) (assoc :message (prepare-message next-message)))

    (str/starts-with? line ":")
    (do
      (println "Ignore event stream comment" line)
      {:next-message next-message})

    (re-find event-re line)
    {:next-message (->> ((or (:keyfn opt) identity)
                         (second (re-find event-re line)))
                        (assoc next-message :event))}

    (re-find data-re line)
    (let [data (second (re-find data-re line))]
      {:next-message (update next-message :data #(str % (when % "\n") data))})

    :default
    (println "Ignoring unknown message" line)))

;; FileSystems

;; java.util.concurrent.Flow$Subscriber

(def client (.build (HttpClient/newBuilder)))
(def path (.toPath (clojure.java.io/file "/tmp/sse.log")))

(def url (java.net.URI. "http://localhost:9119"))


(comment

(let [url (java.net.URI. "http://localhost:9119")

      client (.build (java.net.http.HttpClient/newBuilder))
      req (.. (java.net.http.HttpRequest/newBuilder url) GET build)
      response-subscription (atom nil)
      running? (atom true)
      message-state (atom {})
      subscriber (reify java.util.concurrent.Flow$Subscriber
                   (^void onComplete [this]
                    (when-let [message (:message (consume-event-stream-line @message-state ""))]
                      (prn "==>" message))
                    (println "Complete!"))

                   (^void onError [this ^Throwable throwable]
                    (println "Error!")
                    (prn throwable))

                   (^void onNext [this line]
                    (if @running?
                      (let [{:keys [next-message message]} (consume-event-stream-line @message-state line)]
                        (reset! message-state next-message)
                        (when message
                          (prn "==>" message))
                        (.request @response-subscription 1))
                      (.cancel @response-subscription)))

                   (^void onSubscribe [this ^java.util.concurrent.Flow$Subscription subscription]
                    (.request subscription 1)
                    (reset! response-subscription subscription)))
      result (-> client
                 (.sendAsync req (java.net.http.HttpResponse$BodyHandlers/fromLineSubscriber subscriber)))]
  ;;(Thread/sleep 1000)
  ;;(.cancel result true)
  ;;(reset! running? false)
  )

  (let [req (-> (HttpRequest/newBuilder url)
                .GET
                .build)]
    (-> client
        (.send req (HttpResponse$BodyHandlers/ofString))
        .statusCode)))
