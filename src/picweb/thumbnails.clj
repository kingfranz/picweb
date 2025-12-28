(ns picweb.thumbnails
    (:require [clojure.spec.alpha :as s]
              [clojure.string :as str]
              [clojure.set :refer [intersection]]
              [next.jdbc.sql :as sql]
              [picweb.utils :refer [ds-opts min-start default-page-size ss-valid?
                                    min-grid-size max-grid-size]]))

;;-----------------------------------------------------------------------------

(def timestr-regex #"^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}$")


(s/def :thumbnails/id nat-int?)
(s/def :thumbnails/timestr (s/and string? #(re-matches timestr-regex %)))
(s/def :thumbnails/rating (s/nilable nat-int?))
(s/def :thumbnails/path string?)
(s/def :thumbnails/filename string?)
(s/def :thumbnails/size nat-int?)
(s/def :thumbnails/xres nat-int?)
(s/def :thumbnails/yres nat-int?)
(s/def :thumbnails/thumbnail (s/keys :req-un [:thumbnails/id
                                              :thumbnails/timestr
                                              :thumbnails/rating
                                              :thumbnails/path
                                              :thumbnails/filename
                                              :thumbnails/size
                                              :thumbnails/xres
                                              :thumbnails/yres]))
(s/def :thumbnails/time-index (s/cat :time #(ss-valid? :thumbnails/timestr %)
                                     :id #(ss-valid? :thumbnails/id %)))

(s/def :thumbnails/db (s/coll-of :thumbnails/thumbnail :kind vector? :distinct true))
(s/def :thumbnails/page-size (s/int-in min-grid-size max-grid-size))

(defn db-ok?
    [res]
    (and (some? res) (= (:update_count (first res)) 1)))

(defn db-ok-0?
    [res]
    (and (some? res) (>= (:update_count (first res)) 0)))

(def target-type #{:before :exact :after})

(defn update-thumb
    [id data]
    {:pre [(ss-valid? :thumbnails/id id) (ss-valid? :thumbnails/thumbnail data)]
     :post [(ss-valid? boolean? %)]}
    (let [res (sql/update! ds-opts :thumbnails data {:id id})]
        (db-ok? res)))

(defn update-rating
    [pic-id rating]
    {:pre [(ss-valid? :thumbnails/id pic-id) (ss-valid? :thumbnails/rating rating)]
     :post [ss-valid? (boolean? %)]}
    (let [res (sql/update! ds-opts :thumbnails {:rating rating} {:id pic-id})]
        (db-ok? res)))

(defn delete-thumb
    [id]
    {:pre [(ss-valid? :thumbnails/id id)]
     :post [ss-valid? (boolean? %)]}
    (let [res (sql/delete! ds-opts :thumbnails {:id id})]
        (if (db-ok? res)
            (let [res2 (sql/delete! ds-opts :tags_m2m {:id id})]
                (db-ok-0? res2))
            ; TODO: remove in memory too
            false)))


;;-----------------------------------------------------------------------------

(defn get-thumbs
    [start-time page-sz]
    {:pre [(ss-valid? :thumbnails/timestr start-time)
           (ss-valid? :thumbnails/page-size page-sz)]
     :post [(ss-valid? sequential? %)]}
    (let [res (sql/query ds-opts ["SELECT *
                                   FROM thumbnails
                                   WHERE timestr >= ?
                                   ORDER BY timestr ASC LIMIT ?" start-time page-sz])]
        res))

(defn get-thumb
    [id]
    {:pre [(ss-valid? :thumbnails/id id)]
     :post [(ss-valid? (s/nilable :thumbnails/thumbnail) %)]}
    (let [res (sql/query ds-opts ["SELECT * FROM thumbnails WHERE ID = ?" id])]
        (first res)))

(defn get-thumbs-by-tags
    [tag-ids]
    {:pre [(ss-valid? (s/coll-of :thumbnails/id :kind set) tag-ids)]
     :post [(ss-valid? (s/coll-of :thumbnails/thumbnail :kind vector) %)]}
    (let [target (apply str (interpose "," tag-ids))
          res (sql/query ds-opts ["SELECT *
                                   FROM thumbnails
                                   WHERE ID IN
                                    (SELECT ID
                                     FROM tag_m2m
                                     WHERE tag_id IN (?))" target])]
        res))

(defn get-thumbs-by-rating
    [ratings]
    {:pre [(ss-valid? (s/coll-of :thumbnails/rating :kind set) ratings)]
     :post [(ss-valid? (s/coll-of :thumbnails/thumbnail :kind vector) %)]}
    (let [target (apply str (interpose "," ratings))
          res (sql/query ds-opts ["SELECT *
                                   FROM thumbnails
                                   WHERE rating IN (?)" target])]
        res))

(s/def :thumbnails/tag-set (s/coll-of :thumbnails/id :kind set))
(s/def :thumbnails/rating-set (s/coll-of (s/int-in 0 6) :kind set))
(s/def :thumbnails/selection (s/keys :opt-un [:thumbnails/tag-set
                                              :thumbnails/rating-set]))

(defn get-filtered-thumbs
    [selected]
    {:pre [(ss-valid? :thumbnails/selection selected)]
     :post [(ss-valid? (s/coll-of :thumbnails/thumbnail :kind vector) %)]}
    (cond
        (and (empty? (:tag-set selected)) (empty? (:rating-set selected)))
            (get-thumbs min-start default-page-size)
        (empty? (:tag-set selected))
            (get-thumbs-by-rating (:rating-set selected))
        (empty? (:rating-set selected))
            (get-thumbs-by-tags (:tag-set selected))
        :else (let [by-tags (set (get-thumbs-by-tags (:tag-set selected)))
                    by-ratings(set (get-thumbs-by-rating (:rating-set selected)))]
                  (intersection by-tags by-ratings))))

(defn get-prev-thumb
    [id]
    {:pre [(ss-valid? :thumbnails/id id)]
     :post [(ss-valid? (s/nilable :thumbnails/thumbnail) %)]}
    (let [prev (sql/query ds-opts ["SELECT *
                                    FROM thumbnails
                                    WHERE id < ?
                                    ORDER BY id DESC LIMIT 1" id])]
        (if (empty? prev)
            nil
            (first prev))))

(defn get-next-thumb
    [id]
    {:pre [(ss-valid? :thumbnails/id id)]
     :post [(ss-valid? (s/nilable :thumbnails/thumbnail) %)]}
    (let [next (sql/query ds-opts ["SELECT *
                                    FROM thumbnails
                                    WHERE id > ?
                                    ORDER BY id ASC LIMIT 1" id])]
        (if (empty? next)
            nil
            (first next))))

(defn get-nth-thumb
    [num]
    {:pre [(ss-valid? nat-int? num)]
     :post [(ss-valid? (s/nilable :thumbnails/thumbnail) %)]}
    (let [res (sql/query ds-opts ["SELECT *
                                   FROM thumbnails
                                   ORDER BY timestr ASC LIMIT 1 OFFSET ?" num])]
        (first res)))

(defn get-num-thumbs
    []
    {:post [(integer? %)]}
    (let [res (sql/query ds-opts ["SELECT count(*) FROM thumbnails"])]
        (-> res first vals first)))

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

;;-----------------------------------------------------------------------------

(defn get-page-up
    [start-time page-sz]
    {:pre [(ss-valid? :thumbnails/timestr start-time)
           (ss-valid? :thumbnails/page-size page-sz)]
     :post [(ss-valid? (s/nilable :thumbnails/timestr) %)]}
    (let [res (sql/query ds-opts ["SELECT timestr
                                   FROM thumbnails
                                   WHERE timestr < ?
                                   ORDER BY timestr DESC
                                   LIMIT 1 OFFSET ?" start-time page-sz])]
        (if (and (vector? res) (map? (first res)) (contains? (set (keys (first res))) :timestr))
            (get (first res) :timestr)
            nil)))

(defn get-page-down
    [start-time page-sz]
    {:pre [(ss-valid? :thumbnails/timestr start-time)
           (ss-valid? :thumbnails/page-size  page-sz)]
     :post [(ss-valid? (s/nilable :thumbnails/timestr) %)]}
    (let [res (sql/query ds-opts ["SELECT timestr
                                   FROM thumbnails
                                   WHERE timestr > ?
                                   ORDER BY timestr ASC
                                   LIMIT 1 OFFSET ?" start-time page-sz])]
        (if (and (vector? res) (map? (first res)) (contains? (set (keys (first res))) :timestr))
            (get (first res) :timestr)
            nil)))