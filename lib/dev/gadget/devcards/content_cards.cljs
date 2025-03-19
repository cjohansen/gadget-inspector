(ns gadget.devcards.content-cards
  (:require [dumdom.devcards :refer-macros [defcard]]
            [gadget.ui.content :as c]))

(defn card [component]
  [:div
   [:div {:style {:padding 20}} component]
   [:div.dark {:style {:padding 20}} component]])

(defcard article
  (card
   (c/article
    {:title "It's me, Gadget Inspector"
     :sections [{:text "You are now looking at the Clojure(Script) data browser, Gadget Inspector. To browse all your data here tell Gadget what to inspect:"}
                {:code [:code
                        [:span.s-exp "("]
                        [:span.core-fn "require"]
                        " "
                        [:span.reader-char "'"]
                        [:span.s-exp "["]
                        [:span.symbol "gadget.inspector"]
                        " "
                        [:span.keyword ":as"]
                        " "
                        [:span.symbol "gadget"]
                        [:span.s-exp "]"]
                        [:span.s-exp ")"]
                        [:br] [:br]
                        [:span.s-exp "("]
                        [:span.symbol "gadget/inspect"]
                        " "
                        [:span.string "\"Store\""]
                        " "
                        [:span.symbol "store"]
                        [:span.s-exp ")"]]}
                {:image {:src "/inspector.png"}}]})))
