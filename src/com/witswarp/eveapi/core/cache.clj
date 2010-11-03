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


(defn get-from-cache [db key]
  (let [result (cb/retrieve :key key)
        data (:data result)]
    (if (not (expired? data))
      data
      (do
        (cb/with-txn []
          (cb/delete result))
        nil))))


(defn store-in-cache! [db key result]
  (cb/with-txn []
    (cb/make-instance api-item [key result])))

