(ns gadget.dev
  (:require [clojure.tools.namespace.repl :as repl]
            [gadget.server :as server]
            [ring.adapter.jetty :as jetty]
            [taoensso.timbre :as log])
  (:import java.lang.ProcessHandle))

(log/merge-config! {:level :debug
                    :timestamp-opts {:pattern :iso8601}})

(def server (atom nil))

(defn start []
  (->> (jetty/run-jetty
        #'server/app
        {:port 7117
         :join? false
         :async? true})
       (reset! server))
  (log/info "My pid is" (.pid (ProcessHandle/current))))

(defn stop []
  (when-let [server @server]
    (.stop server)))

(defn restart []
  (stop)
  (repl/refresh :after 'gadget.dev/start))

(defn -main [& args]
  (start))

(comment

  (start)
  (stop)
  (restart)
)
