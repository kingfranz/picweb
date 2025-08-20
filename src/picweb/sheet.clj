(ns picweb.sheet
    (:require [clojure.pprint :as pp]
              [clojure.set :as set]
              [clojure.string :as str]
              [hiccup.form :as hf]
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

(defn contact-page
    [offset remote-addr]
    (let [num-thumbs (get-grid remote-addr)
          pics (get-thumbs offset num-thumbs)]
        (if (empty? pics)
            [:div "No pictures found."]
            (page/html5
                [:head
                 [:title "Contact Sheet"]
                 [:link {:rel "stylesheet" :href "/css/style.css"}]]
                [:body
                 [:h1 "Contact Sheet"]
                 (hf/form-to
                     [:post (str "/gridupdate/" offset)]
                     [:span
                      [:label "Pictures per page: "
                       (hf/text-field {:size 3 :maxlength 3} :num_per_page num-thumbs)]
                      (hf/submit-button {:class "submit"} "Update")])
                 (contact-sheet pics)
                 [:div.pagination1
                  (when (> offset 0)
                      (let [pre-pages (int (/ offset num-thumbs))]
                          (if (> pre-pages 10)
                              [:span.pagination
                               (for [i (range 0 3)]
                                   [:a.pagination {:href (str "/offset/" (* i num-thumbs))} (str " " i " ")])
                               [:span.pagination "..."]
                               (for [i (range (- pre-pages 3) pre-pages)]
                                   [:a.pagination {:href (str "/offset/" (* i num-thumbs))} (str " " i " ")])]
                              (for [i (range 0 pre-pages)]
                                  [:a.pagination {:href (str "/offset/" (* i num-thumbs))}
                                   (str " " i " ")]))))
                  [:span.pagination " This page "]
                  (let [post-pages (int (/ (- (get-num-thumbs) offset) num-thumbs))]
                      (if (> post-pages 10)
                          [:span.pagination
                           (for [i (range 1 4)]
                               [:a.pagination {:href (str "/offset/" (+ offset (* i num-thumbs)))}
                                (str " " (+ (int (/ offset num-thumbs)) i) " ")])
                           [:span.pagination " ... "]
                           (for [i (range (- post-pages 3) post-pages)]
                               [:a.pagination {:href (str "/offset/" (+ offset (* i num-thumbs)))}
                                (str " " (+ (int (/ offset num-thumbs)) i) " ")])]
                          (for [i (range 0 post-pages)]
                              [:a.pagination {:href (str "/offset/" (+ offset (* i num-thumbs)))}
                               (str " " (+ (int (/ offset num-thumbs)) i) " ")])))]]))))

;;---------------------------------------------------------------------------

(defn update-grid
    [offset num-pics remote-addr]
    (if (some? num-pics)
        (do
            (save-grid remote-addr num-pics)
            (ring/redirect (str "/offset/" offset) 303))
        (ring/response "Invalid parameters.")))

;;---------------------------------------------------------------------------
