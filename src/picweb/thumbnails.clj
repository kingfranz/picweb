(ns picweb.thumbnails
    (:require [clojure.spec.alpha :as s]
              [clojure.set :refer [intersection]]
              [next.jdbc.sql :as sql]
              [picweb.utils :refer [ds-opts min-start default-page-size ss-valid? min-grid-size max-grid-size]]))

;;-----------------------------------------------------------------------------

(def timestr-regex #"^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}((\+| )\d{2}:\d{2})?$")


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

(def ^:private thumbnails (atom nil))
(def ^:private time-index (atom nil))

(def ^:private tag2id (atom nil))

(defn load-thumbnails
    []
    (let [tn-data (sql/query ds-opts ["SELECT * FROM thumbnails ORDER BY id ASC"])
          tn-conf (s/conform :thumbnails/db tn-data)
          m2m-data (sql/query ds-opts ["SELECT * FROM tag_m2m ORDER BY tag_id ASC"])]
        (doseq [m tn-data
                :when (not (s/valid? :thumbnails/thumbnail m))]
            (s/explain :thumbnails/thumbnail m))
        (reset! time-index (sort-by first (mapv (fn [m] [(:timestr m) (:id m)]) tn-data)))
        (reset! thumbnails (into (sorted-map) (apply merge (mapv (fn [m] {(:id m) m}) tn-data))))
        (reset! tag2id (mapv (fn [x] [(:id x) (:tag_id x)]) (sort-by :id m2m-data)))))

(defn spy
    [x]
    (println "SPY:" x)
    x)

;-----------------------------------------------------------------------------
; update functions
;-----------------------------------------------------------------------------

(defn db-ok?
    [res]
    (and (some? res) (= (:update_count (first res)) 1)))

(defn db-ok-0?
    [res]
    (and (some? res) (>= (:update_count (first res)) 0)))

(def target-type #{:before :exact :after})

(defn find-idx-by-time
    [time-str target]
    {:pre [(ss-valid? :thumbnails/timestr time-str) (ss-valid? target-type target)]
     :post [(ss-valid? (s/nilable nat-int?) %)]}
    (if (= target :before)
        (loop [idx (dec (count @time-index))]
            (when-let [time-entry (nth @time-index idx nil)]
                (if (< (compare (first time-entry) time-str) 0)
                    (if (= idx 0) nil idx)
                    (recur (dec idx)))))
    (loop [idx 0]
        (when-let [time-entry (nth @time-index idx nil)]
            (if (and (= target :exact) (= (first time-entry) time-str))
                idx
                (if (and (= target :after) (> (compare (first time-entry) time-str) 0))
                    idx
                    (recur (inc idx))))))))

(defn update-thumb
    [id data]
    {:pre [(ss-valid? :thumbnails/id id) (ss-valid? :thumbnails/thumbnail data)]
     :post [(ss-valid? boolean? %)]}
    (let [res (sql/update! ds-opts :thumbnails data {:id id})]
        (if (db-ok? res)
            (swap! thumbnails assoc id data)
            false)))

(defn update-rating
    [pic-id rating]
    {:pre [(ss-valid? :thumbnails/id pic-id) (ss-valid? :thumbnails/rating rating)]
     :post [ss-valid? (boolean? %)]}
    (let [res (sql/update! ds-opts :thumbnails {:rating rating} {:id pic-id})]
        (if (db-ok? res)
            (swap! thumbnails assoc-in [pic-id :rating] rating)
            false)))

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
; get functions
;-----------------------------------------------------------------------------

(defn- get-tn
    [idx]
    (let [ti (nth @time-index idx nil)
          tn (get @thumbnails (second ti) nil)]
        tn))
(defn get-thumbs
    [start-time page-sz]
    {:pre [(ss-valid? :thumbnails/timestr start-time) (ss-valid? :thumbnails/page-size page-sz)]
     :post [(ss-valid? sequential? %)]}
    (let [start-idx (find-idx-by-time start-time :after)
          stop-idx (if start-idx
                         (min (+ start-idx page-sz) (count @time-index))
                         0)
          rr (range start-idx stop-idx)
          result (vec (map get-tn rr))]
        result))

(defn get-thumb
    [id]
    {:pre [(ss-valid? :thumbnails/id id)]
     :post [(ss-valid? (s/nilable :thumbnails/thumbnail) %)]}
    (get @thumbnails id nil))

(defn get-thumbs-by-tags
    [tag-ids]
    {:pre [(ss-valid? (s/coll-of :thumbnails/id :kind set) tag-ids)]
     :post [(ss-valid? (s/coll-of :thumbnails/thumbnail :kind vector) %)]}
    (->> @tag2id
         (filter #(contains? tag-ids (second %)))
         (map #(first %))
         (set)
         (mapv get-thumb)))

(defn get-thumbs-by-rating
    [ratings]
    {:pre [(ss-valid? (s/coll-of :thumbnails/rating :kind set) ratings)]
     :post [(ss-valid? (s/coll-of :thumbnails/thumbnail :kind vector) %)]}
    (->> @thumbnails
         (vals)
         (filter #(contains? ratings (:rating %)))
         (vec)))

(s/def :thumbnails/tag-set (s/coll-of :thumbnails/id :kind set))
(s/def :thumbnails/rating-set (s/coll-of (s/int-in 0 6) :kind set))
(s/def :thumbnails/selection (s/keys :opt-un [:thumbnails/tag-set :thumbnails/rating-set]))

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
    (loop [idx (dec (count @time-index))]
        (let [time-entry (nth @time-index idx nil)]
            (when time-entry
                (if (= (second time-entry) id)
                    (if (= idx 0)
                        nil
                        (get @thumbnails (second (nth @time-index (dec idx)))))
                    (recur (dec idx)))))))

(defn get-next-thumb
    [id]
    {:pre [(ss-valid? :thumbnails/id id)]
     :post [(ss-valid? (s/nilable :thumbnails/thumbnail) %)]}
    (loop [idx 0]
        (let [time-entry (nth @time-index idx nil)]
            (when time-entry
                (if (= (second time-entry) id)
                    (if (= idx (dec (count @time-index)))
                        nil
                        (get @thumbnails (second (nth @time-index (inc idx)))))
                    (recur (inc idx)))))))

(defn get-nth-thumb
    [num]
    {:pre [(ss-valid? nat-int? num)]
     :post [(ss-valid? (s/nilable :thumbnails/thumbnail) %)]}
    (when-let [res (nth @time-index num nil)]
        (get @thumbnails (second res) nil)))

(defn get-num-thumbs
    []
    {:post [(integer? %)]}
    (count @thumbnails))

(defn get-all-thumb-ids
    []
    {:post [(sequential? %)]}
    (keys @thumbnails))

(defn get-all-thumbs
    []
    {:post [(sequential? %)]}
    (vals @thumbnails))

;;-----------------------------------------------------------------------------

(defn get-page-up
    [start-time page-sz]
    {:pre [(ss-valid? :thumbnails/timestr start-time) (ss-valid? :thumbnails/page-size page-sz)]
     :post [(ss-valid? :thumbnails/timestr %)]}
    (if-let [start-idx (find-idx-by-time start-time :before)]
        (let [new-idx (max 0 (- start-idx page-sz))]
            (first (nth @time-index new-idx)))
        start-time))


(defn get-page-down
    [start-time page-sz]
    {:pre [(ss-valid? :thumbnails/timestr start-time) (ss-valid? :thumbnails/page-size  page-sz)]
     :post [(ss-valid? :thumbnails/timestr %)]}
    (if-let [start-idx (find-idx-by-time start-time :after)]
        (let [new-idx (min (count @time-index) (+ start-idx page-sz))]
            (first (nth @time-index new-idx)))
        start-time))