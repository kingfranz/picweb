(ns picweb.thumbnails
    (:require [clojure.string :as str]
              [next.jdbc :as jdbc]
              [next.jdbc.result-set :as rs]
              [next.jdbc.sql :as sql]
              [ring.util.response :as ring]))

(def db {:dbtype "sqlite" :dbname "pictures.sqlite3"})
(def ds (jdbc/get-datasource db))
(def ds-opts (jdbc/with-options ds {:builder-fn rs/as-unqualified-lower-maps}))

;;-----------------------------------------------------------------------------

(defn spy
    [x]
    (println "SPY:" x)
    x)

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
     :post [(boolean? %)]}
    (let [res (sql/update! ds-opts :thumbnails data {:id id})]
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

(defn get-thumbs-by-tags
    [tag-ids]
    (let [target (apply str (interpose "," tag-ids))
          res (sql/query ds-opts ["SELECT * from thumbnails WHERE ID IN (SELECT ID FROM tag_m2m WHERE tag_id IN (?))" target])]
        res))

(defn get-thumbs-by-rating
    [ratings]
    (let [target (apply str (interpose "," ratings))
          res (sql/query ds-opts ["SELECT * from thumbnails WHERE rating IN (?)" target])]
        res))

(defn get-filtered-thumbs
    [selected]
    (cond
        (and (empty? (:tags selected)) (empty? (:ratings selected))) (get-thumbs 0 100)
        (empty? (:tags selected)) (get-thumbs-by-rating (:ratings selected))
        (empty? (:ratings selected)) (get-thumbs-by-tags (:tags selected))
        :else (let [by-tags (set (get-thumbs-by-tags (:tags selected)))
                    by-ratings (set (get-thumbs-by-rating (:ratings selected)))
                    intersect (clojure.set/intersection by-tags by-ratings)]
                ;(get-thumbs-by-tags intersect)
        intersect)))


(defn get-prev-thumb
    [id]
    {:pre [(int? id)]
     :post [(or (nil? %) (map? %))]}
    (let [prev (sql/query ds-opts ["SELECT * FROM thumbnails WHERE id < ? ORDER BY id DESC LIMIT 1" id])]
        (if (empty? prev)
            nil
            (first prev))))

(defn get-next-thumb
    [id]
    {:pre [(int? id)]
     :post [(or (nil? %) (map? %))]}
    (let [next (sql/query ds-opts ["SELECT * FROM thumbnails WHERE id > ? ORDER BY id ASC LIMIT 1" id])]
        (if (empty? next)
            nil
            (first next))))

(defn get-nth-thumb
    [num]
    {:pre [(integer? num)]
     :post [(or (nil? %) (map? %))]}
    (let [res (sql/query ds-opts ["SELECT * FROM thumbnails ORDER BY timeStr ASC LIMIT 1 OFFSET ?" num])]
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
    [remote-addr]
    {:pre [(string? remote-addr)]
     :post [(or (int? %) (nil? %))]}
    (let [res (sql/query ds-opts ["SELECT num_per_page FROM grid WHERE id = ?" remote-addr])]
        (if (empty? res)
            nil ; default grid
            (:num_per_page (first res)))))

(defn save-grid
    [remote-addr num-pics]
    {:pre [(string? remote-addr) (integer? num-pics)]}
    (let [num (get-grid remote-addr)]
        (if (nil? num)
            (sql/insert! ds-opts :grid {:id remote-addr, :num_per_page num-pics})
            (sql/update! ds-opts :grid {:num_per_page num-pics} {:id remote-addr}))
        (ring/redirect "/")))

