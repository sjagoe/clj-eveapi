(ns com.witswarp.eveapi.test.core
  (:use [com.witswarp.eveapi.core] :reload)
  (:use [clojure.test])
  (:require [clj-time.format :as time-fmt]
            [clj-time.core :as time-core]))


(deftest parse-dates-test
  (let [current (time-core/date-time 2010 1 2 12 12 10)
        data {:tag :currentTime,
              :attrs nil,
              :content ["2010-01-02 12:12:10"]}
        expected {:tag :currentTime,
                  :attrs nil,
                  :content [current]}]
    (is (= [expected] (parse-dates [] data)))))


(deftest make-key-test
  (is (= (make-key nil nil '(account Characters)) "/account/Characters"))
  (is (= (make-key "api.eveonline.com" nil '(account Characters)) "/api.eveonline.com/account/Characters"))
  (is (= (make-key nil {:userID 1234 :characterID 5678} '(account Characters)) "/1234/5678/account/Characters"))
  (is (= (make-key "api.eveonline.com" {:userID 1234 :characterID 5679 :apiKey "AbcDef"} '(account Characters)) "/api.eveonline.com/1234/5679/account/Characters"))
  (is (= (make-key nil {:userID 1234} '(account Characters)) "/1234/account/Characters")))


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
      (binding [raw-api-get (fn [& args] {:body test-xml})]
        (is (= expected (api-get '(account Characters) nil nil "./testdata")))))))
