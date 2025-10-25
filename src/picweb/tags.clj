(ns picweb.tags
    (:require [clojure.string :as str]
              [picweb.thumbnails :refer :all]
              [next.jdbc :as jdbc]
              [next.jdbc.result-set :as rs]
              [next.jdbc.sql :as sql]))

;;-----------------------------------------------------------------------------

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

(defn delete-tag
    [tag-id]
    {:pre [(int? tag-id)]
     :post [(boolean? %)]}
    (let [res1 (sql/delete! ds-opts :tag_m2m {:tag_id tag-id})
          res2 (sql/delete! ds-opts :tags {:tag_id tag-id})]
        (and (some? res2) (= (:update_count (first res2)) 1))))

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

(defn save-new-tag
    [tag-name]
    {:pre [(string? tag-name)]
     :post [(or (nil? %) (int? %))]}
    (if-let [tag-id (find-tag-id (str/lower-case tag-name))]
        tag-id
        (let [new-tag-id (insert :tags {:name (str/lower-case tag-name)})]
            new-tag-id)))

(defn disassoc-tag
    [pic-id tag-id]
    {:pre [(int? pic-id) (int? tag-id)]
     :post [(boolean? %)]}
    (let [res (sql/delete! ds-opts :tag_m2m {:id pic-id :tag_id tag-id})]
        (and (some? res) (= (:update_count (first res)) 1))))
