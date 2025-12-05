(ns picweb.filter
    (:require [clojure.string :as str]
              [hiccup.form :refer [form-to check-box submit-button]]
              [hiccup.page :as page]
              [picweb.extra :refer [show-rating show-filters code]]
              [picweb.sheet :refer [contact-sheet]]
              [picweb.tags :refer [mk-tag-str]]
              [picweb.utils :refer [encode-data]]
              [picweb.thumbnails :refer [get-filtered-thumbs]]
              [ring.util.response :as ring]))

(defn filter-page
    [params]
    (let [selected* (code params)
          selected {:tag-set (set (get selected* :tag-set #{}))
                    :rating-set (set (get selected* :rating-set #{}))}]
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
              (submit-button {:class "submit"} "Execute!")
              [:div (show-filters selected check-box)]
              [:div (show-rating selected check-box)]
              (contact-sheet (get-filtered-thumbs selected)
                             (fn [thumb] (mk-tag-str (:id thumb))))
              )])))

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