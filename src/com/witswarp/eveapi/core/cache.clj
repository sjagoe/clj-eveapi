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

(ns com.witswarp.eveapi.core.cache
  (:use [com.witswarp.eveapi.config])
  (:require [cupboard.core :as cb]
            [clj-time.core :as time-core]
            [clojure.contrib.java-utils :as java-utils]))

(cb/defpersist api-item
  ((:key :index :unique)
   (:data)))

(defn expired? [data]
  (if (nil? data)
    true
    (let [cached-until (first (filter #(= :cachedUntil (:tag %))
                                      (:content data)))]
      (if (time-core/after? (time-core/now) (first (:content cached-until)))
        true
        false))))

(defn get-from-cache [cache-path key]
  (cb/with-open-cupboard [cache-path]
    (let [result (cb/retrieve :key key)]
      (if (not (nil? result))
        (:data result)))))

(defn delete-from-cache! [cache-path key]
  (cb/with-open-cupboard [cache-path]
    (cb/with-txn []
      (cb/delete (cb/retrieve :key key)))))

(defn store-in-cache! [cache-path key result]
  (cb/with-open-cupboard [cache-path]
    (cb/with-txn []
      (let [old-result (cb/retrieve :key key)]
        (if (not (nil? old-result))
          (cb/delete old-result))
        (cb/make-instance api-item [key result])))))

(defn init-cache! [cache-path]
  (let [dummy "__DUMMY__"]
    (.mkdir (java-utils/file cache-path))
    (cb/with-open-cupboard [cache-path]
      (cb/with-txn []
        (try
          (cb/retrieve :key dummy)
          (catch java.lang.RuntimeException e
            (cb/make-instance api-item [dummy ""])
            (cb/delete (cb/retrieve :key dummy))))))))

(defn clear-cache! [cache-path]
  (java-utils/delete-file-recursively cache-path))
