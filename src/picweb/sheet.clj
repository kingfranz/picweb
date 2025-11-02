(ns picweb.sheet
    (:require [hiccup.form :as hf]
              [hiccup.page :as page]
              [picweb.thumbnails :refer :all]
              [ring.util.response :as ring]))


(defn contact-sheet
    [thumbs]
    [:div.contact-sheet
     (for [thumb thumbs]
         [:figure.contact-image
          [:a {:href (str "/pic/" (:id thumb))} [:img {:src (str "/thumb/" (:id thumb))}]]
          [:figcaption.contact-text (subs (:timestr thumb) 0 10)]
          ])])

(defn clamp
    [n min-val max-val]
    (cond
        (< n min-val) min-val
        (> n max-val) max-val
        :else n))

(defn- grid-form
    [offset num-thumbs]
    (hf/form-to
        [:post (str "/gridupdate/" offset)]
        [:span
         [:label "Pictures per page: "
          (hf/text-field {:size 3 :maxlength 3} :num_per_page num-thumbs)]
         (hf/submit-button {:class "submit"} "Update")]))

(defn- pagination
    [offset num-thumbs]
    (let [current (int (/ offset num-thumbs))]
        [:div.pagination1
         (when (> offset 0)
             [:span.pagination1
              [:a.pagination {:href (str "/offset/" (dec current))} (str " " (dec current) " ")]])

         [:span.pagination1
          [:abbr {:title (str "Enter a value < 500 for pagenunber\n"
                  "Between 500 and 40000 for imagenumber\n"
                  "Between 19700101 and 20251231 to search by date")}
          [:input.pagination1 {:type "text" :id "current" :value current}]]
          [:div {:id "message"}]
          [:button {:type "button" :id "button" :hidden true} "Go"]]

         (let [post-pages (- num-thumbs current)]
             (when (> post-pages 0)
                 [:span.pagination1
                  [:a.pagination {:href (str "/offset/" (inc current))} (str " " (inc current) " ")]]))]))

(defn contact-page
    [offset remote-addr]
    (let [num-thumbs* (get-grid remote-addr)
          num-thumbs (if (nil? num-thumbs*) 25 num-thumbs*)
          pics (get-thumbs offset num-thumbs)]
        (if (empty? pics)
            [:div "No pictures found."]
            (page/html5
                [:head
                 [:link {:rel "stylesheet" :href "/css/style.css?id=1234"}]
                 [:link {:rel "stylesheet" :href "/css/w3.css"}]
                 [:title "Contact Sheet"]
                 ]
                [:body
                 [:h1 "Contact Sheet"]
                 (hf/text-field {:type "hidden" :id "numOfThumbs" :value (str num-thumbs)} (str num-thumbs))
                 (grid-form offset num-thumbs)
                 (contact-sheet pics)
                 (pagination offset num-thumbs)
                 [:script {:type "application/javascript" :src "/js/script.js"}]]))))

;;---------------------------------------------------------------------------

(defn update-grid
    [offset num-pics remote-addr]
    (if (some? num-pics)
        (do
            (save-grid remote-addr num-pics)
            (ring/redirect (str "/offset/" offset) 303))
        (ring/response "Invalid parameters.")))

;;---------------------------------------------------------------------------
