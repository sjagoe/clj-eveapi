(ns com.witswarp.eveapi.test.core.cache
  (:use [com.witswarp.eveapi.core.cache] :reload)
  (:use [com.witswarp.eveapi.config] :reload)
  (:use [com.witswarp.eveapi.test.core :only (test-cache-path database-fixture)] :reload)
  (:use [clojure.test])
  (:require [clj-time.core :as time-core]
            [cupboard.core :as cb]))

(use-fixtures :each database-fixture)

(deftest expired?-test
  (testing "not expired"
    (let [now (time-core/plus (time-core/now) (time-core/minutes 5))
          data {:content [{:tag :cachedUntil
                           :content [now]}]}]
      (is (= false (#'expired? data)))))
  (testing "expired"
    (let [now (time-core/minus (time-core/now) (time-core/minutes 5))
          data {:content [{:tag :cachedUntil
                           :content [now]}]}]
      (is (= true (#'expired? data))))))

(deftest cache-test
  (testing "not expired"
    (let [now (time-core/plus (time-core/now) (time-core/minutes 5))
          data {:content [{:tag :cachedUntil
                           :content [now]}]}]
      (store-in-cache! test-cache-path "key" data)
      (let [retrieved (get-from-cache test-cache-path "key")]
        (try
          (do
            (is (.isEqual (first (:content (first (:content retrieved)))) now))
            (is (= (:tag (first (:content retrieved))) :cachedUntil)))
          (finally (delete-from-cache! test-cache-path "key"))))))
  (testing "expired"
    (let [now (time-core/now)
          before-now (time-core/minus now (time-core/minutes 5))
          data {:content [{:tag :cachedUntil
                           :content [before-now]}]}]
      (store-in-cache! test-cache-path "key" data)
      ;; TODO: Catch exceptions and clean up DB afterwards
      (let [retrieved (get-from-cache test-cache-path "key")]
        (try
          (do
            (is (.isEqual (first (:content (first (:content retrieved)))) before-now))
            (is (.isBefore (first (:content (first (:content retrieved)))) now))
            (is (= (:tag (first (:content retrieved))) :cachedUntil)))
          (finally (delete-from-cache! test-cache-path "key")))))))
