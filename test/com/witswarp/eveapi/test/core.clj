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

(ns com.witswarp.eveapi.test.core
  (:use [com.witswarp.eveapi.core] :reload)
  (:use [com.witswarp.eveapi.config] :reload)
  (:use [clojure.test])
  (:use [utilize.testutils :only (do-at)])
  (:use [conjure.core :only (stubbing)])
  (:require [com.witswarp.eveapi.core.cache :as cache]
            [clj-time.core :as time-core]))

(def test-cache-path "./testdata")

(defn database-fixture [f]
  (cache/init-cache! test-cache-path)
  (f)
  (cache/clear-cache! test-cache-path))

(use-fixtures :each database-fixture)

(deftest parse-dates-test
  (let [current (time-core/date-time 2010 1 2 12 12 10)
        data {:tag :currentTime,
              :attrs nil,
              :content ["2010-01-02 12:12:10"]}
        expected {:tag :currentTime,
                  :attrs nil,
                  :content [current]}]
    (is (= [expected] (parse-dates [] data)))))

;; TODO: This test does not clean up its data!
(deftest api-get-test
  (let [current (time-core/date-time 2010 1 2 12 12 10)
        cached (time-core/date-time 2010 1 2 12 12 25)
        test-xml "
<?xml version='1.0' encoding='UTF-8'?>
<eveapi version=\"1\">
  <currentTime>2010-01-02 12:12:10</currentTime>
  <result>
    <rowset name=\"characters\" key=\"characterID\" columns=\"name,characterID,corporationName,corporationID\">
      <row name=\"Mary\" characterID=\"150267069\"
           corporationName=\"Starbase Anchoring Corp\" corporationID=\"150279367\" />
      <row name=\"Marcus\" characterID=\"150302299\"
           corporationName=\"Marcus Corp\" corporationID=\"150333466\" />
      <row name=\"Dieinafire\" characterID=\"150340823\"
           corporationName=\"Center for Advanced Studies\" corporationID=\"1000169\" />
    </rowset>
  </result>
  <cachedUntil>2010-01-02 12:12:25</cachedUntil>
</eveapi>
"
        expected {:tag :eveapi,
                  :attrs {:version "1"},
                  :content [{:tag :currentTime,
                             :attrs nil,
                             :content [current]}
                            {:tag :result,
                             :attrs nil,
                             :content [{:tag :rowset,
                                        :attrs {:name "characters",
                                                :key "characterID",
                                                :columns "name,characterID,corporationName,corporationID"},
                                        :content [{:tag :row,
                                                   :attrs {:name "Mary",
                                                           :characterID "150267069",
                                                           :corporationName "Starbase Anchoring Corp",
                                                           :corporationID "150279367"},
                                                   :content nil}
                                                  {:tag :row,
                                                   :attrs {:name "Marcus",
                                                           :characterID "150302299",
                                                           :corporationName "Marcus Corp",
                                                           :corporationID "150333466"},
                                                   :content nil}
                                                  {:tag :row,
                                                   :attrs {:name "Dieinafire",
                                                           :characterID "150340823",
                                                           :corporationName "Center for Advanced Studies",
                                                           :corporationID "1000169"},
                                                   :content nil}]}]}
                            {:tag :cachedUntil,
                             :attrs nil,
                             :content [cached]}]}]
    (testing "no cache"
      (stubbing [raw-api-get {:body test-xml}]
        (do-at current
               (is (= nil (parse-api-result (cache/get-from-cache test-cache-path
                                                                  (cache/make-key nil nil '(account Characters))))))
               (is (= expected (api-get '(account Characters) nil nil test-cache-path)))
               (is (= expected (parse-api-result (cache/get-from-cache test-cache-path
                                                                       (cache/make-key nil nil '(account Characters)))))))))))
