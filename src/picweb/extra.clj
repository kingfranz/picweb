(ns picweb.extra
    (:require [hiccup.form :refer [label]]
              [picweb.tags :refer [get-all-tags]]
              [picweb.utils :refer [decode-data get-newer-thumb get-older-thumb]]
              ))

;;------------------------------------------------------------

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
    [start-time num-thumbs-per-page owner*]
    (let [prev-page (get-older-thumb start-time num-thumbs-per-page)
          next-page (get-newer-thumb start-time num-thumbs-per-page)
          owner owner*]
        [:div
             [:span.pagination1
              [:a.pagination {:href (str "/" owner "?start=" prev-page)} "<"]]

         [:span.pagination1
          [:input.pagination1 {:type "text" :id "current-page" :value start-time}]
          [:div {:id "message"}]
          [:button {:type "button" :id "button" :hidden true} "Go"]]

                 [:span.pagination1
                  [:a.pagination {:href (str "/" owner "?start=" next-page)} ">"]]
         [:div.help "Between 19700101 and 20251231 to search by date"]]))

