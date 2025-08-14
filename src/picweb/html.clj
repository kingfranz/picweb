(ns picweb.html
    (:require [clojure.pprint :as pp]
              [clojure.set :as set]
              [clojure.string :as str]
              [hiccup.form :as hf]
              [hiccup.page :as page]
              [picweb.thumbnails :refer :all]
              [ring.util.response :as ring]))

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
    [thumbs]
    [:div.contact-sheet
     (for [thumb thumbs]
         [:figure.contact-image
          [:a {:href (str "/pic/" (:id thumb))} [:img {:src (str "/thumb/" (:id thumb))}]]
          [:figcaption.contact-text (subs (:timestr thumb) 0 10)]
          ])])

(defn clamp
    [n min-val max-val]
    (cond
        (< n min-val) min-val
        (> n max-val) max-val
        :else n))

(defn contact-page
    [offset remote-addr]
    (let [num-thumbs (get-grid remote-addr)
          pics (get-thumbs offset num-thumbs)]
        (if (empty? pics)
            [:div "No pictures found."]
            (page/html5
                [:head
                 [:title "Contact Sheet"]
                 [:link {:rel "stylesheet" :href "/css/style.css"}]]
                [:body
                 [:h1 "Contact Sheet"]
                 (hf/form-to
                     [:post (str "/gridupdate/" offset)]
                     [:span
                      [:label "Pictures per page: "
                      (hf/text-field {:size 3 :maxlength 3} :num_per_page num-thumbs)]
                      (hf/submit-button {:class "submit"} "Update")])
                 (contact-sheet pics)
                 [:div.pagination1
                  (when (> offset 0)
                      (let [pre-pages (int (/ offset num-thumbs))]
                          (if (> pre-pages 10)
                              [:span.pagination
                               (for [i (range 0 3)]
                                   [:a.pagination {:href (str "/offset/" (* i num-thumbs))} (str " " i " ")])
                               [:span.pagination "..."]
                               (for [i (range (- pre-pages 3) pre-pages)]
                                   [:a.pagination {:href (str "/offset/" (* i num-thumbs))} (str " " i " ")])]
                              (for [i (range 0 pre-pages)]
                                  [:a.pagination {:href (str "/offset/" (* i num-thumbs))}
                                   (str " " i " ")]))))
                  [:span.pagination " This page "]
                  (let [post-pages (int (/ (- (get-num-thumbs) offset) num-thumbs))]
                      (if (> post-pages 10)
                          [:span.pagination
                           (for [i (range 1 4)]
                               [:a.pagination {:href (str "/offset/" (+ offset (* i num-thumbs)))}
                                (str " " (+ (int (/ offset num-thumbs)) i) " ")])
                           [:span.pagination " ... "]
                           (for [i (range (- post-pages 3) post-pages)]
                               [:a.pagination {:href (str "/offset/" (+ offset (* i num-thumbs)))}
                                (str " " (+ (int (/ offset num-thumbs)) i) " ")])]
                          (for [i (range 0 post-pages)]
                              [:a.pagination {:href (str "/offset/" (+ offset (* i num-thumbs)))}
                               (str " " (+ (int (/ offset num-thumbs)) i) " ")])))]]))))

;;---------------------------------------------------------------------------

(defn update-grid
    [offset num-pics remote-addr]
    (if (some? num-pics)
        (do
            (save-grid remote-addr num-pics)
            (ring/redirect (str "/offset/" offset) 303))
        (ring/response "Invalid parameters.")))

;;---------------------------------------------------------------------------

(defn pic-page
    [pic-id]
    (let [pic (get-thumb pic-id)]
        (if (empty? pic)
            [:div "Picture not found."]
            (let [all-tags (get-all-tags)
                  pic-tags (map :tag_id (get-pic-tags pic-id))]
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
                         [:span.pagination
                          [:a.pagination {:href (str "/prev/" pic-id)} "Previous"]
                          [:a.pagination {:href (str "/next/" pic-id)} "Next"]]
                         [:p]
                         (hf/submit-button {:class "submit"} "Update!"))
                     [:div.back-link
                      [:a {:href "/"} "Back to Contact Sheet"]]])))))

;;---------------------------------------------------------------------------

(defn get-pic
    [pic-id]
    (let [pic (get-thumb pic-id)]
        (if (empty? pic)
            (ring/response "Picture not found.")
            (let [img-path (str (:path pic) "/" (:filename pic))]
                (ring/file-response img-path
                                    {:headers {"Content-Type" "image/jpeg"}})))))

(defn get-thumb-pic
    [pic-id]
    (let [pic (get-thumb pic-id)]
        (if (empty? pic)
            (ring/response "Picture not found.")
            (let [img-path (mk-tn-name pic)]
                (ring/file-response img-path
                                    {:headers {"Content-Type" "image/jpeg"}})))))

(defn get-css
    []
    (ring/file-response "/home/soren/Linux/clojure/picweb/resources/public/css/style.css"
                        {:headers {"Content-Type" "text/css"}}))

;;---------------------------------------------------------------------------

(defn check-name
    [x]
    (let [nn (name x)
          ss (str/starts-with? nn "tag_")]
        (println "" nn "->" ss)
        ss))

(defn checked-tags
    [params]
    (let [ttt (->> params
                   (keys)
                   (filter #(check-name %))
                   (map #(subs (name %) 4))
                   (map str/lower-case)
                   (map #(find-tag-id %))
                   (remove nil?))]
        ttt))

(defn update-tags
    [pic-id new-tag params]
    (try
        (let [tag-ids (set (checked-tags params))
              pic-tag-ids (set (map :tag_id (get-pic-tags pic-id)))
              ; Remove tags that are not in the current pic
              to-remove (set/difference pic-tag-ids tag-ids)
              to-add (set/difference tag-ids pic-tag-ids)]
            ; check existing tags
            (doseq [tag-id to-remove]
                (remove-tag pic-id tag-id))
            (doseq [tag-id to-add]
                (assoc-tag pic-id tag-id))
            ; check new tag
            (when (some? new-tag)
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
            (println "Stack trace:" (with-out-str (pp/pprint (.getStackTrace e))))
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

(defn get-prev
    [pic-id]
    (let [prev (get-prev-thumb pic-id)]
        (if (empty? prev)
            (ring/response "No previous picture.")
            (ring/redirect (str "/pic/" (:id prev))))))

(defn get-next
    [pic-id]
    (let [next (get-next-thumb pic-id)]
        (if (empty? next)
            (ring/response "No next picture.")
            (ring/redirect (str "/pic/" (:id next))))))
