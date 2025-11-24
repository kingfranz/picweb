(ns picweb.html
    (:require [clojure.java.shell :refer [sh]]
              [clojure.pprint :as pp]
              [clojure.set :as set]
              [clojure.string :as str]
              [hiccup.form :refer :all]
              [hiccup.page :as page]
              [picweb.extra :refer [show-filters show-rating]]
              [picweb.tags :refer [get-all-tags get-pic-tags disassoc-tag assoc-tag save-tag
                                 find-tag-id]]
              [picweb.thumbnails :refer [get-thumb update-thumb get-prev-thumb
                                         get-next-thumb get-grid get-all-thumb-ids
                                         update-rating find-thumb-by-date get-nth-thumb
                                         get-num-thumbs get-closest-thumb]]
              [ring.util.response :as ring])
    )

;;---------------------------------------------------------------------------

(defn- split-ext
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

(defn- tag-and-rate
    [all-tags pic-tags pic-id pic]
    (form-to
        [:post (str "/tagupdate?pic-id=" pic-id)]
        [:div.tags
         (show-filters {:tags (set pic-tags)} check-box)
         [:p]
         (hidden-field :pic-id pic-id)
         [:br]
         [:label "Add a new tag:"
          [:addr {:title "Separate multiple tags with ;"}
           (text-field {:placeholder "New tag(s)"} :new-tag)
           ]]]
        [:p]
        [:span.rating
         (show-rating {:ratings #{(:rating pic)}} radio-button)
         ]
        [:p]
        (submit-button {:class "submit"} "Update!")))

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
      [:img {:src   (str "/picture?pic-id=" pic-id)
             :alt   (:filename pic)
             :title (:filename pic)}]]])

(defn pic-page
    [pic-id]
    (try
        (let [pic (get-thumb pic-id)]
            (if (empty? pic)
                [:div "Picture not found."]
                (let [all-tags (get-all-tags)
                      pic-tags (map :tag_id (get-pic-tags pic-id))]
                    (page/html5
                        [:head
                         (page/include-css "/css/style.css")
                         (page/include-css "/css/w3.css")
                         [:meta {:http-equiv "cache-control" :content "no-cache, must-revalidate, post-check=0, pre-check=0"}]
                         [:meta {:http-equiv "cache-control" :content "max-age=0"}]
                         [:meta {:http-equiv "expires" :content "0"}]
                         [:meta {:http-equiv "expires" :content "Tue, 01 Jan 1980 1:00:00 GMT"}]
                         [:meta {:http-equiv "pragma" :content "no-cache"}]
                         [:link {:rel "icon" :href "http://ubuntupc.lan:4559/favicon.png"}]
                         [:title "Picture Details"]
                         ]
                        [:body
                         (pic-and-info pic pic-id)
                         [:div.wrapper
                          [:a {:href (str "/rotate-left?pic-id=" pic-id)} "Rotate Left   .."]
                          [:a {:href (str "/rotate-right?pic-id=" pic-id)} "..   Rotate Right"]
                          ]
                         (tag-and-rate all-tags pic-tags pic-id pic)
                         [:div.wrapper
                          [:a {:href (str "/prev?pic-id=" pic-id)} "Previous   ."]
                          [:a {:href (str "/rndpic")} ".   Random"]
                          [:a {:href (str "/next?pic-id=" pic-id)} "..   Next"]]
                         [:p]
                         [:table.footer-table
                          [:tr.footer-tr
                           [:td.footer-td [:a {:href (str "/sheetat?pic-id=" pic-id)} "Back to Contact Sheet"]]
                           [:td.footer-td [:a {:href "/edit-tags"} "Edit Tags"]]
                           [:td.footer-td "Open in GIMP"]]
                          [:tr.footer-tr
                           [:td.footer-td "PicWeb - Clojure based Photo Gallery"]
                           [:td.footer-td "Developed by Kingfranz"]
                           [:td.footer-td "2025-11-20 01:00"]]]
                         ]))))
        (catch Exception e
            (println "Error generating picture page for pic-id" pic-id ":" (.getMessage e))
            (println "Stack trace:" (with-out-str (pp/pprint (.getStackTrace e))))
            [:div "Error generating picture page."])))

;;---------------------------------------------------------------------------

(defn sheet-at
    [pic-id remote-addr]
    {:pre [(int? pic-id) (string? remote-addr)]}
    (let [num-thumbs (get-grid remote-addr)
          all-ids (get-all-thumb-ids)
          pic-idx (.indexOf all-ids pic-id)
          page-num (int (/ pic-idx num-thumbs))]
        (ring/redirect (str "/offset?offset=" (* page-num num-thumbs)))))

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
                          tn-cmd (str "convert " img-path " -auto-orient -thumbnail 150x150^ -gravity center -extent 150x150 " tn-file)
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
                (ring/redirect (str "/pic?pic-id=" pic-id))))))


