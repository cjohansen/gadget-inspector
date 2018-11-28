(ns gadget.diff-test
  (:require [gadget.diff :as sut]
            #?(:clj [clojure.test :refer [deftest testing is]]
               :cljs [cljs.test :refer [deftest testing is]])))

(deftest diff-test
  (testing "Reports added keys"
    (is (= (sut/diff {} {:a 1 :b 2})
           {:added #{:a :b}
            :removed #{}
            :changed {}})))

  (testing "Reports removed keys"
    (is (= (sut/diff {:a 1 :b 2} {})
           {:removed #{:a :b}
            :added #{}
            :changed {}})))

  (testing "Reports changed keys"
    (is (= (sut/diff {:a 1 :b 2} {:a 2 :b 3})
           {:removed #{}
            :added #{}
            :changed {:a {:old 1 :new 2}
                      :b {:old 2 :new 3}}})))

  (testing "Recursively diffs changed keys"
    (is (= (sut/diff {:a {:c 1}} {:a {:c 2}})
           {:removed #{}
            :added #{}
            :changed {:a {:added #{}
                          :removed #{}
                          :changed {:c {:old 1
                                        :new 2}}}}}))

    (is (= (sut/diff {:a {:c 1}} {:a {:d 2}})
           {:removed #{}
            :added #{}
            :changed {:a {:added #{:d}
                          :removed #{:c}
                          :changed {}}}})))

  (testing "Diffs vectors"
    (is (= (sut/diff [:a :b :c] [:a :b])
           {:old [:a :b :c]
            :new [:a :b]}))))
