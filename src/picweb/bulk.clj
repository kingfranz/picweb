(ns picweb.bulk
    (:require [clojure.string :as str]
              [hiccup.form :refer [form-to check-box hidden-field submit-button]]
              [hiccup.page :as page]
              [picweb.sheet :refer [contact-sheet grid-form]]
              [picweb.tags :refer [mk-tag-str has-tags? assoc-tag]]
              [picweb.thumbnails :refer [get-grid get-thumbs]]
              [picweb.extra :refer [show-filters pagination]]
              [ring.util.response :as ring]))

(defn bulk-page
    [start-time remote-addr]
    (let [num-thumbs (get-grid remote-addr)
          pics (get-thumbs start-time num-thumbs)]
        (if (empty? pics)
            [:div "No pictures found."]
            (page/html5
                [:head
                 [:title "Bulk Tag Pictures"]
                 (page/include-css "/css/style.css")
                 (page/include-css "/css/w3.css")]
                [:body
                 [:h1 "Select Tag(s) and Pictures to apply to"]
                 [:h2.submit [:a {:href "/"} "Back to Contact Sheet"]]
                 (hidden-field :numOfThumbs (str num-thumbs))
                 [:table
                  [:tr
                   [:td (grid-form start-time num-thumbs "bulk")]
                   [:td (pagination start-time num-thumbs "bulk")]
                   ]]
                 [:script {:type "application/javascript" :src "/js/script.js"}]
                 [:div
                  (form-to
                      [:post "/bulk-tag-update"]
                      (hidden-field :numOfThumbs (str num-thumbs))
                      (hidden-field :start-time (str start-time))
                      [:div (show-filters {:tags #{}} check-box)]
                      [:hr]
                      (contact-sheet pics
                                     (fn [thumb] (mk-tag-str (:id thumb))) ; tooltip
                                     (fn [thumb] [:span (check-box {:class "tags"} ; caption
                                                                      (str "img_" (:id thumb))) (if (has-tags? (:id thumb)) " T" "")]))
                      (submit-button {:class "submit"} "Apply to selected Pictures")
                      )]
                 ]))))

(defn bulk-update
    [params]
    (let [selected-tags (->> params
                             (keys)
                             (filter #(str/starts-with? (name %) "tag_"))
                             (map #(parse-long (subs (name %) 4))))
          selected-imgs (->> params
                             (keys)
                             (filter #(str/starts-with? (name %) "img_"))
                             (map #(parse-long (subs (name %) 4))))]
        (doseq [img-id selected-imgs
                tag-id selected-tags]
            (assoc-tag img-id tag-id))
        (ring/redirect (str "/bulk?start=" (get params :start-time 0)))))

