(ns gadget.core-test
  (:require [gadget.core :as sut]
            [gadget.helper :refer [prepped-keys prepped-vals]]
            #?(:clj [clojure.test :refer [deftest testing is]]
               :cljs [cljs.test :refer [deftest testing is]])))

(deftest path-prep-test
  (testing "Formats root path"
    (is (= (:path (sut/prepare-data {:path [] :label "My data"}))
           [{:text "My data"}])))

  (testing "Links earlier path elements"
    (is (= (:path (sut/prepare-data {:path [:some 0 :stuff] :label "My data"}))
           [{:text "My data" :actions {:go [[:set-path "My data" []]]}}
            {:text ":some" :actions {:go [[:set-path "My data" [:some]]]}}
            {:text "0" :actions {:go [[:set-path "My data" [:some 0]]]}}
            {:text ":stuff"}]))))

(deftest prepare-data-test
  (testing "Encodes keys as strings with type hints"
    (is (= (prepped-keys {:a 1
                          "b" 2
                          3 3
                          true 4
                          nil 5
                          :bb 2
                          {:a 2} 6
                          [:a] 7
                          '(:b) 8
                          #{:c} 9
                          'd 10
                          (range 5) 11
                          (range 100) 12})
           [[:gadget/keyword ":a"]
            [:gadget/keyword ":bb"]
            [:gadget/code {} 'd]
            [:gadget/string "\"b\""]
            [:gadget/number "3"]
            [:gadget/inline-coll {:brackets ["{" "}"]
                                  :xs [[:gadget/keyword ":a"] " " [:gadget/number "2"]]}]
            [:gadget/inline-coll {:brackets ["[" "]"]
                                  :xs [[:gadget/keyword ":a"]]}]
            [:gadget/inline-coll {:brackets ["'(" ")"]
                                  :xs [[:gadget/keyword ":b"]]}]
            [:gadget/inline-coll {:brackets ["#{" "}"]
                                  :xs [[:gadget/keyword ":c"]]}]
            [:gadget/link [:gadget/code {} "(100 numbers)"]]
            [:gadget/inline-coll {:brackets ["(" ")"]
                                  :xs [[:gadget/number "0"]
                                       [:gadget/number "1"]
                                       [:gadget/number "2"]
                                       [:gadget/number "3"]
                                       [:gadget/number "4"]]}]
            [:gadget/boolean "true"]
            [:gadget/code {} "nil"]])))

  (testing "Browses vectors"
    (is (= (prepped-keys [1 2 3 4 5])
           [[:gadget/number "0"]
            [:gadget/number "1"]
            [:gadget/number "2"]
            [:gadget/number "3"]
            [:gadget/number "4"]])))

  (testing "Keyword values"
    (is (= (prepped-vals {:a :b})
           [[:gadget/keyword ":b"]])))

  (testing "String values"
    (is (= (prepped-vals {:a "b"})
           [[:gadget/string "\"b\""]])))

  (testing "Number values"
    (is (= (prepped-vals {:a 2})
           [[:gadget/number "2"]])))

  (testing "Boolean values"
    (is (= (prepped-vals {:a true})
           [[:gadget/boolean "true"]])))

  (testing "nil values"
    (is (= (prepped-vals {:a nil})
           [[:gadget/code {} "nil"]])))

  (testing "Symbol values"
    (is (= (prepped-vals {:a 'Symbolic})
           [[:gadget/code {} 'Symbolic]])))

  #?(:cljs (testing "JavaScript object values"
             (is (= (prepped-vals {:a (js/Map.)})
                    [[:gadget/code {}
                      "object["
                      [:strong {} "Map"]
                      "]"
                      nil]])))))

(deftest prepare-maps-test
  (testing "Inlinable map"
    (is (= (prepped-vals {:a {:small "Map"}})
           [[:gadget/inline-coll {:brackets ["{" "}"]
                                  :xs [[:gadget/keyword ":small"]
                                       " "
                                       [:gadget/string "\"Map\""]]}]])))

  (testing "Sorts keys in inlinable map"
    (is (= (prepped-vals {:a {:small "Map" :another "Key" :tiny "Stuff"}})
           [[:gadget/inline-coll
             {:brackets ["{" "}"]
              :xs [[:gadget/keyword ":another"]
                   " "
                   [:gadget/string "\"Key\""]
                   ","
                   " "
                   [:gadget/keyword ":small"]
                   " "
                   [:gadget/string "\"Map\""]
                   ","
                   " "
                   [:gadget/keyword ":tiny"]
                   " "
                   [:gadget/string "\"Stuff\""]]}]])))

  (testing "Inlinable map with nil"
    (is (= (prepped-vals {:a {:small nil}})
           [[:gadget/inline-coll
             {:brackets ["{" "}"]
              :xs [[:gadget/keyword ":small"]
                   " "
                   [:gadget/code {} "nil"]]}]])))

  (testing "Browsable map"
    (is (= (prepped-vals {:a {:slightly "Bigger"
                              "map" "That is inconvenient"
                              :to "Display on a single line"
                              'numbers (range 50)}})
           [[:gadget/code {}
             [:gadget/inline-coll
              {:brackets ["{" "}"]
               :xs [[:gadget/keyword ":slightly"]
                    [:gadget/keyword ":to"]
                    [:gadget/code {} 'numbers]
                    [:gadget/string "\"map\""]]}]]])))

  (testing "Browsable map with too many keys"
    (is (= (prepped-vals {:a (->> 100 range
                                  (map (fn [i] [(str "key" i) i]))
                                  (into {}))})
           [[:gadget/link [:gadget/code {} "{100 keys}"]]])))

  (testing "Summarizes map with string key in braces"
    (is (= (prepped-vals {:invoices {"5505505" [{:dueamount 100 :kid "00000000" :due-data "2018-11-01T00:00:00Z"} {:dueamount 100 :kid "00000001" :due-data "2018-10-01T00:00:00Z"} {:dueamount 100 :kid "00000002" :due-data "2018-09-01T00:00:00Z"} {:dueamount 100 :kid "00000003" :due-data "2018-08-01T00:00:00Z"}]}})
           [[:gadget/code {}
             [:gadget/inline-coll
              {:brackets ["{" "}"]
               :xs [[:gadget/string "\"5505505\""]]}]]]))))

(deftest prepare-set-test
  (testing "Inlinable set"
    (is (= (prepped-vals {:small-set #{:a :b :c}})
           [[:gadget/inline-coll
             {:brackets ["#{" "}"]
              :xs [[:gadget/keyword ":a"]
                   [:gadget/keyword ":b"]
                   [:gadget/keyword ":c"]]}]])))

  (testing "Sorts sets"
    (is (= (prepped-vals {:small-set #{:b :a :c}})
           [[:gadget/inline-coll
             {:brackets ["#{" "}"]
              :xs [[:gadget/keyword ":a"]
                   [:gadget/keyword ":b"]
                   [:gadget/keyword ":c"]]}]])))

  (testing "Browsable set"
    (is (= (prepped-vals {:bigger-set (->> (range 100)
                                           (map #(keyword (str "Item" %)))
                                           set)})
           [[:gadget/link [:gadget/code {} "#{100 keywords}"]]])))

  (testing "Browsable set with single item"
    (is (= (prepped-vals {:set #{(->> (range 100)
                                      (map #(keyword (str "Item" %))))}})
           [[:gadget/link [:gadget/code {} "#{1 seq}"]]]))))

(deftest prepare-vector-test
  (testing "Empty vector"
    (is (= (prepped-vals {:small-vector []})
           [[:gadget/inline-coll {:brackets ["[" "]"], :xs ()}]])))

  (testing "Inlinable vector"
    (is (= (prepped-vals {:small-vector [:a :b :c]})
           [[:gadget/inline-coll
             {:brackets ["[" "]"]
              :xs [[:gadget/keyword ":a"]
                   [:gadget/keyword ":b"]
                   [:gadget/keyword ":c"]]}]])))

  (testing "Browsable vector"
    (is (= (prepped-vals {:bigger-vector (->> (range 100)
                                              (map #(keyword (str "Item" %)))
                                              vec)})
           [[:gadget/link [:gadget/code {} "[100 keywords]"]]]))))

(deftest prepare-list-test
  (testing "Inlinable list"
    (is (= (prepped-vals {:small-list '(:a :b :c)})
           [[:gadget/inline-coll
             {:brackets ["'(" ")"]
              :xs [[:gadget/keyword ":a"]
                   [:gadget/keyword ":b"]
                   [:gadget/keyword ":c"]]}]])))

  (testing "Browsable list"
    (is (= (prepped-vals {:bigger-list (->> (range 100)
                                            (map #(keyword (str "Item" %)))
                                            (into '()))})
           [[:gadget/link [:gadget/code {} "(100 keywords)"]]]))))

(deftest prepare-seq-test
  (testing "Inlinable seq"
    (is (= (prepped-vals {:small-seq (range 3)})
           [[:gadget/inline-coll
             {:brackets ["(" ")"]
              :xs [[:gadget/number "0"]
                   [:gadget/number "1"]
                   [:gadget/number "2"]]}]])))

  (testing "Browsable seq"
    (is (= (prepped-vals {:bigger-list (range 400)})
           [[:gadget/link [:gadget/code {} "(400 numbers)"]]])))

  (testing "Lazy seq"
    (is (= (prepped-vals {:lazy (map #(keyword (str "Item" %)) (range 10000))})
           [[:gadget/link [:gadget/code {} "(1000+ items, click to load 0-1000)"]]]))))

(deftest prepare-navigated-data-test
  (testing "Serves up data at path"
    (is (= (-> {:label "Some data"
                :path [:key]
                :ref (atom {:key {:a 1, :b 2, :token token}})}
               sut/prepare-data
               (select-keys [:path :hiccup]))
           {:path [{:text "Some data", :actions {:go [[:set-path "Some data" []]]}}
                   {:text ":key"}]
            :hiccup [:gadget/browser
                     {:key "Some data-browser"
                      :data [{:k [:gadget/keyword ":a"]
                              :v [:gadget/number "1"]
                              :actions {:go [[:set-path "Some data" [:key :a]]]
                                        :copy [[:copy-to-clipboard "Some data" [:key :a]]]}}
                             {:k [:gadget/keyword ":b"]
                              :v [:gadget/number "2"]
                              :actions {:go [[:set-path "Some data" [:key :b]]]
                                        :copy [[:copy-to-clipboard "Some data" [:key :b]]]}}
                             {:k [:gadget/keyword ":token"]
                              :v [:gadget/string "\"eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c\""]
                              :actions {:go [[:set-path "Some data" [:key :token]]]
                                        :copy [[:copy-to-clipboard "Some data" [:key :token]]]}}]}]}))))

(def calls (atom []))

(defmethod sut/render-data :default [data-fn]
  (swap! calls conj data-fn))

(deftest does-not-call-render-when-paused
  (reset! calls [])
  (sut/pause!)
  (sut/render)
  (is (= [] @calls))
  (sut/resume!))

(deftest renders-when-resumed
  (sut/set-render-debounce-ms! 0)
  (reset! calls [])
  (sut/pause!)
  (sut/resume!)
  (is (= 1 (count @calls))))

(deftest does-not-call-render-when-atom-data-is-not-inspectable
  (sut/set-render-debounce-ms! 0)
  (let [data (atom {:id 12})]
    (reset! calls [])
    (sut/inspect "My data" data {:inspectable? (fn [state] (nil? (:id state)))})
    (swap! data assoc :name "Gadget")
    (is (= 0 (count @calls)))
    (swap! data dissoc :id)
    (is (= 1 (count @calls)))))

(deftest does-not-call-render-when-data-is-not-inspectable
  (sut/set-render-debounce-ms! 0)
  (reset! calls [])
  (sut/inspect "My data" {:id 12} {:inspectable? (fn [state] (nil? (:id state)))})
  (is (= 0 (count @calls)))
  (sut/inspect "My data" {:name "Gadget"} {:inspectable? (fn [state] (nil? (:id state)))})
  (is (= 1 (count @calls))))
