(ns picweb.store
    (:require [ring.middleware.session.store :as store]))

(defonce key-store (atom {}))

(defn- read-store-data
    [key]
    (get @key-store key))

(defn- save-store-data
    [key data]
    (swap! key-store assoc key data))

(defn- delete-store-data
    [key]
    (swap! key-store dissoc key))

(deftype ShopStore []
    store/SessionStore
    (store/read-session [_ key]
        (read-store-data key))
    (store/write-session [_ key data]
        (let [key (or key (rand-int Integer/MAX_VALUE))]
            (save-store-data key data)
            key))
    (store/delete-session [_ key]
        (delete-store-data key)
        nil))
