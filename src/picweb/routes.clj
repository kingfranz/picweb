(ns picweb.routes
    (:require [ring.util.response :as ring]
              [picweb.html :refer [contact-page pic-page get-pic get-thumb-pic get-css update-tags update-cookie]]
              [compojure.core :refer :all]
              [clojure.pprint :as pp]))


(defroutes app-routes
    (GET "/" {cookies :cookie}
            (contact-page cookies 0))

    (GET "/offset/:num" {cookies :cookie} [num]
            (contact-page cookies (Integer/parseInt num)))

    (GET "/pic/:num" {cookies :cookie} [num]
            (pic-page cookies (Integer/parseInt num)))

    (GET "/picture/:num" {cookies :cookie} [num]
            (get-pic cookies (Integer/parseInt num)))

    (GET "/thumb/:num" {cookies :cookie} [num]
            (get-thumb-pic cookies (Integer/parseInt num)))

    (POST "/tagupdate" {{:cookie cookies :pic-id pic-id :new-tag new-tag} :params} :as params
             (update-tags cookies (Integer/parseInt pic-id) new-tag params))

    (POST "/cookieupd/:num" {{:num num :cookie cookies :col col :rows rows} :params}
             (update-cookie cookies (Integer/parseInt num) (Integer/parseInt col) (Integer/parseInt rows)))

    (GET "/css/style.css" []
            (get-css))
    )
