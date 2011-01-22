(ns com.witswarp.eveapi.core.cache
  (:require [cupboard.core :as cb]
            [clj-time.core :as time-core]))

(cb/defpersist api-item
  ((:key :index :unique)
   (:data)))

(defn expired? [data]
  (let [cached-until (first (filter #(= :cachedUntil (:tag %))
                                    (:content data)))]
    (if (time-core/after? (time-core/now) (first (:content cached-until)))
      true
      false)))

(defn get-from-cache [key]
  (let [result (cb/retrieve :key key)]
    (:data result)))

(defn delete-from-cache! [key]
  (cb/with-txn []
    (cb/delete (cb/retrieve :key key))))

(defn store-in-cache! [key result]
  (cb/with-txn []
    (cb/make-instance api-item [key result])))
