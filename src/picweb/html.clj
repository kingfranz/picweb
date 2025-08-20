(ns picweb.html
    (:require [clojure.java.shell :refer [sh]]
              [clojure.pprint :as pp]
              [clojure.set :as set]
              [clojure.string :as str]
              [hiccup.form :as hf]
              [hiccup.page :as page]
              [picweb.thumbnails :refer :all]
              [ring.util.response :as ring])
    (:import (java.io File)
             (java.nio.file Files)))

;;---------------------------------------------------------------------------

(def ing-root "/mnt/backup/final/")

(defn- split-ext
    [filename]
    (let [parts (str/split filename #"\.")]
        (if (> (count parts) 1)
            [(str/join "." (butlast parts)) (last parts)]
            [filename ""])))

(defn- mk-tn-name
    [thumb]
    (let [[fname ext] (split-ext (:filename thumb))]
        (str (:path thumb) "/" fname "_tn." ext)))

;;---------------------------------------------------------------------------

(defn- tag-and-rate
    [all-tags pic-tags pic-id pic]
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
         [:br]
         [:label "Add a new tag:"]
         [:input {:type "text" :name "new" :id "new" :class "tags"
                  :placeholder "New tag (lowercase)"}]

         ;(hf/text-field {:class "tags" :type "text" :value ""} "new")
         ]

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
        (hf/submit-button {:class "submit"} "Update!")))

(defn- pic-and-info
    [pic pic-id]
    [:div
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
             :title (:filename pic)}]]])

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
                     (pic-and-info pic pic-id)
                     [:div.wrapper
                       [:a {:href (str "/rotate-left/" pic-id)} "Rotate Left   .."]
                       [:a {:href (str "/rotate-right/" pic-id)} "..   Rotate Right"]
                      ]
                     (tag-and-rate all-tags pic-tags pic-id pic)
                     [:div.wrapper
                      [:a {:href (str "/prev/" pic-id)} "Previous   .."]
                      [:a {:href (str "/next/" pic-id)} "..   Next"]]
                     [:p]
                     [:div.back-link
                      [:a {:href (str "/sheetat/" pic-id)} "Back to Contact Sheet"]]])))))

;;---------------------------------------------------------------------------

(defn sheet-at
    [pic-id remote-addr]
    {:pre [(int? pic-id) (string? remote-addr)] }
    (let [num-thumbs (get-grid remote-addr)
          all-ids (get-all-thumb-ids)
          pic-idx (.indexOf all-ids pic-id)
          page-num (int (/ pic-idx num-thumbs))]
        (ring/redirect (str "/offset/" (* page-num num-thumbs)))))

(defn- file-size
    [path]
    (let [f (java.io.File. path)
          l (.length f)]
        l))

(defn- rotate-image
    [thumb pic-id angle]
    (let [img-path (str (:path thumb) "/" (:filename thumb))
          cmd (str "convert " img-path " -rotate " angle " " img-path)]
        (println "Rotating image with command:" cmd)
        (try
            ; run the command to rotate the image
            (let [result (clojure.java.shell/sh "bash" "-c" cmd)]
                (if (= 0 (:exit result))
                    ; if the command was successful, create a thumbnail
                    (let [tn-file (mk-tn-name (get-thumb pic-id))
                          tn-cmd (str "convert " img-path " -auto-orient -thumbnail 200x200^ -gravity center -extent 200x200 " tn-file)
                          tn-result (sh "bash" "-c" tn-cmd)]
                        (if (= 0 (:exit tn-result))
                            ; if thumbnail creation was successful, update DB
                            (let [image (javax.imageio.ImageIO/read (java.io.File. img-path))]
                                (update-thumb pic-id {:xres (.getWidth image)
                                                      :yres (.getHeight image)
                                                      :size (file-size img-path)})
                                (println "Image rotated and thumbnail created successfully.")
                                true)
                            (do
                                (println "Error creating thumbnail:" (:err tn-result))
                                false)))
                    (do
                        (println "Error rotating image:" (:err result))
                        false)))
            (catch Exception e
                (println "Exception during image rotation:" (.getMessage e))
                (println "Stack trace:" (with-out-str (pp/pprint (.getStackTrace e))))
                false))))

(defn rotate-left
    [pic-id]
    (let [pic (get-thumb pic-id)]
        (if (empty? pic)
            (ring/response "Picture not found.")
            (do
                (rotate-image pic pic-id -90)
                (ring/redirect (str "/pic/" pic-id))))))


(defn rotate-right
    [pic-id]
    (let [pic (get-thumb pic-id)]
        (if (empty? pic)
            (ring/response "Picture not found.")
            (let [img-path (str (:path pic) "/" (:filename pic))]
                (rotate-image pic pic-id 90)
                (ring/redirect (str "/pic/" pic-id))))))


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

(defn- check-name
    [x]
    (let [nn (name x)
          ss (str/starts-with? nn "tag_")]
        (println "" nn "->" ss)
        ss))

(defn- checked-tags
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
