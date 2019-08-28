(ns gadget.extensions-test
  (:require [gadget.extensions :as sut]
            [gadget.core :as gadget]
            [gadget.helper :refer [prepped-keys prepped-vals]]
            #?(:clj [clojure.test :refer [deftest testing is]]
               :cljs [cljs.test :refer [deftest testing is]])))

(def token "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c")

(deftest prepare-jwt-test
  (testing "Recognizes and links JWT"
    (is (= (prepped-vals {:token token})
           [[:span {}
             [:strong {} "JWT: "]
             [:gadget/string "\"eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...\""]]])))

  (testing "Tries to avoid marking non-JWTs as JWTs"
    (is (= (prepped-vals {:version "1.0.2"})
           [[:gadget/string "\"1.0.2\""]]))

    (is (= (prepped-vals {:version "no.linkapp.com"})
           [[:gadget/string "\"no.linkapp.com\""]]))))

(deftest nav-in-test
  (testing "Resolves custom JWT accessor"
    (is (= (keys (gadget/nav-in {:token token} [:token :data])) [:sub :name :iat])))

  (testing "Resolves custom date accessor"
    (is (= (gadget/nav-in {:date #?(:cljs (js/Date. 2018 0 1 12 0 0))} [:date :year]) 2018))))
