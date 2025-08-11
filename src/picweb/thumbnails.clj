(ns picweb.thumbnails
    (:require [next.jdbc :as jdbc]
              [next.jdbc.result-set :as rs]
              [next.jdbc.sql :as sql]
              [clojure.string :as str]
              [clojure.pprint :refer [pprint]]
              [hiccup.core :as hiccup]
              [hiccup.page :as page]
              [hiccup.element :as element]))

(def db {:dbtype "sqlite" :dbname "pictures.sqlite3"})
(def ds (jdbc/get-datasource db))
(def ds-opts (jdbc/with-options ds {:builder-fn rs/as-unqualified-lower-maps}))

;;-----------------------------------------------------------------------------

(defn insert
    [table data]
    (try
        (let [res (sql/insert! ds-opts table data)]
            (if (empty? res)
                nil
                (first (vals res))))
        (catch Exception e
            (println "Error inserting into table:" table "with data:" data)
            (println "Exception:" (.getMessage e))
            nil)))

(defn get-all-tags
    []
    (let [sql "SELECT * FROM tags ORDER BY name"
          res (jdbc/execute! ds-opts [sql])]
        ;(println "get-all-tags ->" res)
        res))

(defn get-pic-tags
    [id]
    (let [sql "SELECT * FROM tags t WHERE t.tag_id IN (SELECT m.tag_id FROM tag_m2m m WHERE m.id = ?)"
          res (jdbc/execute! ds-opts [sql id])]
        ;(println "get-pic-tags: " id "->" res)
        res))

(defn get-tag
    [tag-id]
    (let [sql "SELECT * FROM tags WHERE tag_id = ?"
          res (jdbc/execute-one! ds-opts [sql tag-id])]
        (if (empty? res)
            nil
            (first res))))

(defn find-tag-id
    [tag-name]
    (let [sql "SELECT tag_id FROM tags WHERE name = ?"
          res (jdbc/execute-one! ds-opts [sql tag-name])]
        (if (empty? res)
            nil
            (:tag_id res))))

(defn save-tag
    [pic-id tag-name]
    (let [new-tag-id (insert :tags {:name tag-name})]
        (if new-tag-id
            (let [res (insert :tag_m2m {:id pic-id, :tag_id new-tag-id})]
                (if (some? res)
                    new-tag-id
                    (do
                        (println "Failed to save tag association"
                             "for pic-id:" pic-id "and tag-name:" tag-name)
                        nil)))
            nil)))

(defn remove-tag
    [pic-id tag-id]
    (let [sql-m2m "DELETE FROM tag_m2m WHERE id = ? AND tag_id = ?"
          res-m2m (jdbc/execute-one! ds-opts [sql-m2m pic-id tag-id])]
        (if (= 1 (count res-m2m))
            {:status :success}
            {:status :error :message "Failed to remove tag association"})))

(defn update-rating
    [pic-id rating]
    (sql/update! ds-opts :thumbnails {:rating rating} {:id pic-id}))

;;-----------------------------------------------------------------------------

(defn get-thumbs
    [offset num-per-page]
    (let [sql (str "SELECT * FROM thumbnails ORDER BY timestr ASC LIMIT ? OFFSET ?")
          res (jdbc/execute! ds-opts [sql num-per-page offset])]
        res))

(defn get-thumb
    [id]
    (let [sql (str "SELECT * FROM thumbnails WHERE ID = ?")
          res (jdbc/execute-one! ds-opts [sql id])]
        res))

(defn get-num-thumbs
    []
    (let [sql (str "SELECT count(*) FROM thumbnails")
          res (jdbc/execute-one! ds-opts [sql])]
        (first (vals res))))

;;-----------------------------------------------------------------------------

