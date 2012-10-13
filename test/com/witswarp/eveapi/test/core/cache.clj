;; EVE Manager - Character and corporation management for EVE Online
;; Copyright (C) 2011 Simon Jagoe

;; This program is free software: you can redistribute it and/or
;; modify it under the terms of version 3 of the GNU General Public
;; License as published by the Free Software Foundation.

;; This program is distributed in the hope that it will be useful,
;; but WITHOUT ANY WARRANTY; without even the implied warranty of
;; MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
;; GNU General Public License for more details.

;; You should have received a copy of the GNU General Public License
;; along with this program.  If not, see <http://www.gnu.org/licenses/>.

(ns com.witswarp.eveapi.test.core.cache
  (:use [com.witswarp.eveapi.core.cache] :reload)
  (:use [com.witswarp.eveapi.test.core :only (test-cache-path database-fixture)] :reload)
  (:use [clojure.test])
  (:require [clj-time.core :as time-core]
            [cupboard.core :as cb]))

(use-fixtures :each database-fixture)

(deftest make-key-test
  (is (= (make-key nil nil '(account Characters)) "/account/Characters"))
  (is (= (make-key "api.eveonline.com" nil '(account Characters)) "/api.eveonline.com/account/Characters"))
  (is (= (make-key nil {:keyID 1234 :characterID 5678} '(account Characters)) "/1234/5678/account/Characters"))
  (is (= (make-key "api.eveonline.com" {:keyID 1234 :characterID 5679 :vCode "AbcDef"} '(account Characters)) "/api.eveonline.com/1234/5679/account/Characters"))
  (is (= (make-key nil {:keyID 1234} '(account Characters)) "/1234/account/Characters")))

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
