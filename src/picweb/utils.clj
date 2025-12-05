(ns picweb.utils
    (:require [clojure.spec.alpha :as s]
              [next.jdbc :as jdbc]
              [next.jdbc.result-set :as rs]
              [next.jdbc.sql :as sql]))

(def db {:dbtype "sqlite" :dbname "pictures.sqlite3"})
(def ds (jdbc/get-datasource db))
(def ds-opts (jdbc/with-options ds {:builder-fn rs/as-unqualified-lower-maps}))

(def min-start "1970-01-01T00:00:00+00:00")
(def max-start "2025-12-31T23:59:59+00:00")
(def default-page-size 50)
(def min-grid-size 10)
(def max-grid-size 201)

(defn ss-valid?
    [sp v]
    ;(println "++++++++++++++++ Validating spec" sp "with value:" v)
    (if (not (s/valid? sp v))
        (do
            (println (s/explain-str sp v))
            (flush)
            (println "Spec validation failed for" sp "with value:" v)
            false)
        true))

(defn fix-time
    [v]
    (let [dt (if (= (type v) String)
                 v
                 (format "%08d" v))]
        (cond
            (re-matches #"^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}((\+| )\d{2}:\d{2})?$" dt) (subs dt 0 19)
            (re-matches #"^\d{4}-\d{2}-\d{2}$" dt) (str dt "T00:00:00")
            (re-matches #"^\d{8}$" dt) (str (subs dt 0 4) "-" (subs dt 4 6) "-" (subs dt 6 8) "T00:00:00")
            :else (do
                      (println "Unrecognized time format:" dt)
                      dt))))

(defn insert
    [table data]
    {:pre [(keyword table) (map? data)]
     :post [(or (nil? %) (integer? %))]}
    (try
        (let [res (sql/insert! ds-opts table data)]
            (if (empty? res)
                nil
                (first (vals res))))
        (catch Exception e
            (println "Error inserting into table:" table "with data:" data)
            (println "Exception:" (.getMessage e))
            nil)))


(defn encode-data [data]
    (.encodeToString (java.util.Base64/getEncoder) (.getBytes data)))

(defn decode-data [encoded-data]
    (String. (.decode (java.util.Base64/getDecoder) encoded-data)))

;(select-1 :all :thumbnails (> :timestr start :asc) (offset page-sz))