(defn rotate-right
    [pic-id]
    (let [pic (get-thumb pic-id)]
        (if (empty? pic)
            (ring/response "Picture not found.")
            (do
                (rotate-image pic pic-id 90)
                (ring/redirect (str "/pic?pic-id=" pic-id))))))


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

;;---------------------------------------------------------------------------

(defn extract-rating
    [params]
    (->> params
         (keys)
         (map name)
         (filter #(str/starts-with? % "rating-"))
         (map #(subs % 7))
         (map #(Integer/parseInt %))
         (filter #(<= 1 % 5))
         (set)))

(defn- check-name
    [x]
    (let [nn (name x)
          ss (and (str/starts-with? nn "tag_") (not= nn "tag_"))]
        ss))

(defn- checked-tags
    [params]
    (->> params
         (keys)
         (map name)
         (filter #(str/starts-with? % "tag_"))
         (map #(subs % 4))
         (map #(Integer/parseInt %))
         (remove nil?)
         (set)))

(def allowed-str "abcdefghijklmnopqrstuvwxyzåäö0123456789- ")
(def lc-allowed-chars (set (seq allowed-str)))
(def allowed-chars (set/union lc-allowed-chars (str/upper-case allowed-str)))

(defn update-tags
    [pic-id request]
    (try
        (let [params (get request :params {})
              new-tag (get params :new-tag "")
              rating* (extract-rating params)
              rating (if (empty? rating*) nil (first rating*))
              tag-ids (checked-tags params)
              pic-tag-ids (set (map :tag_id (get-pic-tags pic-id)))
              aaa (if (str/blank? new-tag)
                      []
                      (->> (str/split new-tag #";")
                           (map str/trim)
                           (remove str/blank?)
                           (remove #(< (count %) 2))
                           (filter #(every? (fn [s] (contains? allowed-chars s)) (seq %)))
                           (filter #(nil? (find-tag-id %)))))
              ; Remove tags that are not in the current pic
              to-remove (set/difference pic-tag-ids tag-ids)
              to-add (set/difference tag-ids pic-tag-ids)]
            ; check existing tags
            (doseq [tag-id to-remove]
                (disassoc-tag pic-id tag-id))
            (doseq [tag-id to-add]
                (assoc-tag pic-id tag-id))
            ; check new tag
            (doseq [tag-name aaa]
                (when-not (save-tag pic-id tag-name)
                    (throw (Exception. (str "Failed to save new tag: " tag-name)))))
            ; check rating
            (when (and rating (int? rating))
                (if (and (>= rating 1) (<= rating 5))
                    (update-rating pic-id rating)
                    (throw (Exception. "Invalid rating value."))))
            (ring/redirect (str "/pic?pic-id=" pic-id)))
        (catch Exception e
            (println "Error updating tags:" (.getMessage e))
            (println (str "Parameters: " pic-id " " request))
            (println "Stack trace:" (with-out-str (pp/pprint (.getStackTrace e))))
            (ring/response "Error updating tags.")
            )
        ))

;;---------------------------------------------------------------------------

(defn four-oh-four
    []
    (page/html5
        [:head
         [:title "404 Not Found"]
         (page/include-css "/css/style.css")
         (page/include-css "/css/w3.css")]
        [:body
         [:h1 "404 Not Found"]
         [:p "The page you are looking for does not exist."]]))

;;---------------------------------------------------------------------------

(defn get-prev
    [pic-id]
    (let [prev (get-prev-thumb pic-id)]
        (if (empty? prev)
            (ring/response "No previous picture.")
            (ring/redirect (str "/pic?pic-id=" (:id prev))))))

(defn get-next
    [pic-id]
    (let [next (get-next-thumb pic-id)]
        (if (empty? next)
            (ring/response "No next picture.")
            (ring/redirect (str "/pic?pic-id=" (:id next))))))

(defn get-rand-pic
    []
    (let [num (get-num-thumbs)
          target (rand-int num)
          thumb (get-nth-thumb target)]
        (if (empty? thumb)
            (ring/response "No random picture found.")
            (ring/redirect (str "/pic?pic-id=" (:id thumb))))))

;(defn find-date
;    [target remote-addr]
;    {:pre [(int? target) (< 19700101 target 20251231)]
;     :post [(or (nil? %) (map? %))]}
;    (if-let [found (find-thumb-by-date target)]
;        (let [all-ids (get-all-thumb-ids)
;              pic-idx (.indexOf all-ids (:id found))
;              num-thumbs (get-grid remote-addr)
;              page-num (int (/ pic-idx num-thumbs))]
;            (ring/redirect (str "/offset?offset=" (* page-num num-thumbs))))
;        (ring/response (str "No picture found for date: " target))))
;
;(defn find-number
;    [num remote-addr]
;    {:pre [(int? num) (< 500 num 40000)]
;     :post [(or (nil? %) (map? %))]}
;    (let [t (get-closest-thumb num)]
;        (if (empty? t)
;            (ring/response (str "No picture found for number: " num))
;            (find-date (thumb2int t) remote-addr))))
