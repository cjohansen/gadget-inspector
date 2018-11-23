(ns gadget.core-test
  (:require [gadget.core :as sut]
            #?(:clj [clojure.test :refer [deftest testing is]]
               :cljs [cljs.test :refer [deftest testing is]])))

(deftest path-prep-test
  (testing "Formats root path"
    (is (= (:path (sut/prepare-data {:path [] :label "My data"}))
           [{:text "My data"}])))

  (testing "Links earlier path elements"
    (is (= (:path (sut/prepare-data {:path [:some 0 :stuff] :label "My data"}))
           [{:text "My data" :actions [[:set-path "My data" []]]}
            {:text ":some" :actions [[:set-path "My data" [:some]]]}
            {:text "0" :actions [[:set-path "My data" [:some 0]]]}
            {:text ":stuff"}])))

  (testing "Links virtual path elements"
    (is (= (:path (sut/prepare-data {:path [:some 0 :token :gadget/JWT :data]
                                     :label "My data"}))
           [{:text "My data" :actions [[:set-path "My data" []]]}
            {:text ":some" :actions [[:set-path "My data" [:some]]]}
            {:text "0" :actions [[:set-path "My data" [:some 0]]]}
            {:text ":token" :actions [[:set-path "My data" [:some 0 :token]]]}
            {:text "JWT" :actions [[:set-path "My data" [:some 0 :token :gadget/JWT]]]}
            {:text ":data"}]))))

