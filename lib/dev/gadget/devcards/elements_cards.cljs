(ns gadget.devcards.elements-cards
  (:require [dumdom.devcards :refer-macros [defcard]]
            [gadget.ui.elements :as e]))

(defn card [component]
  [:div {:style {:display "flex"}}
   [:div {:style {:padding 20
                  :flex "1 0 auto"}} component]
   [:div.dark {:style {:padding 20
                       :flex "1 0 auto"}} component]])

(defcard string-value
  (card (e/value {:type :string
                  :value (pr-str "String!")})))

(defcard number-value
  (card (e/value {:type :number
                  :value (pr-str 42)})))

(defcard keyword-value
  (card (e/value {:type :keyword
                  :value (pr-str :hello)})))

(defcard boolean-value
  (card (e/value {:type :boolean
                  :value (pr-str true)})))

(defcard symbol-value
  (card (e/value {:type :symbol
                  :value (pr-str 'symbolic-things)})))

(defcard literal-value
  (card (e/value {:type :literal
                  :prefix "#inst"
                  :value {:type :string
                          :value (pr-str "2021-01-12T12:00:00Z")}})))

(defcard inline-vector
  (card (e/inline-collection
         {:brackets ["[" "]"]
          :xs (map (fn [v] {:type :keyword
                            :value (pr-str v)})
                   [:a :b :c])})))

(defcard inline-set
  (card (e/inline-collection
         {:brackets ["#{" "}"]
          :xs (map (fn [v] {:type :keyword
                            :value (pr-str v)})
                   [:a :b :c])})))

(defcard inline-map
  (card (e/inline-map
         {:brackets ["{" "}"]
          :xs (->> {:name "Christian"
                    :keys 2}
                   (map (fn [[k v]]
                          [{:type :keyword
                            :value (pr-str k)}
                           {:type (if (string? v)
                                    :string
                                    :number)
                            :value (pr-str v)}])))})))

(defcard inline-map-key-summary
  (card (e/inline-map
         {:brackets ["{" "}"]
          :xs (->> {:name "Christian"
                    :keys 2}
                   (map (fn [[k v]]
                          [{:type :keyword
                            :value (pr-str k)}])))})))

(defcard inline-list
  (card (e/inline-collection
         {:brackets ["(" ")"]
          :xs (map (fn [v] {:type :keyword
                            :value (pr-str v)})
                   [:a :b :c])})))

(defcard link
  (card (e/link {:text "[3 strings]"
                 :actions []})))

(defcard button
  (card (e/button {:title "Copy to clipboard"
                   :text "Copy"
                   :actions []})))

(defcard prefixed-tuple
  [:div
   [:div
    (e/tuple
     {:prefix "datom"
      :values [{:type :number
                :value (pr-str 124)}
               {:type :keyword
                :value (pr-str :user/id)}
               {:type :literal
                :prefix "#uuid"
                :value {:type :string
                        :value (pr-str "941291f5-6661-476d-b839-7d457be329e5")}}]})]
   [:div
    (e/tuple
     {:prefix "datom"
      :values [{:type :number
                :actions []
                :value (pr-str 124)}
               {:type :keyword
                :actions []
                :value (pr-str :user/id)}
               {:type :literal
                :actions []
                :prefix "#uuid"
                :value {:type :string
                        :value (pr-str "941291f5-6661-476d-b839-7d457be329e5")}}]})]])

(defcard prefixed-tuple-dark
  [:div.dark {:style {:padding 20}}
   [:div
    (e/tuple
     {:prefix "datom"
      :values [{:type :number
                :value (pr-str 124)}
               {:type :keyword
                :value (pr-str :user/id)}
               {:type :literal
                :prefix "#uuid"
                :value {:type :string
                        :value (pr-str "941291f5-6661-476d-b839-7d457be329e5")}}]})]
   [:div
    (e/tuple
     {:prefix "datom"
      :values [{:type :number
                :actions []
                :value (pr-str 124)}
               {:type :keyword
                :actions []
                :value (pr-str :user/id)}
               {:type :literal
                :actions []
                :prefix "#uuid"
                :value {:type :string
                        :value (pr-str "941291f5-6661-476d-b839-7d457be329e5")}}]})]])

(defcard linked-tuple
  (card
   (e/tuple
    {:values [{:type :keyword
               :actions []
               :value (pr-str :user/id)}
              {:type :number
               :actions []
               :value (pr-str 245)}]})))
