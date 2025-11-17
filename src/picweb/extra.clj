(ns picweb.extra
    (:require [hiccup.form :refer :all]
              [picweb.tags :refer :all]
              [picweb.thumbnails :refer :all]))

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
