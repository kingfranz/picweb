(ns picweb.bulk
    (:require [clojure.string :as str]
              [hiccup.form :refer :all]
              [hiccup.page :as page]
              [picweb.sheet :refer [contact-sheet grid-form]]
              [picweb.tags :refer [mk-tag-str has-tags? assoc-tag]]
              [picweb.thumbnails :refer [get-grid get-thumbs]]
              [picweb.extra :refer [show-filters pagination]]
              [ring.util.response :as ring]))

(defn bulk-page
    [offset remote-addr]
    (let [num-thumbs (get-grid remote-addr)
          pics (get-thumbs offset :offset num-thumbs)]
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
                   [:td (grid-form offset num-thumbs "bulk")]
                   [:td (pagination offset num-thumbs "bulk")]
                   ]]
                 [:script {:type "application/javascript" :src "/js/script.js"}]
                 [:div
                  (form-to
                      [:post "/bulk-tag-update"]
                      (hidden-field :numOfThumbs (str num-thumbs))
                      (hidden-field :offset (str offset))
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
    (let [st (filter #(str/starts-with? (name %) "tag_") (keys params))
          selected-tags (map #(parse-long (subs (name %) 4)) st)
          si (filter #(str/starts-with? (name %) "img_") (keys params))
          selected-imgs (map #(parse-long (subs (name %) 4)) si)]
        (doseq [img-id selected-imgs
                tag-id selected-tags]
            (assoc-tag img-id tag-id))
        (ring/redirect (str "/bulk?offset=" (get params :offset 0)))))

