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

(deftest prepare-data-test
  (testing "Encodes keys as strings with type hints"
    (is (= (->> {:ref (atom {:a 1
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
                             (range 100) 12})}
                sut/prepare-data
                :data
                keys
                set)
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
    (is (= (vals (:data (sut/prepare-data {:ref (atom {:a :b})})))
           [{:val ":b" :type :keyword}])))

  (testing "String values"
    (is (= (vals (:data (sut/prepare-data {:ref (atom {:a "b"})})))
           [{:val "\"b\"" :type :string}])))

  (testing "Number values"
    (is (= (vals (:data (sut/prepare-data {:ref (atom {:a 2})})))
           [{:val "2" :type :number}])))

  (testing "Boolean values"
    (is (= (vals (:data (sut/prepare-data {:ref (atom {:a true})})))
           [{:val "true" :type :boolean}])))

  (testing "nil values"
    (is (= (vals (:data (sut/prepare-data {:ref (atom {:a nil})})))
           [{:val "nil" :type :nil}])))

  (testing "Symbol values"
    (is (= (vals (:data (sut/prepare-data {:ref (atom {:a 'Symbolic})})))
           [{:val "Symbolic" :type :symbol}])))

  #?(:cljs (testing "JavaScript object values"
             (is (= (vals (:data (sut/prepare-data {:ref (atom {:a (js/Map.)})})))
                    [{:val "object[Map]" :type :object :constructor "Map"}])))))

(deftest prepare-maps-test
  (testing "Inlinable map"
    (is (= (vals (:data (sut/prepare-data {:ref (atom {:a {:small "Map"}})})))
           [{:type :map :val {{:val ":small" :type :keyword} {:val "\"Map\"" :type :string}}}])))

  (testing "Inlinable map with nil"
    (is (= (vals (:data (sut/prepare-data {:ref (atom {:a {:small nil}})})))
           [{:type :map :val {{:val ":small" :type :keyword} {:val "nil" :type :nil}}}])))

  (testing "Browsable map"
    (is (= (vals (:data (sut/prepare-data {:ref (atom {:a {:slightly "Bigger"
                                                           :map "That is inconvenient"
                                                           :to "Display on a single line"
                                                           :numbers (range 50)}})})))
           [{:type :map-keys
             :val [:map :numbers :slightly :to]
             :actions [[:set-path "???" []]]}])))

  (testing "Browsable map with too many keys"
    (is (= (-> {:ref (atom {:a (->> 100
                                    range
                                    (map (fn [i] [(str "key" i) i]))
                                    (into {}))})}
               sut/prepare-data
               :data
               vals)
           [{:type :summary
             :val "{100 keys}"
             :actions [[:set-path "???" []]]}]))))

(deftest prepare-set-test
  (testing "Inlinable set"
    (is (= (vals (:data (sut/prepare-data {:ref (atom {:small-set #{:a :b :c}})})))
           [{:type :set :val #{:a :b :c}}])))

  (testing "Browsable set"
    (is (= (-> {:ref (atom {:bigger-set (->> (range 100)
                                             (map #(keyword (str "Item" %)))
                                             set)})}
               sut/prepare-data
               :data
               vals)
           [{:type :summary
             :val "#{100 keywords}"
             :actions [[:set-path "???" []]]}])))

  (testing "Browsable set with single item"
    (is (= (-> {:ref (atom {:set #{(->> (range 100)
                                        (map #(keyword (str "Item" %))))}})}
               sut/prepare-data
               :data
               vals)
           [{:type :summary
             :val "#{1 seq}"
             :actions [[:set-path "???" []]]}]))))

(deftest prepare-vector-test
  (testing "Empty vector"
    (is (= (vals (:data (sut/prepare-data {:ref (atom {:small-vector []})})))
           [{:type :vector :val []}])))

  (testing "Inlinable vector"
    (is (= (vals (:data (sut/prepare-data {:ref (atom {:small-vector [:a :b :c]})})))
           [{:type :vector :val [{:val ":a" :type :keyword}
                                 {:val ":b" :type :keyword}
                                 {:val ":c" :type :keyword}]}])))

  (testing "Browsable vector"
    (is (= (-> {:ref (atom {:bigger-vector (->> (range 100)
                                                (map #(keyword (str "Item" %)))
                                                vec)})}
               sut/prepare-data
               :data
               vals)
           [{:type :summary
             :val "[100 keywords]"
             :actions [[:set-path "???" []]]}]))))

(deftest prepare-vector-test
  (testing "Inlinable list"
    (is (= (vals (:data (sut/prepare-data {:ref (atom {:small-list '(:a :b :c)})})))
           [{:type :list :val '(:a :b :c)}])))

  (testing "Browsable list"
    (is (= (-> {:ref (atom {:bigger-list (->> (range 100)
                                              (map #(keyword (str "Item" %)))
                                              (into '()))})}
               sut/prepare-data
               :data
               vals)
           [{:type :summary
             :val "(100 keywords)"
             :actions [[:set-path "???" []]]}]))))

(deftest prepare-seq-test
  (testing "Inlinable seq"
    (is (= (vals (:data (sut/prepare-data {:ref (atom {:small-seq (range 3)})})))
           [{:type :seq :val '(0 1 2)}])))

  (testing "Browsable seq"
    (is (= (-> {:ref (atom {:bigger-list (range 400)})}
               sut/prepare-data
               :data
               vals)
           [{:type :summary :val "(400 numbers)"}])))

  (testing "Lazy seq"
    (is (= (-> {:ref (atom {:lazy (map #(keyword (str "Item" %)) (range 10000))})}
               sut/prepare-data
               :data
               vals)
           [{:type :summary :val "(1000+ items, click to load 0-1000)"}]))))

(def token "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c")

(deftest prepare-jwt-test
  (testing "Recognizes and links JWT"
    (is (= (-> {:ref (atom {:token token}) :label "App state"}
               sut/prepare-data
               :data
               vals)
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
    (is (= (sut/prepare-data {:label "My data"
                              :path [:key]
                              :ref (atom {:key {:a 1, :b 2}})})
           {:path [{:text "My data" :actions [[:set-path "My data" []]]}
                   {:text ":key"}]
            :data {{:type :keyword :val ":a"} {:val "1" :type :number}
                   {:type :keyword :val ":b"} {:val "2" :type :number}}}))))
