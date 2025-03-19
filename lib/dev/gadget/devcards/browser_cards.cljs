(ns gadget.devcards.browser-cards
  (:require [dumdom.devcards :refer-macros [defcard]]
            [gadget.ui.browser :as b]
            [gadget.ui.elements :as e]))

(defn card [component]
  [:div
   [:div {:style {:padding 20}} component]
   [:div.dark {:style {:padding 20}} component]])

(defcard table
  (card
   (b/table
    {:entries
     [{:k [:code.symbol "^meta"]
       :v (e/inline-map
           {:brackets ["{" "}"]
            :xs [[{:type :keyword
                   :value (pr-str :git-sha)}]
                 [{:type :keyword
                   :value (pr-str :build-date)}]]})
       :onClick []
       :button {:text "Copy" :actions []}}
      {:k [:code.keyword ":name"]
       :v [:code.string "\"Christian\""]
       :onClick []
       :button {:text "Copy" :actions []}}
      {:k [:code.keyword ":language"]
       :v [:code.string "\"Clojure\""]
       :onClick []
       :button {:text "Copy" :actions []}}
      {:k [:code.keyword ":environment"]
       :v [:code.string "\"Browser\""]
       :onClick []
       :button {:text "Copy" :actions []}}
      {:k [:code.keyword ":xs"]
       :v [:a.link {:actions []} [:code "[3 maps]"]]
       :onClick []
       :button {:text "Copy"
                :actions []}}]})))

(defcard datascript-index
  (card
   (b/table
    {:entries
     (->> [(e/tuple
            {:prefix "datom"
             :values [{:type :number
                       :value (pr-str 1)
                       :actions []}
                      {:type :keyword
                       :value (pr-str :user/id)
                       :actions []}
                      {:type :literal
                       :prefix "#uuid"
                       :value (pr-str "ea75f3b6-3f91-486b-900d-5e7fcae9fd55")}
                      {:type :number
                       :value (pr-str 536870913)
                       :actions []}
                      {:type :boolean
                       :value (pr-str true)}]})
           (e/tuple
            {:prefix "datom"
             :values [{:type :number
                       :value (pr-str 1)
                       :actions []}
                      {:type :keyword
                       :value (pr-str :user/name)
                       :actions []}
                      {:type :string
                       :value (pr-str "Christian Johansen")}
                      {:type :number
                       :value (pr-str 536870913)
                       :actions []}
                      {:type :boolean
                       :value (pr-str true)}]})
           (e/tuple
            {:prefix "datom"
             :values [{:type :number
                       :value (pr-str 2)
                       :actions []}
                      {:type :keyword
                       :value (pr-str :user/id)
                       :actions []}
                      {:type :literal
                       :prefix "#uuid"
                       :value (pr-str "86ad3085-bc0b-4cc2-bab9-cd0ba583a58b")}
                      {:type :number
                       :value (pr-str 536870914)
                       :actions []}
                      {:type :boolean
                       :value (pr-str true)}]})
           (e/tuple
            {:prefix "datom"
             :values [{:type :number
                       :value (pr-str 2)
                       :actions []}
                      {:type :keyword
                       :value (pr-str :user/friends)
                       :actions []}
                      {:type :number
                       :value (pr-str 1)}
                      {:type :number
                       :value (pr-str 536870914)
                       :actions []}
                      {:type :boolean
                       :value (pr-str true)}]})]
          (map-indexed (fn [idx d]
                         {:v d})))})))

(def path-entries
  [{:text "."
    :actions []}
   {:type :keyword
    :value (pr-str :token)
    :actions []}
   {:type :keyword
    :value (pr-str :data)
    :actions []}
   {:type :keyword
    :value (pr-str :email)}])

(defcard path
  (card (b/path {:entries path-entries})))

(defcard browser
  (card
   (b/browser
    {:button {:text "Copy"
              :actions []}
     :path {:entries path-entries}
     :entries
     [{:k (e/symbol-value "Type")
       :v (e/string-value "String")}
      {:k (e/symbol-value "Value")
       :v (e/string-value (pr-str "christian@cjohansen.no"))}]})))

(defcard toolbar
  (card
   (b/toolbar
    {:tabs [{:text [:strong "Store"]
             :actions []}
            {:text "Browse"
             :selected? true
             :actions []}
            {:text "Transactions"
             :actions []}]})))

(defcard inspector-panel
  (card
   (b/panel
    {:button {:text "Copy"
              :actions []}
     :path {:entries path-entries}
     :entries
     [{:k (e/symbol-value "Type")
       :v (e/string-value "String")}
      {:k (e/symbol-value "Value")
       :v (e/string-value (pr-str "christian@cjohansen.no"))}]
     :tabs [{:text [:strong "Store"]
             :actions []}
            {:text "Browse"
             :selected? true
             :actions []}
            {:text "Transactions"
             :actions []}]})))
