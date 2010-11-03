(ns com.witswarp.eveapi.test.core.cache
  (:use [com.witswarp.eveapi.core.cache] :reload)
  (:use [clojure.test])
  (:require [clj-time.core :as time-core]))


(deftest expired?-test
  (let [now (time-core/minus (time-core/now) (time-core/minutes 5))
        data {:content [{:tag :cachedUntil
                         :content [now]}]}]
    (is (= true (#'expired? data))))
  (let [now (time-core/plus (time-core/now) (time-core/minutes 5))
        data {:content [{:tag :cachedUntil
                         :content [now]}]}]
    (is (= false (#'expired? data)))))
