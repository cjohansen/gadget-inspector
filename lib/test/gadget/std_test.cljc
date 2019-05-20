(ns gadget.std-test
  (:require [gadget.std :as sut]
            #?(:clj [clojure.test :refer [deftest testing is]]
               :cljs [cljs.test :refer [deftest testing is]])))

(def token "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c")

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

  (testing "Returns non-collections untouched"
    (is (= (sut/get-in* :oh/noes [3]) :oh/noes)))

  (testing "Resolves custom JWT accessor"
    (is (= (keys (sut/get-in* {:token token} [:token :gadget/JWT])) [:header :data :signature]))))
