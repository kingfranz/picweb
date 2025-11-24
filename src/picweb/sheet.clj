(ns picweb.sheet
    (:require [hiccup.form :refer :all]
              [hiccup.page :as page]
              [picweb.tags :refer [mk-tag-str has-tags?]]
              [picweb.thumbnails :refer [get-grid get-thumbs save-grid]]
              [picweb.extra :refer [pagination]]
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
           [:a {:href (str "/pic?pic-id=" (:id thumb))} [:img {:src (str "/thumb?pic-id=" (:id thumb))}]]]
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

(defn- thumb2int
    [thumb]
    (Integer/parseInt (str (subs (:timestr thumb) 0 4)
                           (subs (:timestr thumb) 5 7)
                           (subs (:timestr thumb) 8 10))))

(defn contact-page
    [value value-type remote-addr]
    (let [num-thumbs (get-grid remote-addr)
          pics (get-thumbs value value-type num-thumbs)
          offset (thumb2int (first pics))]
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
                 [:table
                  [:tr
                   [:td (grid-form offset num-thumbs "offset")]
                   [:td (pagination offset num-thumbs "offset")]
                   ]]

                 [:script {:type "application/javascript" :src "/js/script.js"}]
                 [:div.wrapper
                 [:a {:href "/filter"} "Filter"]
                  ]
                 [:div.wrapper
                  [:a {:href "/bulk"} "Bulk tagging"]
                  ]
                 (contact-sheet pics (fn [thumb] (mk-tag-str (:id thumb))))
                 ]))))

;;---------------------------------------------------------------------------

(defn update-grid
    [offset num-pics remote-addr owner]
    (if (and (some? num-pics) (integer? num-pics) (> num-pics 10) (<= num-pics 500))
        (do
            (save-grid remote-addr num-pics)
            (ring/redirect (str "/" owner "&offset=" offset)))
        (ring/response "Invalid parameters.")))

;;---------------------------------------------------------------------------
