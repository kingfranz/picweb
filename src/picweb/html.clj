(ns picweb.html
    (:require [clojure.string :as str]
              [hiccup.form :as hf]
              [hiccup.page :as page]
              [picweb.thumbnails :refer :all]
              [ring.util.response :as ring]
              [ring.middleware.cookies :as cookies]))

;;---------------------------------------------------------------------------

(def ing-root "/mnt/backup/final/")

(defn split-ext
    [filename]
    (let [parts (str/split filename #"\.")]
        (if (> (count parts) 1)
            [(str/join "." (butlast parts)) (last parts)]
            [filename ""])))

(defn mk-tn-name
    [thumb]
    (let [[fname ext] (split-ext (:filename thumb))]
        (str (:path thumb) "/" fname "_tn." ext)))

;;---------------------------------------------------------------------------

(defn contact-sheet
    [thumbs tags offset num-cols num-rows]
    [:div.contact-sheet
     (for [thumb thumbs]
         ;         (println "contact-sheet: " (:id thumb))
         [:div.contact-image
          [:a {:href (str "/pic/" (:id thumb))} [:img {:src (str "/thumb/" (:id thumb))}]]
          ])])

(defn contact-page
    [cookie offset]
    (let [col-per-page (if (some? (:col cookie)) (Integer/parseInt (:col cookie)) 4)
          rows-per-page (if (some? (:row cookie)) (Integer/parseInt (:row cookie)) 3)
          num-thumbs (* col-per-page rows-per-page)
          pics (get-thumbs offset num-thumbs)]
        (if (empty? pics)
            [:div "No pictures found."]
            (let [tags (get-all-tags)]
                (page/html5
                    [:head
                     [:title "Contact Sheet"]
                     [:link {:rel "stylesheet" :href "/css/style.css"}]]
                    [:body
                     [:h1 "Contact Sheet"]
                     (hf/form-to
                         [:post (str "/cookieupd/" offset)]
                     [:span
                      [:label "Columns: "]
                      (hf/text-field {:size 2 :maxlength 2} :col col-per-page)
                      [:label "Rows: "]
                      (hf/text-field {:size 2 :maxlength 2} :rows rows-per-page)
                      (hf/submit-button {:class "submit"} "Update")])
                     (contact-sheet pics tags offset col-per-page rows-per-page)
                     [:div.pagination
                      (when (> offset 0)
                          [:a {:href (str "/offset/" (- offset num-thumbs))} "Previous"])
                      [:span.page-info (str "Page " (inc (/ offset num-thumbs)))]
                      (when (< (+ offset num-thumbs) (get-num-thumbs))
                          [:a {:href (str "/offset/" (+ offset num-thumbs))} "Next"])]])))))

;;---------------------------------------------------------------------------

(defn update-cookie
    [cookie offset col rows]
        (if (and (some? col) (some? rows))
;            (do
                (-> (ring/redirect (str "/offset/" offset) 303)
                    (assoc-in [:cookies "col" :value](str col))
                    (assoc-in [:cookies "row" :value] (str rows)))
                ;(ring/set-cookie request "col" col)
                ;(ring/set-cookie request "row" rows)
                ;(assoc (ring/redirect (str "/offset/" offset) 303)
                ;    :headers {:cookies {"col" col "row" rows}}))
            (ring/response "Invalid parameters.")))

;;---------------------------------------------------------------------------

(defn pic-page
    [cookie pic-id]
    (let [pic (get-thumb pic-id)]
        (if (empty? pic)
            [:div "Picture not found."]
            (let [all-tags (get-all-tags)
                  pic-tags (map :tag_id (get-pic-tags pic-id))
                  xxx (println pic)]
                (page/html5
                    [:head
                     [:title "Picture Details"]
                     [:link {:rel "stylesheet" :href "/css/style.css"}]]
                    [:body
                     [:h1 "Picture Details"]
                     [:table.pic-details-table
                      [:tr.pic-details-tr
                       [:td.pic-details-td (str "Path: " (:path pic) "/" (:filename pic))]
                       [:td.pic-details-td (str "Timestamp: " (:timestr pic))]]
                      [:tr.pic-details-tr
                       [:td.pic-details-td (str "Size: " (:size pic))]
                       [:td.pic-details-td (str "Dim: " (:xres pic) "x" (:yres pic))]]
                      ]
                     [:div.pic-image
                      [:img {:src   (str "/picture/" pic-id)
                             :alt   (:filename pic)
                             :title (:filename pic)}]]
                     (hf/form-to
                         [:post "/tagupdate"]
                         [:div.tags
                          (for [tag (sort-by :name all-tags)]
                              [:span.tags
                               [:label.tags (:name tag)]
                               (hf/check-box {:class "tags"}
                                   (str "tag_" (:name tag))
                                   (some #(= (:tag_id tag) %) pic-tags))])
                          (hf/text-field {:type "hidden" :value pic-id} "pic-id")
                          [:label "Add a new tag:"]
                          (hf/text-field {:type "text" :value ""} "new")]

                         [:p]
                         [:span.rating
                         [:label.rating "Rating: "]
                         (hf/drop-down {:class "rating"} :rating
                                       [["no-id" "No rating"]
                                        ["Delete it!" "1"]
                                        ["Move it" "2"]
                                        ["Not great" "3"]
                                        ["Average" "4"]
                                        ["Great" "5"]]
                                        (if (:rating pic)
                                             (str (:rating pic))
                                             "no-id"))
                          ]
                         [:p]
                         (hf/submit-button {:class "submit"} "Update!"))
                     [:div.back-link
                      [:a {:href "/"} "Back to Contact Sheet"]]])))))

;;---------------------------------------------------------------------------

(defn get-pic
    [cookie pic-id]
    (let [pic (get-thumb pic-id)]
        (if (empty? pic)
            (ring/response "Picture not found.")
            (let [img-path (str (:path pic) "/" (:filename pic))]
                (ring/file-response img-path
                                    {:headers {"Content-Type" "image/jpeg"}})))))

(defn get-thumb-pic
    [cookie pic-id]
    (let [pic (get-thumb pic-id)]
        (if (empty? pic)
            (ring/response "Picture not found.")
            (let [img-path (mk-tn-name pic)]
                (ring/file-response img-path
                                    {:headers {"Content-Type" "image/jpeg"}})))))

(defn get-css
    [cookie]
    (ring/file-response "/home/soren/Linux/clojure/picweb/resources/public/css/style.css"
                        {:headers {"Content-Type" "text/css"}}))

;;---------------------------------------------------------------------------

(defn update-tags
    [cookie pic-id new-tag params]
    (try
        (let [tags (filter #(str/starts-with? % "tag_") (keys params))
              pic-tags (get-pic-tags pic-id)]
            ; check existing tags
            (doseq [tag tags]
                (let [tag-id (get-in params [tag])]
                    (if (and (not (empty? tag-id)) (not= tag-id "no-id"))
                        (save-tag pic-id tag-id)
                        (remove-tag pic-id tag-id))))
            ; check new tag
            (when (not (empty? new-tag))
                (when-not (save-tag pic-id (str/lower-case new-tag))
                    (ring/response "Failed to save new tag.")))
            ; check rating
            (let [rating (get-in params [:rating])]
                (when (and rating (not= rating "no-id"))
                    (let [rating-value (Integer/parseInt rating)]
                        (if (and (>= rating-value 1) (<= rating-value 5))
                            (do
                                (update-rating pic-id rating-value)
                                ; Save the rating logic here
                                ; For example, save to a database or update pic metadata
                                (ring/redirect (str "/pic/" pic-id)))
                            (ring/response "Invalid rating value."))))))

            (catch Exception e
                (println "Error updating tags:" (.getMessage e))
                (ring/response "Error updating tags."))))

;;---------------------------------------------------------------------------

(defn four-oh-four
    []
    (page/html5
        [:head
         [:title "404 Not Found"]
         [:link {:rel "stylesheet" :href "/css/style.css"}]]
        [:body
         [:h1 "404 Not Found"]
         [:p "The page you are looking for does not exist."]]))

;;---------------------------------------------------------------------------

