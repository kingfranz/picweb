(ns picweb.sheet
    (:require [hiccup.form :refer :all]
              [hiccup.page :as page]
              [picweb.tags :refer :all]
              [picweb.thumbnails :refer :all]
              [ring.util.response :as ring]))


(defn contact-sheet
    ([thumbs tool-tip]
     (contact-sheet thumbs tool-tip (fn [thumb] (str (subs (:timestr thumb) 0 10)
                                            (if (has-tags? (:id thumb)) " T" "")
                                            (if (:rating thumb) " R" "")))))
    ([thumbs tool-tip caption]
    [:div.contact-sheet
     (for [thumb thumbs
           :let [tt (tool-tip thumb)]]
         [:figure.contact-image
          [:addr {:title tt}
           [:a {:href (str "/pic/" (:id thumb))} [:img {:src (str "/thumb/" (:id thumb))}]]]
          [:figcaption.contact-text (caption thumb)]
          ])]))

(defn grid-form
    [offset num-thumbs owner]
    (form-to
        [:post "/gridupdate"]
        (hidden-field :offset (str offset))
        (hidden-field :owner owner)
        [:span
         [:label.simple "Pictures per page: "
          (text-field {:size 3 :maxlength 3} :num_per_page num-thumbs)]
         (submit-button {:class "submit"} "Update")]))

(defn- pagination
    [offset num-thumbs-per-page]
    (let [current-page (int (/ offset num-thumbs-per-page))
          prev-page (if (>= current-page 1) (dec current-page) 0)
          prev-offset (if (> offset num-thumbs-per-page) (- offset num-thumbs-per-page) 0)]
        [:div
         (when (> offset 0)
             [:span.pagination1
              [:a.pagination {:href (str "/offset/" prev-offset)} (str " " prev-page " ")]])

         [:span.pagination1
          [:input.pagination1 {:type "text" :id "current-page" :value current-page}]
          [:div {:id "message"}]
          [:button {:type "button" :id "button" :hidden true} "Go"]]

         (let [total-num-thumbs (get-num-thumbs)
               post-pages (int (/ (- total-num-thumbs (* (inc current-page) num-thumbs-per-page)) num-thumbs-per-page))]
             (when (> post-pages 0)
                 [:span.pagination1
                  [:a.pagination {:href (str "/offset/" (+ offset num-thumbs-per-page))} (str " " (inc current-page) " ")]]))
         [:div.help (str "Enter a value < 500 for pagenunber\n"
                         "Between 500 and 40000 for imagenumber\n"
                         "Between 19700101 and 20251231 to search by date")]]))

(defn contact-page
    [offset remote-addr]
    (let [num-thumbs* (get-grid remote-addr)
          num-thumbs (if (nil? num-thumbs*) 25 num-thumbs*)
          pics (get-thumbs offset num-thumbs)]
        (if (empty? pics)
            [:div "No pictures found."]
            (page/html5
                [:head
                 (page/include-css "/css/style.css")
                 (page/include-css "/css/w3.css")
                 [:meta {:http-equiv "cache-control" :content "no-cache, must-revalidate, post-check=0, pre-check=0"}]
                 [:meta {:http-equiv "cache-control" :content "max-age=0"}]
                 [:meta {:http-equiv "expires" :content "0"}]
                 [:meta {:http-equiv "expires" :content "Tue, 01 Jan 1980 1:00:00 GMT"}]
                 [:meta {:http-equiv "pragma" :content "no-cache"}]
                 [:title "Contact Sheet"]
                 ]
                [:body
                 [:h1 "Contact Sheet"]
                 (hidden-field :numOfThumbs (str num-thumbs))
                 (grid-form offset num-thumbs "offset")
                 [:div.wrapper
                 [:a {:href "/filter"} "Filter"]
                  ]
                 [:div.wrapper
                  [:a {:href "/bulk"} "Bulk tagging"]
                  ]
                 (contact-sheet pics (fn [thumb] (mk-tag-str (:id thumb))))
                 (pagination offset num-thumbs)
                 [:script {:type "application/javascript" :src "/js/script.js"}]]))))

;;---------------------------------------------------------------------------

(defn update-grid
    [offset num-pics remote-addr owner]
    (if (and (some? num-pics) (integer? num-pics) (> num-pics 10) (<= num-pics 500))
        (do
            (save-grid remote-addr num-pics)
            (ring/redirect (str "/" owner "&offset=" offset) 303))
        (ring/response "Invalid parameters.")))

;;---------------------------------------------------------------------------
