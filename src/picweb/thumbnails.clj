(ns picweb.thumbnails
    (:require [clojure.string :as str]
              [next.jdbc :as jdbc]
              [next.jdbc.result-set :as rs]
              [next.jdbc.sql :as sql]))

(def db {:dbtype "sqlite" :dbname "pictures.sqlite3"})
(def ds (jdbc/get-datasource db))
(def ds-opts (jdbc/with-options ds {:builder-fn rs/as-unqualified-lower-maps}))

;;-----------------------------------------------------------------------------

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

(defn update-thumb
    [id data]
    {:pre [(int? id) (map? data)]
     :post [(or (nil? %) (map? %))]}
    (try
        (let [res (sql/update! ds-opts :thumbnails data {:id id})]
            (if (empty? res)
                nil
                (first (vals res))))
        (catch Exception e
            (println "Error updating thumbnail with ID:" id "and data:" data)
            (println "Exception:" (.getMessage e))
            nil)))


(defn get-all-tags
    []
    {:post [(sequential? %)]}
    (let [res (sql/query ds-opts ["SELECT * FROM tags ORDER BY name"])]
        res))

(defn get-pic-tags
    [id]
    {:pre [(int? id)]
     :post [(sequential? %)]}
    (let [res (sql/query ds-opts ["SELECT * FROM tags t WHERE t.tag_id IN (SELECT m.tag_id FROM tag_m2m m WHERE m.id = ?)" id])]
        res))

(defn get-prev-thumb
    [id]
    {:pre [(int? id)]
     :post [(or (nil? %) (map? %))]}
    (let [prev (sql/query ds-opts ["SELECT * FROM thumbnails WHERE id < ? ORDER BY id DESC LIMIT 1" id])]
        (if (empty? prev)
            nil
            prev)))

(defn get-next-thumb
    [id]
    {:pre [(int? id)]
     :post [(or (nil? %) (map? %))]}
    (let [next (sql/query ds-opts ["SELECT * FROM thumbnails WHERE id > ? ORDER BY id ASC LIMIT 1" id])]
        (if (empty? next)
            nil
            next)))

(defn get-tag
    [tag-id]
    {:pre [(int? tag-id)]
     :post [(or (nil? %) (map? %))]}
    (let [res (sql/query ds-opts ["SELECT * FROM tags WHERE tag_id = ?" tag-id])]
        (if (empty? res)
            nil
            (first res))))

(defn find-tag-id
    [tag-name]
    {:pre [(string? tag-name)]
     :post [(or (nil? %) (int? %))]}
    (let [res (sql/query ds-opts ["SELECT tag_id FROM tags WHERE name = ?" tag-name])]
        (if (empty? res)
            nil
            (:tag_id (first res)))))

(defn assoc-tag
    [pic-id tag-id]
    {:pre [(int? pic-id) (int? tag-id)]
     :post [(or (nil? %) (int? %))]}
    (let [res (insert :tag_m2m {:id pic-id, :tag_id tag-id})]
        res))

(defn save-tag
    [pic-id tag-name]
    {:pre [(int? pic-id) (string? tag-name)]
     :post [(or (nil? %) (int? %))]}
    (if-let [tag-id (find-tag-id (str/lower-case tag-name))]
        (when (assoc-tag pic-id tag-id)
            tag-id)
        (let [new-tag-id (insert :tags {:name (str/lower-case tag-name)})]
            (if new-tag-id
                (let [res (insert :tag_m2m {:id pic-id, :tag_id new-tag-id})]
                    (if res
                        new-tag-id
                        (do
                            (println "Failed to save tag association"
                                 "for pic-id:" pic-id "and tag-name:" tag-name)
                            nil)))
                nil))))

(defn disassoc-tag
    [pic-id tag-id]
    {:pre [(int? pic-id) (int? tag-id)]
     :post [(boolean? %)]}
    (let [res (sql/delete! ds-opts :tag_m2m {:id pic-id :tag_id tag-id})]
        (and (some? res) (= (:update_count (first res)) 1))))

(defn update-rating
    [pic-id rating]
    {:pre [(int? pic-id) (integer? rating)]
     :post [(boolean? %)]}
    (let [res (sql/update! ds-opts :thumbnails {:rating rating} {:id pic-id})]
        (and (some? res) (= (:update_count (first res)) 1))))

;;-----------------------------------------------------------------------------

(defn get-thumbs
    [offset num-per-page]
    {:pre [(integer? offset) (integer? num-per-page)]
     :post [(sequential? %)]}
    (let [res (sql/query ds-opts ["SELECT * FROM thumbnails ORDER BY timestr ASC LIMIT ? OFFSET ?" num-per-page offset])]
        res))

(defn get-thumb
    [id]
    {:pre [(integer? id)]
     :post [(or (nil? %) (map? %))]}
    (let [res (sql/query ds-opts ["SELECT * FROM thumbnails WHERE ID = ?" id])]
        (first res)))

(defn get-num-thumbs
    []
    {:post [(integer? %)]}
    (let [res (sql/query ds-opts ["SELECT count(*) FROM thumbnails"])]
        (-> res first vals first)))

(defn get-all-thumb-ids
    []
    {:post [(sequential? %)]}
    (let [res (sql/query ds-opts ["SELECT id FROM thumbnails ORDER BY timestr ASC"])]
        (map :id res)))

(defn get-all-thumbs
    []
    {:post [(sequential? %)]}
    (let [res (sql/query ds-opts ["SELECT * FROM thumbnails ORDER BY timestr ASC"])]
        res))

(defn delete-thumb
    [id]
    {:pre [(int? id)]
     :post [(boolean? %)]}
    (let [res (sql/delete! ds-opts :thumbnails {:id id})]
        (if (and (some? res) (= (:update_count (first res)) 1))
            (let [res2 (sql/delete! ds-opts :tags_m2m {:id id})]
                (and (some? res2) (>= (:update_count (first res2)) 0)))
            false)))

(defn find-thumb-by-date
    [date]
    {:pre [(int? date) (< 19700101 date 20251231)]
     :post [(or (nil? %) (map? %))]}
    (let [dstr (str/replace (str date) #"^(\d{4})(\d{2})(\d{2})$" "$1-$2-$3")
          res (sql/query ds-opts ["SELECT * FROM thumbnails WHERE timestr >= ? ORDER BY timestr ASC LIMIT 1" dstr])]
        (if (empty? res)
            nil
            (first res))))

;;-----------------------------------------------------------------------------

(defn get-grid
    [id]
    {:pre [(string? id)]
     :post [(int? %)]}
    (let [res (sql/query ds-opts ["SELECT num_per_page FROM grid WHERE id = ?" id])]
        (if (nil? res)
            25 ; default grid
            (:num_per_page (first res)))))

(defn save-grid
    [id num-pics]
    {:pre [(integer? id) (integer? num-pics)]}
    (let [num (get-grid id)]
        (if (or (nil? num) (not= num num-pics))
            (sql/insert! ds-opts :grid {:id id, :num_per_page num-pics})
            (sql/update! ds-opts :grid {:num_per_page num-pics} {:id id}))))

