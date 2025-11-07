(ns picweb.filter
    (:require [hiccup.form :as hf]
              [hiccup.page :as page]
              [picweb.sheet :refer [contact-sheet]]
              [picweb.tags :refer :all]
              [picweb.thumbnails :refer :all]
              [ring.util.response :as ring]))

(defn encode-data [data]
    (.encodeToString (java.util.Base64/getEncoder) (.getBytes data)))

(defn decode-data [encoded-data]
    (String. (.decode (java.util.Base64/getDecoder) encoded-data)))

(defn- show-filters
    [checked]
    (hf/form-to
        [:post "/update-filter"]
        [:div.tags
         (let [all (sort-by :name (get-all-tags))]
             (for [tag all]
                 [:span.tags
                  [:label.tags (:name tag)
                   (hf/check-box {:class "tags"}
                                 (str "tag_" (:tag_id tag))
                                 (some #(= (:tag_id tag) %) checked))]]))
         (hf/submit-button {:class "submit"} "Execute!")
         ]))

(defn code
    [s]
    (let [encoded (get s :filter "")
          decoded (if (not= encoded "") (decode-data encoded) "[]")]
          (try
              (read-string decoded)
              (catch Exception e
                  (println "Error decoding filter data:" (.getMessage e))
                  []))))

(defn filter-page
    [params]
    (page/html5
        [:head
         [:title "Filter Pictures"]
         (page/include-css "/css/style.css")
         (page/include-css "/css/w3.css")]
        [:body
         [:h1 "Filter Pictures"]
         [:h2.submit [:a {:href "/"} "Back to Contact Sheet"]]
         [:div
              (show-filters (code params))]
              (contact-sheet (if (empty? (code params))
                                 (get-thumbs 0 100)
                                 (get-thumbs-by-tags (code params))))]))

(defn update-filter
    [params]
    (let [selected-tags (for [tag (get-all-tags)
                              :let [param-key (str "tag_" (:tag_id tag))]
                              :when (contains? params (keyword param-key))]
                            (:tag_id tag))
          encoded-data (encode-data (pr-str selected-tags))]
        (ring/redirect (str "/filter?filter=" encoded-data))))