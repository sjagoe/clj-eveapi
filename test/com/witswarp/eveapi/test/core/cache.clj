(ns com.witswarp.eveapi.test.core.cache
  (:use [com.witswarp.eveapi.core.cache] :reload)
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
  (testing "not expired"
    (let [cache-path (File. "./testdata")
          now (time-core/plus (time-core/now) (time-core/minutes 5))
          data {:content [{:tag :cachedUntil
                           :content [now]}]}]
      (.mkdir cache-path)
      (cb/with-open-cupboard [(.toString cache-path)]
        (store-in-cache! "key" data)
        (is (= data (get-from-cache "key")))
        (cb/retrieve :key "key" :callback cb/delete))))
  (testing "expired"
    (let [cache-path (File. "./testdata")
          now (time-core/minus (time-core/now) (time-core/minutes 5))
          data {:content [{:tag :cachedUntil
                           :content [now]}]}]
      (.mkdir cache-path)
      (cb/with-open-cupboard [(.toString cache-path)]
        (store-in-cache! "key" data)
        (is (= nil (get-from-cache "key")))
        (cb/retrieve :key "key" :callback cb/delete)))))
