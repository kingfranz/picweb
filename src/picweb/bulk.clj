(ns picweb.bulk
    (:require [clojure.string :as str]
              [hiccup.form :refer [check-box form-to hidden-field submit-button]]
              [hiccup.page :as page]
              [picweb.extra :refer [get-grid pagination show-filters]]
              [picweb.sheet :refer [contact-sheet grid-form]]
              [picweb.tags :refer [assoc-tag has-tags? mk-tag-str]]
              [picweb.thumbnails :refer [get-thumbs]]
              [picweb.utils :refer [fix-time]]
              [ring.util.response :as ring]))

(defn bulk-page
    [start-time* remote-addr]
    (let [num-thumbs (get-grid remote-addr)
          start-time (fix-time start-time*)
          pics (get-thumbs start-time num-thumbs)]
        (if (empty? pics)
            (page/html5 [:div "No pictures found."])
            (page/html5
                [:head
                 [:title "Bulk Tag Pictures"]
                 (page/include-css "/css/style.css")
                 (page/include-css "/css/w3.css")
                 [:meta {:http-equiv "cache-control" :content "no-cache, must-revalidate, post-check=0, pre-check=0"}]
                 [:meta {:http-equiv "cache-control" :content "max-age=0"}]
                 [:meta {:http-equiv "expires" :content "0"}]
                 [:meta {:http-equiv "expires" :content "Tue, 01 Jan 1980 1:00:00 GMT"}]
                 [:meta {:http-equiv "pragma" :content "no-cache"}]
                 ]
                [:body
                 [:h1 "Select Tag(s) and Pictures to apply to"]
                 [:h2.submit [:a {:href "/"} "Back to Contact Sheet"]]
                 (hidden-field :numOfThumbs (str num-thumbs))
                 [:table
                  [:tr
                   [:td (grid-form start-time num-thumbs "bulk")]
                   [:td (pagination start-time num-thumbs "bulk")]
                   (page/include-js "/js/script.js")
                   ]]
                 [:div
                  (form-to
                      [:post "/bulk-tag-update"]
                      (submit-button {:class "submit"} "Apply to selected Pictures")
                      (hidden-field :numOfThumbs (str num-thumbs))
                      (hidden-field :start-time (str start-time))
                      [:div (show-filters {:tag-set #{}} check-box)]
                      [:hr]
                      (contact-sheet pics
                                     (fn [thumb] (mk-tag-str (:id thumb))) ; tooltip
                                     (fn [thumb] [:span (check-box {:class "tags"} ; caption
                                                                   (str "img_" (:id thumb))) (if (has-tags? (:id thumb)) " T" "")]))
                      )]]))))

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

