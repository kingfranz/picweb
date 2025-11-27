(ns picweb.utils
    (:require [next.jdbc :as jdbc]
              [next.jdbc.result-set :as rs]
              [next.jdbc.sql :as sql]))

(def db {:dbtype "sqlite" :dbname "pictures.sqlite3"})
(def ds (jdbc/get-datasource db))
(def ds-opts (jdbc/with-options ds {:builder-fn rs/as-unqualified-lower-maps}))

(def min-start 19700101)
(def max-start 20251231)
(def default-page-size 50)

(defn int2thumb
    [num]
    (let [s (format "%08d" num)
          timestr (str (subs s 0 4) "-" (subs s 4 6) "-" (subs s 6 8) "T00:00:00")]
        timestr))

(defn thumb2int
    [thumb]
    (Integer/parseInt (str (subs (:timestr thumb) 0 4)
                           (subs (:timestr thumb) 5 7)
                           (subs (:timestr thumb) 8 10))))

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

(defn get-older-thumb
    [start page-sz]
    {:pre [(int? start) (int? page-sz)]
     :post [(or (nil? %) (int? %))]}
    (let [res (sql/query ds-opts ["SELECT * FROM thumbnails WHERE timestr < ? ORDER BY timestr DESC LIMIT 1 OFFSET ?" (int2thumb start) page-sz])]
        (if (empty? res)
            nil
            (thumb2int (last res)))))

(defn get-newer-thumb
    [start page-sz]
    {:pre [(int? start) (int? page-sz)]
     :post [(or (nil? %) (int? %))]}
    (let [res (sql/query ds-opts ["SELECT * FROM thumbnails WHERE timestr > ? ORDER BY timestr ASC LIMIT 1 OFFSET ?" (int2thumb start) page-sz])]
        (if (empty? res)
            nil
            (thumb2int (last res)))))

;(select-1 :all :thumbnails (> :timestr start :asc) (offset page-sz))
