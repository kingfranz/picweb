(ns picweb.extra
    (:require [hiccup.form :refer [label]]
              [next.jdbc.sql :as sql]
              [picweb.tags :refer [get-all-tags]]
              [picweb.thumbnails :refer [get-page-down get-page-up]]
              [picweb.utils :refer [decode-data ds-opts]]
              [ring.util.response :as ring]))

;;------------------------------------------------------------

(defn show-filters
    [selected elem]
    [:div.tags
     (for [tag (sort-by :name (get-all-tags))]
         [:span.tags
          [:label.tags (:name tag)
           (elem {:class "tags"}
                 (str "tag_" (:tag_id tag))
                 (some #(= (:tag_id tag) %) (get selected :tag-set #{})))]])
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
             (map-indexed (fn [i r] (labeled-elem elem (get selected :rating-set #{}) i r)) ratings))])

(defn pagination
    [start-time num-thumbs-per-page owner*]
    (let [prev-page (get-page-up start-time num-thumbs-per-page)
          next-page (get-page-down start-time num-thumbs-per-page)
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


(defn get-grid-raw
    [remote-addr]
    {:pre  [(string? remote-addr)]
     :post [(or (int? %) (nil? %))]}
    (let [res (sql/query ds-opts ["SELECT num_per_page FROM grid WHERE id = ?" remote-addr])]
        (if (empty? res)
            nil                                             ; default grid
            (:num_per_page (first res)))))

(defn get-grid
    [remote-addr]
    {:pre  [(string? remote-addr)]
     :post [(or (int? %) (nil? %))]}
    (let [g (get-grid-raw remote-addr)]
        (if (nil? g)
            25
            g)))

(defn save-grid
    [remote-addr num-pics]
    {:pre [(string? remote-addr) (integer? num-pics)]}
    (let [num (get-grid-raw remote-addr)]
        (if (nil? num)
            (sql/insert! ds-opts :grid {:id remote-addr, :num_per_page num-pics})
            (sql/update! ds-opts :grid {:num_per_page num-pics} {:id remote-addr}))
        (ring/redirect "/")))
