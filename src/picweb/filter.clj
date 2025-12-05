(ns picweb.filter
    (:require [clojure.string :as str]
              [hiccup.form :refer [check-box form-to hidden-field submit-button]]
              [hiccup.page :as page]
              [picweb.extra :refer [get-grid show-rating pagination show-filters code]]
              [picweb.sheet :refer [contact-sheet grid-form]]
              [picweb.tags :refer [mk-tag-str]]
              [picweb.utils :refer [encode-data]]
              [picweb.thumbnails :refer [get-filtered-thumbs]]
              [ring.util.response :as ring]))

(defn filter-page
    [start-time params remote-addr]
    (let [num-thumbs (get-grid remote-addr)
          selected* (code params)
          selected {:tag-set (set (get selected* :tag-set #{}))
                    :rating-set (set (get selected* :rating-set #{}))}]
        (page/html5
         [:head
          [:title "Filter Pictures"]
          (page/include-css "/css/style.css")
          (page/include-css "/css/w3.css")
          [:meta {:http-equiv "cache-control" :content "no-cache, must-revalidate, post-check=0, pre-check=0"}]
          [:meta {:http-equiv "cache-control" :content "max-age=0"}]
          [:meta {:http-equiv "expires" :content "0"}]
          [:meta {:http-equiv "expires" :content "Tue, 01 Jan 1980 1:00:00 GMT"}]
          [:meta {:http-equiv "pragma" :content "no-cache"}]
          ]
         [:body
          [:h1 "Filter Pictures"]
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
              [:post "/update-filter"]
              (submit-button {:class "submit"} "Execute!")
              [:div (show-filters selected check-box)]
              [:div (show-rating selected check-box)]
              [:hr]
              (contact-sheet (get-filtered-thumbs selected)
                             (fn [thumb] (mk-tag-str (:id thumb))))
              )]])))

(defn update-filter
    [params]
    (let [selected-tags (->> (keys params)
                             (filter #(str/starts-with? (name %) "tag_"))
                             (map #(parse-long (subs (name %) 4))))
          selected-ratings (->> (keys params)
                                (filter #(str/starts-with? (name %) "rating-"))
                                (map #(parse-long (subs (name %) 7))))
          selected {:tag-set (set selected-tags)
                    :rating-set (set selected-ratings)}
          encoded-data (encode-data (pr-str selected))]
        (ring/redirect (str "/filter?filter=" encoded-data))))