(ns picweb.extra
    (:require [hiccup.form :refer :all]
              [picweb.tags :refer :all]
              ;[picweb.thumbnails :refer [get-num-thumbs]]
              ))

;;------------------------------------------------------------

(defn encode-data [data]
    (.encodeToString (java.util.Base64/getEncoder) (.getBytes data)))

(defn decode-data [encoded-data]
    (String. (.decode (java.util.Base64/getDecoder) encoded-data)))

(defn show-filters
    [selected elem]
    [:div.tags
     (let [all (sort-by :name (get-all-tags))]
         (for [tag all]
             [:span.tags
              [:label.tags (:name tag)
               (elem {:class "tags"}
                     (str "tag_" (:tag_id tag))
                     (some #(= (:tag_id tag) %) (get selected :tags #{})))]]))
     ])

(defn code
    [s]
    (let [encoded (get s :filter "")
          decoded (if (not= encoded "") (decode-data encoded) "[]")]
        (try
            (read-string decoded)
            (catch Exception e
                (println "Error decoding filter data:" (.getMessage e))
                []))))

(defonce ratings ["No rating" "Delete it!" "Move it" "Not great" "Average" "Great"])

(defn r2i
    [r]
    (cond
        (or (nil? r) (= r "No rating")) 0
        (= r "Delete it!") 1
        (= r "Move it") 2
        (= r "Not great") 3
        (= r "Average") 4
        (= r "Great") 5
        :else 0))

;;------------------------------------------------------------

(defn labeled-elem
    [elem active-elems idx label]
    [:label.rating-lbl (elem {:class "rating-cb"}
                             (str "rating-" idx)
                             (contains? active-elems idx)
                             label)
     (str label "    ")])

(defn show-rating
    [selected elem]
    [:div {:class "form-group"}
     (label {:class "control-label"} "rating" "Rating")
     (reduce conj [:div {:class "btn-group"}]
             (map-indexed (fn [i r] (labeled-elem elem (get selected :ratings #{}) i r)) ratings))])

(defn pagination
    [offset num-thumbs-per-page owner*]
    (let [current-page (int (/ offset num-thumbs-per-page))
          prev-page (if (>= current-page 1) (dec current-page) 0)
          prev-offset (if (> offset num-thumbs-per-page) (- offset num-thumbs-per-page) 0)
          owner owner*]
        [:div
         (when (> offset 0)
             [:span.pagination1
              [:a.pagination {:href (str "/" owner "?offset=" prev-offset)} (str " " prev-page " ")]])

         ;(hidden-field :owner owner)
         [:span.pagination1
          [:input.pagination1 {:type "text" :id "current-page" :value current-page}]
          [:div {:id "message"}]
          [:button {:type "button" :id "button" :hidden true} "Go"]]

         (let [total-num-thumbs (picweb.thumbnails/get-num-thumbs)
               post-pages (int (/ (- total-num-thumbs (* (inc current-page) num-thumbs-per-page)) num-thumbs-per-page))]
             (when (> post-pages 0)
                 [:span.pagination1
                  [:a.pagination {:href (str "/" owner "?offset=" (+ offset num-thumbs-per-page))} (str " " (inc current-page) " ")]]))
         [:div.help (str "Enter a value < 500 for pagenunber\n"
                         "Between 500 and 40000 for imagenumber\n"
                         "Between 19700101 and 20251231 to search by date")]]))

;(defn thumb2int
;    [thumb]
;    (Integer/parseInt (str (subs (:timestr thumb) 0 4)
;                           (subs (:timestr thumb) 5 7)
;                           (subs (:timestr thumb) 8 10))))
;
;(defn int2thumb
;    [num]
;    (let [s (format "%08d" num)
;          timestr (str (subs s 0 4) "-" (subs s 4 6) "-" (subs s 6 8) "T00:00:00")]
;        timestr))
;
