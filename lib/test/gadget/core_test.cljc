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
           [{:text "My data" :actions {:go [[:set-path "My data" []]]}}
            {:text ":some" :actions {:go [[:set-path "My data" [:some]]]}}
            {:text "0" :actions {:go [[:set-path "My data" [:some 0]]]}}
            {:text ":stuff"}])))

  (testing "Does not link virtual element when it is the current one"
    (is (= (:path (sut/prepare-data {:path [:some 0 :token :gadget/JWT]
                                     :label "My data"}))
           [{:text "My data" :actions {:go [[:set-path "My data" []]]}}
            {:text ":some" :actions {:go [[:set-path "My data" [:some]]]}}
            {:text "0" :actions {:go [[:set-path "My data" [:some 0]]]}}
            {:text ":token"}])))

  (testing "Links virtual path elements"
    (is (= (:path (sut/prepare-data {:path [:some 0 :token :gadget/JWT :data]
                                     :label "My data"}))
           [{:text "My data" :actions {:go [[:set-path "My data" []]]}}
            {:text ":some" :actions {:go [[:set-path "My data" [:some]]]}}
            {:text "0" :actions {:go [[:set-path "My data" [:some 0]]]}}
            {:text ":token" :actions {:go [[:set-path "My data" [:some 0 :token :gadget/JWT]]]}}
            {:text ":data"}]))))

