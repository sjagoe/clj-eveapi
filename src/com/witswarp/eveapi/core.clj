;; eveapi - Clojure interface to the EVE API
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

(ns com.witswarp.eveapi.core
  (:import (java.io ByteArrayInputStream))
  (:require [clj-http.client :as http]
            [clojure.xml :as xml]
            [clj-time.format :as time-fmt]
            [com.witswarp.eveapi.core.cache :as cache]))

(defn path-args-to-path [path-args]
  "Converts api-get EVE-API arguments to an EVE-API path"
  (str "/" (apply str (interpose "/" path-args)) ".xml.aspx"))

(defn raw-api-get [query-params host path]
  "Gets and returns the raw XML response from the EVE-API"
  (let [uri (str host path)]
    (http/post uri {:query-params query-params})))

(def formatter (time-fmt/formatter "yyyy-MM-dd HH:mm:ss"))

(defn parse-dates [content xml-snippet]
  "Parses string dates to clj-time dates"
  (try
    (cons (assoc xml-snippet :content [(time-fmt/parse formatter (first (:content xml-snippet)))]) content)
    (catch Exception _
      (cons xml-snippet content))))

(defn parse-api-result [result-string]
  "Parses the XML response from the API into Clojure maps"
  (if (not (nil? result-string))
    (let [xml-parsed (with-open [bytes (ByteArrayInputStream. (.. result-string trim getBytes))] (xml/parse bytes))]
      (assoc xml-parsed :content
             (reverse (reduce parse-dates [] (:content xml-parsed)))))))

(defn api-get [path-args query-params host cache-path]
  "Fetches and parses the requested API item. If the requested item is
in the cache (and the cached item is valid), then it will be returned
from the cache."
  (let [key (cache/make-key host query-params path-args)]
    (let [raw-cache-result (cache/get-from-cache cache-path key)
          cache-result (parse-api-result raw-cache-result)]
      (if (cache/expired? cache-result) 
        (let [path (path-args-to-path path-args)
              raw-result (:body (raw-api-get query-params host path))]
          (cache/store-in-cache! cache-path key raw-result)
          (parse-api-result raw-result))
        cache-result))))
