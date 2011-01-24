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
