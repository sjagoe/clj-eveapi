(ns clj-eveapi.core
  (:import (java.io ByteArrayInputStream))
  (:require [clj-http.client :as http])
  (:require [clojure.xml :as xml]))


(defn path-args-to-path [& path-args]
  (str "/" (apply str (interpose "/" path-args)) ".xml.aspx"))


(defn raw-api-get [query-params host path]
  "doc string"
  (let [uri (str host path)]
    (http/post uri {:query-params query-params})))


(defn api-get [query-params host & path-args]
  (let [path (apply path-args-to-path path-args)
        result (raw-api-get query-params host path)]
    (with-open [bytes (ByteArrayInputStream. (.getBytes (.trim (:body result))))] (xml/parse bytes))))
