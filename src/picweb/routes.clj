(ns picweb.routes
    (:require [ring.util.response :as ring]
                [clojure.string :as str]
              [picweb.html :refer [contact-page pic-page get-pic
                                   get-thumb-pic get-css update-tags update-grid
                                   get-prev get-next]]
              [compojure.core :refer :all]
              [compojure.coercions :refer :all]
              [clojure.pprint :as pp]))

(defn get-num
    [req k]
    (if (contains? req k)
        (Integer/parseInt (str/replace (get req k) #"[^0-9]+" ""))
        (if (and (contains? req :params) (contains? (:params req) k))
            (Integer/parseInt (str/replace (get-in req [:params k]) #"[^0-9]+" ""))
            nil)))

(defroutes app-routes
    (GET "/" request
            (contact-page 0 (:remote-addr request)))

    (GET "/offset/:num" request
            (contact-page (get-num request :uri)
                          (:remote-addr request)))

    (GET "/pic/:num" request
            (pic-page (get-num request :uri)))

    (GET "/picture/:num" request
            (get-pic (get-num request :uri)))

    (GET "/thumb/:num" request
            (get-thumb-pic (get-num request :uri)))

    (POST "/tagupdate" [pic-id :<< as-int, new & params]
             (update-tags pic-id new params))

    (POST "/gridupdate/:num" request
        (update-grid (get-num request :uri)
                     (get-num request :num_per_page)
                     (:remote-addr request)))

           (GET "/prev/:num" request
               (get-prev (get-num request :uri)))

           (GET "/next/:num" request
               (get-next (get-num request :uri)))

           (GET "/css/style.css" []
            (get-css))
    )
