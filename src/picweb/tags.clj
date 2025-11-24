(ns picweb.tags
    (:require [hiccup.page :as page]
              [hiccup.form :refer :all]
              [picweb.thumbnails :refer [ds-opts insert]]
              [next.jdbc.sql :as sql]
              [ring.util.response :as ring]))

;;-----------------------------------------------------------------------------

(defn get-all-tags
    []
    {:post [(sequential? %)]}
    (let [res (sql/query ds-opts ["SELECT * FROM tags ORDER BY name COLLATE NOCASE ASC"])]
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

(defn has-tag?
    [pic-id tag-id]
    {:pre [(int? pic-id) (int? tag-id)]
     :post [(boolean? %)]}
    (let [res (sql/query ds-opts ["SELECT * FROM tag_m2m WHERE id = ? AND tag_id = ?" pic-id tag-id])]
        (seq res)))

(defn assoc-tag
    [pic-id tag-id]
    {:pre [(int? pic-id) (int? tag-id)]
     :post [(or (nil? %) (int? %))]}
    (if (has-tag? pic-id tag-id)
        tag-id
        (let [res (insert :tag_m2m {:id pic-id, :tag_id tag-id})]
            res)))

(defn delete-tag-from-db
    [tag-id]
    {:pre [(int? tag-id)]
     :post [(boolean? %)]}
    (let [res1 (sql/delete! ds-opts :tag_m2m {:tag_id tag-id})]
        (if (> (res1 :next.jdbc/update-count) 0)
            (let [res2 (sql/delete! ds-opts :tags {:tag_id tag-id})]
                (if (> (res2 :next.jdbc/update-count) 0)
                    true
                    false))
            false)))

(defn has-tags?
    [pic-id]
    {:pre [(int? pic-id)]
     :post [(boolean? %)]}
    (let [res (sql/query ds-opts ["SELECT COUNT(*) AS cnt FROM tag_m2m WHERE id = ?" pic-id])]
        (> (:cnt (first res)) 0)))

(defn mk-tag-str
    [pic-id]
    {:pre  [(int? pic-id)]
     :post [(string? %)]}
    (let [res (sql/query ds-opts ["SELECT name FROM tags WHERE tag_id IN (SELECT tag_id from tag_m2m WHERE ID = ?)" pic-id])
          ret (if (empty? res)
            ""
            (apply str (interpose "," (map :name res))))]
        ret))

(defn save-tag
    [pic-id tag-name]
    {:pre [(int? pic-id) (string? tag-name)]
     :post [(or (nil? %) (int? %))]}
    (if-let [tag-id (find-tag-id tag-name)]
        (when (assoc-tag pic-id tag-id)
            tag-id)
        (let [new-tag-id (insert :tags {:name tag-name})]
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
    (if-let [tag-id (find-tag-id tag-name)]
        tag-id
        (let [new-tag-id (insert :tags {:name tag-name})]
            new-tag-id)))

(defn disassoc-tag
    [pic-id tag-id]
    {:pre [(int? pic-id) (int? tag-id)]
     :post [(boolean? %)]}
    (let [res (sql/delete! ds-opts :tag_m2m {:id pic-id :tag_id tag-id})]
        (and (some? res) (= (:update_count (first res)) 1))))

(defn get-all-m2m
    []
    (sql/query ds-opts ["SELECT * FROM tag_m2m ORDER BY tag_id"]))

;;-----------------------------------------------------------------------------------------

(defn edit-tags
    []
    (page/html5
        [:head
         (page/include-css "/css/style.css")
         [:style "body {width: 50%;}"]
         (page/include-css "/css/w3.css")
         [:meta {:http-equiv "cache-control" :content "no-cache, must-revalidate, post-check=0, pre-check=0"}]
         [:meta {:http-equiv "cache-control" :content "max-age=0"}]
         [:meta {:http-equiv "expires" :content "0"}]
         [:meta {:http-equiv "expires" :content "Tue, 01 Jan 1980 1:00:00 GMT"}]
         [:meta {:http-equiv "pragma" :content "no-cache"}]
         [:title "Tags"]
         ]
        [:body
         (let [all-tags (get-all-tags)
               all-links (get-all-m2m)
               tags-with-extra (map #(assoc % :usage (count (filter (fn [ll] (= (:tag_id %) (:tag_id ll))) all-links))) all-tags)]
             [:div.w3-container
             [:table.w3-table.w3-border.w3-small
              [:tr
               [:th.bkg "Name"]
               [:th.bkg "ID"]
               [:th.bkg "Num"]
               [:th.bkg "Rename"]
               [:th.bkg "Delete"]
               ]
              (for [t tags-with-extra]
                  [:tr
                   (form-to
                       [:post (str "/rename-tag?tag-id=" (:tag_id t))]
                       [:td.bkg (text-field (keyword (str "name-" (:tag_id t))) (:name t))]
                   [:td.bkg (:tag_id t)]
                   [:td.bkg (:usage t)]
                   [:td.bkg  (submit-button "Rename")])
                   [:td.bkg [:a {:href (str "/delete-tag?tag-id=" (:tag_id t))}  "Del"]] ;[:button {:onclick "return confirm('Are you sure?')"}]]]
                   ])]])
         [:p]
         [:p]
         [:a.pagination {:href "/"} "Back to contact sheet"]
         ]))

(defn rename-tag
    [tag-id request]
    (let [new-name (get-in request [:params (keyword (str "name-" tag-id))])]
        (sql/update! ds-opts :tags {:name new-name} {:tag_id tag-id})
        (ring/redirect "/edit-tags")))

(defn delete-tag
    [tag-id]
    (if (delete-tag-from-db tag-id)
        (ring/redirect "/edit-tags")
        (ring/response "Unable to delete tag")))