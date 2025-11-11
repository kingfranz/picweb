(ns picweb.extra
    (:require [hiccup.page :as page]
              [hiccup.form :refer :all]
              [picweb.thumbnails :refer :all]
              [ring.util.response :as ring]))

(defonce raing-grp :ratings)
(defonce ratings ["No rating" "Delete it!" "Move it" "Not great" "Average" "Great"])

(defn r2i
    [r]
    (cond
        (or (nil? r) (= r "No rating")) 0
        (= r "Delete it!") 1
        (= r "Move it") 2
        (= r "Not great") 3
        (= r "Average") 4
        (= r "Great") 5
        :else 0))

(defn labeled-radio
    [target idx label]
    (let [checked (or (and (or (nil? target) (= target 0)) (= idx 0)) (= idx target))]
        [:label.rating-rb (radio-button :rating-grp checked label)
      (str label "    ")]))

(defn show-rating
    [rating]
    [:div {:class "form-group"}
     (label {:class "control-label"} "rating" "Rating")
     (reduce conj [:div {:class "btn-group"}]
             (map-indexed (fn [i r] (labeled-radio rating i r)) ratings))])
