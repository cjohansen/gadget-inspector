(ns cljs-atom-browser.panel)

(set! *warn-on-infer* true)

(def console (.. js/chrome -extension getBackgroundPage -console))

(set! js/window.receiveMessage
      (fn [message]
        (console.log "Got message" (type message) message)))