(defn prepped-keys [data]
  (->> {:ref (atom data)
        :label "My data"}
       sut/prepare-data
       :data
       (map first)
       (map #(select-keys % [:val :type]))))

(defn prepped-vals [data]
  (->> {:ref (atom data)
        :label "My data"}
       sut/prepare-data
       :data
       (map second)
       (map #(select-keys % [:val :type :constructor :actions]))))

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
           [{:val ":a" :type :keyword}
            {:val ":bb" :type :keyword}
            {:val "d" :type :symbol}
            {:val "\"b\"" :type :string}
            {:val "3" :type :number}
            {:val "{:a 2}" :type :map}
            {:val "[:a]" :type :vector}
            {:val "(:b)" :type :list}
            {:val "#{:c}" :type :set}
            {:val "(0 1 2 3 4 5 6 7 8 9 ...)" :type :seq}
            {:val "(0 1 2 3 4)" :type :seq}
            {:val "true" :type :boolean}
            {:val "nil" :type :nil}])))

  (testing "Treats vectors like maps"
    (is (= (prepped-keys [1 2 3 4 5])
           [{:val "0" :type :number}
            {:val "1" :type :number}
            {:val "2" :type :number}
            {:val "3" :type :number}
            {:val "4" :type :number}])))

  (testing "Keyword values"
    (is (= (prepped-vals {:a :b})
           [{:val ":b"
             :type :keyword
             :actions {:copy [[:copy-to-clipboard "My data" [:a]]]}}])))

  (testing "String values"
    (is (= (prepped-vals {:a "b"})
           [{:val "\"b\""
             :type :string
             :actions {:copy [[:copy-to-clipboard "My data" [:a]]]}}])))

  (testing "Number values"
    (is (= (prepped-vals {:a 2})
           [{:val "2"
             :type :number
             :actions {:copy [[:copy-to-clipboard "My data" [:a]]]}}])))

  (testing "Boolean values"
    (is (= (prepped-vals {:a true})
           [{:val "true"
             :type :boolean
             :actions {:copy [[:copy-to-clipboard "My data" [:a]]]}}])))

  (testing "nil values"
    (is (= (prepped-vals {:a nil})
           [{:val "nil"
             :type :nil
             :actions {:copy [[:copy-to-clipboard "My data" [:a]]]}}])))

  (testing "Symbol values"
    (is (= (prepped-vals {:a 'Symbolic})
           [{:val "Symbolic"
             :type :symbol
             :actions {:copy [[:copy-to-clipboard "My data" [:a]]]}}])))

  #?(:cljs (testing "JavaScript object values"
             (is (= (prepped-vals {:a (js/Map.)})
                    [{:val "object[Map]"
                      :type :object
                      :constructor "Map"
                      :actions {:copy [[:copy-to-clipboard "My data" [:a]]]}}])))))

(deftest prepare-maps-test
  (testing "Inlinable map"
    (is (= (prepped-vals {:a {:small "Map"}})
           [{:type :map
             :val [[{:val ":small" :type :keyword} {:val "\"Map\"" :type :string}]]
             :actions {:copy [[:copy-to-clipboard "My data" [:a]]]}}])))

  (testing "Sorts keys in inlinable map"
    (is (= (->> (prepped-vals {:a {:small "Map" :another "Key" :tiny "Stuff"}})
                first
                :val
                (map (comp :val first)))
           [":another" ":small" ":tiny"])))

  (testing "Inlinable map with nil"
    (is (= (prepped-vals {:a {:small nil}})
           [{:type :map
             :val [[{:val ":small" :type :keyword} {:val "nil" :type :nil}]]
             :actions {:copy [[:copy-to-clipboard "My data" [:a]]]}}])))

  (testing "Browsable map"
    (is (= (prepped-vals {:a {:slightly "Bigger"
                              "map" "That is inconvenient"
                              :to "Display on a single line"
                              'numbers (range 50)}})
           [{:type :map-keys
             :val [{:val ":slightly" :type :keyword}
                   {:val ":to" :type :keyword}
                   {:val "numbers" :type :symbol}
                   {:val "\"map\"" :type :string}]
             :actions {:go [[:set-path "My data" [:a]]]
                       :copy [[:copy-to-clipboard "My data" [:a]]]}}])))

  (testing "Browsable map with too many keys"
    (is (= (prepped-vals {:a (->> 100 range
                                  (map (fn [i] [(str "key" i) i]))
                                  (into {}))})
           [{:type :summary
             :val "{100 keys}"
             :actions {:go [[:set-path "My data" [:a]]]
                       :copy [[:copy-to-clipboard "My data" [:a]]]}}])))

  (testing "Summarizes map with string key in braces"
    (is (= (prepped-vals {:invoices {"5505505" [{:dueamount 100 :kid "00000000" :due-data "2018-11-01T00:00:00Z"} {:dueamount 100 :kid "00000001" :due-data "2018-10-01T00:00:00Z"} {:dueamount 100 :kid "00000002" :due-data "2018-09-01T00:00:00Z"} {:dueamount 100 :kid "00000003" :due-data "2018-08-01T00:00:00Z"}]}})
           [{:val [{:val "\"5505505\"" :type :string}]
             :type :map-keys
             :actions {:go [[:set-path "My data" [:invoices]]]
                       :copy [[:copy-to-clipboard "My data" [:invoices]]]}}]))))

(deftest prepare-set-test
  (testing "Inlinable set"
    (is (= (prepped-vals {:small-set #{:a :b :c}})
           [{:type :set
             :actions {:copy [[:copy-to-clipboard "My data" [:small-set]]]}
             :val #{{:type :keyword :val ":a"}
                    {:type :keyword :val ":b"}
                    {:type :keyword :val ":c"}}}])))

  (testing "Sorts sets"
    (is (= (prepped-vals {:small-set #{:b :a :c}})
           [{:type :set
             :actions {:copy [[:copy-to-clipboard "My data" [:small-set]]]}
             :val #{{:type :keyword :val ":a"}
                    {:type :keyword :val ":b"}
                    {:type :keyword :val ":c"}}}])))

  (testing "Browsable set"
    (is (= (prepped-vals {:bigger-set (->> (range 100)
                                           (map #(keyword (str "Item" %)))
                                           set)})
           [{:type :summary
             :val "#{100 keywords}"
             :actions {:go [[:set-path "My data" [:bigger-set]]]
                       :copy [[:copy-to-clipboard "My data" [:bigger-set]]]}}])))

  (testing "Browsable set with single item"
    (is (= (prepped-vals {:set #{(->> (range 100)
                                      (map #(keyword (str "Item" %))))}})
           [{:type :summary
             :val "#{1 seq}"
             :actions {:go [[:set-path "My data" [:set]]]
                       :copy [[:copy-to-clipboard "My data" [:set]]]}}]))))

(deftest prepare-vector-test
  (testing "Empty vector"
    (is (= (prepped-vals {:small-vector []})
           [{:type :vector
             :actions {:copy [[:copy-to-clipboard "My data" [:small-vector]]]}
             :val []}])))

  (testing "Inlinable vector"
    (is (= (prepped-vals {:small-vector [:a :b :c]})
           [{:type :vector
             :actions {:copy [[:copy-to-clipboard "My data" [:small-vector]]]}
             :val [{:val ":a" :type :keyword}
                   {:val ":b" :type :keyword}
                   {:val ":c" :type :keyword}]}])))

  (testing "Browsable vector"
    (is (= (prepped-vals {:bigger-vector (->> (range 100)
                                              (map #(keyword (str "Item" %)))
                                              vec)})
           [{:type :summary
             :val "[100 keywords]"
             :actions {:go [[:set-path "My data" [:bigger-vector]]]
                       :copy [[:copy-to-clipboard "My data" [:bigger-vector]]]}}]))))

(deftest prepare-list-test
  (testing "Inlinable list"
    (is (= (prepped-vals {:small-list '(:a :b :c)})
           [{:type :list
             :actions {:copy [[:copy-to-clipboard "My data" [:small-list]]]}
             :val '({:val ":a" :type :keyword}
                    {:val ":b" :type :keyword}
                    {:val ":c" :type :keyword})}])))

  (testing "Browsable list"
    (is (= (prepped-vals {:bigger-list (->> (range 100)
                                            (map #(keyword (str "Item" %)))
                                            (into '()))})
           [{:type :summary
             :val "(100 keywords)"
             :actions {:go [[:set-path "My data" [:bigger-list]]]
                       :copy [[:copy-to-clipboard "My data" [:bigger-list]]]}}]))))

(deftest prepare-seq-test
  (testing "Inlinable seq"
    (is (= (prepped-vals {:small-seq (range 3)})
           [{:type :seq
             :actions {:copy [[:copy-to-clipboard "My data" [:small-seq]]]}
             :val '({:type :number :val "0"}
                    {:type :number :val "1"}
                    {:type :number :val "2"})}])))

  (testing "Browsable seq"
    (is (= (prepped-vals {:bigger-list (range 400)})
           [{:type :summary
             :actions {:copy [[:copy-to-clipboard "My data" [:bigger-list]]]
                       :go [[:set-path "My data" [:bigger-list]]]}
             :val "(400 numbers)"}])))

  (testing "Lazy seq"
    (is (= (prepped-vals {:lazy (map #(keyword (str "Item" %)) (range 10000))})
           [{:type :summary
             :val "(1000+ items, click to load 0-1000)"
             :actions {:go [[:set-path "My data" [:lazy]]]
                       :copy [[:copy-to-clipboard "My data" [:lazy]]]}}]))))

(def token "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c")

(deftest prepare-jwt-test
  (testing "Recognizes and links JWT"
    (is (= (prepped-vals {:token token})
           [{:type :jwt
             :val "\"eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...\""
             :actions {:go [[:set-path "My data" [:token :gadget/JWT]]]
                       :copy [[:copy-to-clipboard "My data" [:token]]]}}])))

  (testing "Tries to avoid marking non-JWTs as JWTs"
    (is (= (prepped-vals {:version "1.0.2"})
           [{:type :string
             :actions {:copy [[:copy-to-clipboard "My data" [:version]]]}
             :val "\"1.0.2\""}]))

    (is (= (prepped-vals {:version "no.linkapp.com"})
           [{:type :string
             :actions {:copy [[:copy-to-clipboard "My data" [:version]]]}
             :val "\"no.linkapp.com\""}]))))

(deftest prepare-navigated-data-test
  (testing "Serves up data at path"
    (is (= (-> {:label "Some data"
                :path [:key]
                :ref (atom {:key {:a 1, :b 2, :token token}})}
               sut/prepare-data
               (select-keys [:path :data]))
           {:path [{:text "Some data" :actions {:go [[:set-path "Some data" []]]}}
                   {:text ":key"}]
            :data [[{:type :keyword :val ":a"}
                    {:val "1"
                     :type :number
                     :actions {:copy [[:copy-to-clipboard "Some data" [:key :a]]]}}]
                   [{:type :keyword :val ":b"}
                    {:val "2"
                     :type :number
                     :actions {:copy [[:copy-to-clipboard "Some data" [:key :b]]]}}]
                   [{:type :keyword :val ":token"}
                    {:type :jwt
                     :val "\"eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...\""
                     :actions {:go [[:set-path "Some data" [:key :token :gadget/JWT]]]
                               :copy [[:copy-to-clipboard "Some data" [:key :token]]]}}]]}))))

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
  (reset! calls [])
  (sut/pause!)
  (sut/resume!)
  (is (= 1 (count @calls))))

(deftest does-not-call-render-when-atom-data-is-not-inspectable
  (let [data (atom {:id 12})]
    (reset! calls [])
    (sut/inspect "My data" data {:inspectable? (fn [state] (nil? (:id state)))})
    (swap! data assoc :name "Gadget")
    (is (= 0 (count @calls)))
    (swap! data dissoc :id)
    (is (= 1 (count @calls)))))

(deftest does-not-call-render-when-data-is-not-inspectable
  (reset! calls [])
  (sut/inspect "My data" {:id 12} {:inspectable? (fn [state] (nil? (:id state)))})
  (is (= 0 (count @calls)))
  (sut/inspect "My data" {:name "Gadget"} {:inspectable? (fn [state] (nil? (:id state)))})
  (is (= 1 (count @calls))))
