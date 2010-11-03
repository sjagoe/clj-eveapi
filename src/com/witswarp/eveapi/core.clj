(ns com.witswarp.eveapi.core
  (:import (java.io ByteArrayInputStream))
  (:require [clj-http.client :as http]
            [clojure.xml :as xml]
            [clj-time.format :as time-fmt]
            [cupboard.core :as cb]
            [com.witswarp.eveapi.core.cache :as cache]))


(defn path-args-to-path [path-args]
  (str "/" (apply str (interpose "/" path-args)) ".xml.aspx"))


(defn raw-api-get [query-params host path]
  "doc string"
  (let [uri (str host path)]
    (http/post uri {:query-params query-params})))


(def formatter (time-fmt/formatter "yyyy-MM-dd HH:mm:ss"))


(defn parse-dates [content xml-snippet] 
  (try
    (cons (assoc xml-snippet :content [(time-fmt/parse formatter (first (:content xml-snippet)))]) content)
    (catch Exception _
      (cons xml-snippet content))))


(defn parse-api-result [result-string]
  (let [xml-parsed (with-open [bytes (ByteArrayInputStream. (.. result-string trim getBytes))] (xml/parse bytes))]
    (assoc xml-parsed :content
           (reverse (reduce parse-dates [] (:content xml-parsed))))))


(defn api-get
  ([path-args {:keys [userID characterID] :as query-params} host cache-path]
     (let [key (apply str (interpose \/ (conj (seq path-args) characterID userID)))]
       (cb/with-open-cupboard [db cache-path]
         (let [cache-result (cache/get-from-cache db key)]
           (if (not (nil? cache-result))
             cache-result
             (let [result (api-get path-args query-params host)]
               (cache/store-in-cache! db key result)
               result))))))
  ([path-args query-params host]
     (let [path (path-args-to-path path-args)
           result (raw-api-get query-params host path)]
       (parse-api-result (:body result)))))