(defn prepped-keys [data]
  (->> {:ref (atom data)
        :label "My data"}
       sut/prepare-data
       :data
       keys
       (map #(select-keys % [:val :type]))
       set))

(defn prepped-vals [data]
  (->> {:ref (atom data)
        :label "My data"}
       sut/prepare-data
       :data
       vals
       (map #(select-keys % [:val :type :constructor :actions]))))

(deftest prepare-data-test
  (testing "Encodes keys as strings with type hints"
    (is (= (prepped-keys {:a 1
                          "b" 2
                          3 3
                          true 4
                          nil 5
                          {:a 2} 6
                          [:a] 7
                          '(:b) 8
                          #{:c} 9
                          'd 10
                          (range 5) 11
                          (range 100) 12})
           #{{:val ":a" :type :keyword}
             {:val "\"b\"" :type :string}
             {:val "3" :type :number}
             {:val "true" :type :boolean}
             {:val "nil" :type :nil}
             {:val "{:a 2}" :type :map}
             {:val "[:a]" :type :vector}
             {:val "(:b)" :type :list}
             {:val "#{:c}" :type :set}
             {:val "d" :type :symbol}
             {:val "(0 1 2 3 4 5 6 7 8 9 ...)" :type :seq}
             {:val "(0 1 2 3 4)" :type :seq}})))

  (testing "Keyword values"
    (is (= (prepped-vals {:a :b})
           [{:val ":b" :type :keyword}])))

  (testing "String values"
    (is (= (prepped-vals {:a "b"})
           [{:val "\"b\"" :type :string}])))

  (testing "Number values"
    (is (= (prepped-vals {:a 2})
           [{:val "2" :type :number}])))

  (testing "Boolean values"
    (is (= (prepped-vals {:a true})
           [{:val "true" :type :boolean}])))

  (testing "nil values"
    (is (= (prepped-vals {:a nil})
           [{:val "nil" :type :nil}])))

  (testing "Symbol values"
    (is (= (prepped-vals {:a 'Symbolic})
           [{:val "Symbolic" :type :symbol}])))

  #?(:cljs (testing "JavaScript object values"
             (is (= (prepped-vals {:a (js/Map.)})
                    [{:val "object[Map]" :type :object :constructor "Map"}])))))

(deftest prepare-maps-test
  (testing "Inlinable map"
    (is (= (prepped-vals {:a {:small "Map"}})
           [{:type :map :val {{:val ":small" :type :keyword} {:val "\"Map\"" :type :string}}}])))

  (testing "Inlinable map with nil"
    (is (= (prepped-vals {:a {:small nil}})
           [{:type :map :val {{:val ":small" :type :keyword} {:val "nil" :type :nil}}}])))

  (testing "Browsable map"
    (is (= (prepped-vals {:a {:slightly "Bigger"
                              :map "That is inconvenient"
                              :to "Display on a single line"
                              :numbers (range 50)}})
           [{:type :map-keys
             :val [{:val ":map" :type :keyword}
                   {:val ":numbers" :type :keyword}
                   {:val ":slightly" :type :keyword}
                   {:val ":to" :type :keyword}]
             :actions [[:set-path "My data" [:a]]]}])))

  (testing "Browsable map with too many keys"
    (is (= (prepped-vals {:a (->> 100 range
                                  (map (fn [i] [(str "key" i) i]))
                                  (into {}))})
           [{:type :summary
             :val "{100 keys}"
             :actions [[:set-path "My data" [:a]]]}])))

  (testing "Summarizes map with string key in braces"
    (is (= (prepped-vals {:invoices {"5505505" [{:dueamount 100 :kid "00000000" :due-data "2018-11-01T00:00:00Z"} {:dueamount 100 :kid "00000001" :due-data "2018-10-01T00:00:00Z"} {:dueamount 100 :kid "00000002" :due-data "2018-09-01T00:00:00Z"} {:dueamount 100 :kid "00000003" :due-data "2018-08-01T00:00:00Z"}]}})
           [{:val [{:val "\"5505505\"" :type :string}]
             :type :map-keys
             :actions [[:set-path "My data" [:invoices]]]}]))))

(deftest prepare-set-test
  (testing "Inlinable set"
    (is (= (prepped-vals {:small-set #{:a :b :c}})
           [{:type :set :val #{{:type :keyword :val ":a"}
                               {:type :keyword :val ":b"}
                               {:type :keyword :val ":c"}}}])))

  (testing "Browsable set"
    (is (= (prepped-vals {:bigger-set (->> (range 100)
                                           (map #(keyword (str "Item" %)))
                                           set)})
           [{:type :summary
             :val "#{100 keywords}"
             :actions [[:set-path "My data" [:bigger-set]]]}])))

  (testing "Browsable set with single item"
    (is (= (prepped-vals {:set #{(->> (range 100)
                                      (map #(keyword (str "Item" %))))}})
           [{:type :summary
             :val "#{1 seq}"
             :actions [[:set-path "My data" [:set]]]}]))))

(deftest prepare-vector-test
  (testing "Empty vector"
    (is (= (prepped-vals {:small-vector []})
           [{:type :vector :val []}])))

  (testing "Inlinable vector"
    (is (= (prepped-vals {:small-vector [:a :b :c]})
           [{:type :vector :val [{:val ":a" :type :keyword}
                                 {:val ":b" :type :keyword}
                                 {:val ":c" :type :keyword}]}])))

  (testing "Browsable vector"
    (is (= (prepped-vals {:bigger-vector (->> (range 100)
                                              (map #(keyword (str "Item" %)))
                                              vec)})
           [{:type :summary
             :val "[100 keywords]"
             :actions [[:set-path "My data" [:bigger-vector]]]}]))))

(deftest prepare-vector-test
  (testing "Inlinable list"
    (is (= (prepped-vals {:small-list '(:a :b :c)})
           [{:type :list :val '({:val ":a" :type :keyword}
                                {:val ":b" :type :keyword}
                                {:val ":c" :type :keyword})}])))

  (testing "Browsable list"
    (is (= (prepped-vals {:bigger-list (->> (range 100)
                                            (map #(keyword (str "Item" %)))
                                            (into '()))})
           [{:type :summary
             :val "(100 keywords)"
             :actions [[:set-path "My data" [:bigger-list]]]}]))))

(deftest prepare-seq-test
  (testing "Inlinable seq"
    (is (= (prepped-vals {:small-seq (range 3)})
           [{:type :seq :val '({:type :number :val "0"}
                               {:type :number :val "1"}
                               {:type :number :val "2"})}])))

  (testing "Browsable seq"
    (is (= (prepped-vals {:bigger-list (range 400)})
           [{:type :summary :val "(400 numbers)"}])))

  (testing "Lazy seq"
    (is (= (prepped-vals {:lazy (map #(keyword (str "Item" %)) (range 10000))})
           [{:type :summary :val "(1000+ items, click to load 0-1000)"}]))))

(def token "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c")

(deftest prepare-jwt-test
  (testing "Recognizes and links JWT"
    (is (= (prepped-vals {:token token})
           [{:type :jwt
             :val "\"eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...\""
             :actions [[:set-path "App state" [:gadget/JWT]]]}]))))

(deftest get-in*-test
  (testing "Gets keys from maps"
    (is (= (sut/get-in* {:a "Banana"} [:a]) "Banana"))
    (is (= (sut/get-in* {:a {:b "Banana"}} [:a :b]) "Banana"))
    (is (= (sut/get-in* {:a {:b {"Banana" 2}}} [:a :b "Banana"]) 2)))

  (testing "Gets vector items"
    (is (= (sut/get-in* ["Banana"] [0]) "Banana"))
    (is (= (sut/get-in* {:a ["Apple" "Banana"]} [:a 1]) "Banana"))
    (is (= (sut/get-in* [[:a :b] [:c :d]] [1 0]) :c)))

  (testing "Gets list items"
    (is (= (sut/get-in* '("Banana") [0]) "Banana"))
    (is (= (sut/get-in* {:a '("Apple" "Banana")} [:a 1]) "Banana"))
    (is (= (sut/get-in* (list (list :a :b) (list :c :d)) [1 0]) :c)))

  (testing "Gets lazy seq items"
    (is (= (sut/get-in* (range 100) [3]) 3)))

  (testing "Resolves custom JWT accessor"
    (is (= (keys (sut/get-in* {:token token} [:token :gadget/JWT])) [:header :data :signature]))))

(deftest prepare-navigated-data-test
  (testing "Serves up data at path"
    (is (= (-> {:label "Some data"
                :path [:key]
                :ref (atom {:key {:a 1, :b 2, :token token}})}
               sut/prepare-data
               (select-keys [:path :data]))
           {:path [{:text "Some data" :actions [[:set-path "Some data" []]]}
                   {:text ":key"}]
            :data {{:type :keyword :val ":a"} {:val "1" :type :number :copyable "1"}
                   {:type :keyword :val ":b"} {:val "2" :type :number :copyable "2"}
                   {:type :keyword :val ":token"}
                   {:type :jwt
                    :val "\"eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...\""
                    :actions [[:set-path "Some data" [:token :gadget/JWT]]]
                    :copyable (str "\"" token "\"")}}}))))

(deftest prepare-copyable-test
  (testing "Prepares data at path for copying"
    (is (= (-> {:label "My data"
                :path []
                :ref (atom {:key {:a 1, :b 2}})}
               sut/prepare-data
               :copyable)
           "{:key {:a 1, :b 2}}"))

    (is (= (-> {:label "My data"
                :path [:key]
                :ref (atom {:key {:a 1, :b 2}})}
               sut/prepare-data
               :copyable)
           "{:a 1, :b 2}")))

  (testing "Prepares data for copying for each item"
    (is (= (->> {:label "My data"
                 :path []
                 :ref (atom {:key {:a 1, :b 2}})}
                sut/prepare-data
                :data
                vals
                (map :copyable))
           ["{:a 1, :b 2}"]))))
