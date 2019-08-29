(ns gadget.datafy-test
  (:require [gadget.datafy :as sut]
            [gadget.helper :refer [prepped-keys prepped-vals]]
            #?(:clj [clojure.test :refer [deftest testing is]]
               :cljs [cljs.test :refer [deftest testing is]])))

(deftest synthetic-type-test
  (is (= (sut/synthetic-type "String") :string))
  (is (= (sut/synthetic-type 42) :number))
  (is (= (sut/synthetic-type :kw) :keyword))
  (is (= (sut/synthetic-type true) :boolean))
  (is (= (sut/synthetic-type {}) :map))
  (is (= (sut/synthetic-type []) :vector))
  (is (= (sut/synthetic-type '()) :list))
  (is (= (sut/synthetic-type nil) :nil))
  (is (= (sut/synthetic-type #{}) :set))
  (is (= (sut/synthetic-type 'aloha) :symbol))
  (is (= (sut/synthetic-type (map identity [])) :seq))
  #?(:cljs (is (= (sut/synthetic-type (js/Date.)) :date)))
  #?(:cljs (is (= (sut/synthetic-type (js/Object.)) :object))))

(sut/add-type-inference
 (fn [v]
   (when (and (string? v) (re-find #"^LOL" v))
     :lol)))

(deftest custom-type-test
  (is (= (sut/synthetic-type "string") :string))
  (is (= (sut/synthetic-type "LOL: String") :lol)))

(sut/add-type-inference
 (fn [v]
   (when (and (string? v) (re-find #"^LOL!" v))
     :lulz)))

(deftest overlapping-type-inference-prefers-last-added
  (is (= (sut/synthetic-type "LOL: String") :lol))
  (is (= (sut/synthetic-type "LOL! String") :lulz)))
