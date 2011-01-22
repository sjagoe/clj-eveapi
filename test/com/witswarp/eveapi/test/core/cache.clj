(ns com.witswarp.eveapi.test.core.cache
  (:use [com.witswarp.eveapi.core.cache] :reload)
  (:use [com.witswarp.eveapi.config] :reload)
  (:use [com.witswarp.eveapi.test.core :only (test-cache-path)] :reload)
  (:use [clojure.test])
  (:require [clj-time.core :as time-core]
            [cupboard.core :as cb])
  (:import [java.io File]))

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
  (binding [*cache-path* test-cache-path]
    (testing "not expired"
      (let [cache-path (File. "./testdata")
            now (time-core/plus (time-core/now) (time-core/minutes 5))
            data {:content [{:tag :cachedUntil
                             :content [now]}]}]
        (.mkdir cache-path)
        (cb/with-open-cupboard [(.toString cache-path)]
          (store-in-cache! "key" data)
          (let [retrieved (get-from-cache "key")]
            (try
              (do
                (is (.isEqual (first (:content (first (:content retrieved)))) now))
                (is (= (:tag (first (:content retrieved))) :cachedUntil)))
              (finally (delete-from-cache! "key")))))))
    (testing "expired"
      (let [cache-path (File. "./testdata")
            now (time-core/now)
            before-now (time-core/minus now (time-core/minutes 5))
            data {:content [{:tag :cachedUntil
                             :content [before-now]}]}]
        (.mkdir cache-path)
        (cb/with-open-cupboard [(.toString cache-path)]
          (store-in-cache! "key" data)
          ;; TODO: Catch exceptions and clean up DB afterwards
          (let [retrieved (get-from-cache "key")]
            (try
              (do
                (is (.isEqual (first (:content (first (:content retrieved)))) before-now))
                (is (.isBefore (first (:content (first (:content retrieved)))) now))
                (is (= (:tag (first (:content retrieved))) :cachedUntil)))
              (finally (delete-from-cache! "key")))))))))
