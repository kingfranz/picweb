(ns picweb.filter
    (:require [clojure.string :as str]
              [hiccup.form :refer :all]
              [hiccup.page :as page]
              [picweb.extra :refer [show-rating show-filters code encode-data]]
              [picweb.sheet :refer [contact-sheet]]
              [picweb.tags :refer :all]
              [picweb.thumbnails :refer :all]
              [ring.util.response :as ring]))

(defn filter-page
    [params]
    (let [selected (code params)]
        (page/html5
         [:head
          [:title "Filter Pictures"]
          (page/include-css "/css/style.css")
          (page/include-css "/css/w3.css")]
         [:body
          [:h1 "Filter Pictures"]
          [:h2.submit [:a {:href "/"} "Back to Contact Sheet"]]
          (form-to
              [:post "/update-filter"]
              [:div (show-filters selected check-box)]
              [:div (show-rating selected check-box)]
              (contact-sheet (get-filtered-thumbs selected)
                             (fn [thumb] (mk-tag-str (:id thumb))))
              (submit-button {:class "submit"} "Execute!")
              )])))

(defn update-filter
    [params]
    (let [selected-tags (->> (keys params)
                             (filter #(str/starts-with? (name %) "tag_"))
                             (map #(parse-long (subs (name %) 4))))
          selected-ratings (->> (keys params)
                                (filter #(str/starts-with? (name %) "rating-"))
                                (map #(parse-long (subs (name %) 7))))
          selected {:tags (set selected-tags)
                    :ratings (set selected-ratings)}
          encoded-data (encode-data (pr-str selected))]
        (ring/redirect (str "/filter?filter=" encoded-data))))