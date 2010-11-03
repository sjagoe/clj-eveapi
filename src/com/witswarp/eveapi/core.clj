(ns com.witswarp.eveapi.core
  (:import (java.io ByteArrayInputStream))
  (:require [clj-http.client :as http])
  (:require [clojure.xml :as xml])
  (:require [clj-time.format :as clj-time]))


(defn path-args-to-path [path-args]
  (str "/" (apply str (interpose "/" path-args)) ".xml.aspx"))


(defn raw-api-get [query-params host path]
  "doc string"
  (let [uri (str host path)]
    (http/post uri {:query-params query-params})))


(def formatter (clj-time/formatter "yyyy-MM-dd HH:mm:ss"))


(defn parse-dates [content xml-snippet] 
  (try
    (cons (assoc xml-snippet :content [(clj-time/parse formatter (first (:content xml-snippet)))]) content)
    (catch Exception _
      (cons xml-snippet content))))


(defn parse-api-result [result-string]
  (let [xml-parsed (with-open [bytes (ByteArrayInputStream. (.. result-string trim getBytes))] (xml/parse bytes))]
    (assoc xml-parsed :content
           (reverse (reduce parse-dates [] (:content xml-parsed))))))


(defn api-get [path-args query-params host]
  (let [path (path-args-to-path path-args)
        result (raw-api-get query-params host path)]
    (parse-api-result (:body result))))
