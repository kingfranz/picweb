(ns picweb.routes
    (:require [clojure.string :as str]
              [compojure.coercions :refer :all]
              [compojure.core :refer :all]
              [compojure.route :as route]
              [picweb.html :refer [get-next sheet-at get-rand-pic
                                   get-pic get-prev get-thumb-pic find-date four-oh-four
                                   pic-page update-tags rotate-left rotate-right]]
              [picweb.tags :refer [edit-tags rename-tag delete-tag]]
              [picweb.sheet :refer [contact-page update-grid]]
              [picweb.filter :refer [filter-page update-filter]]))

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

           (GET "/sheetat/:num" request
               (sheet-at (get-num request :uri)
                             (:remote-addr request)))

           (GET "/pic/:num" request
               (pic-page (get-num request :uri)))

           (GET "/picture/:num" request
               (get-pic (get-num request :uri)))

           (GET "/thumb/:num" request
               (get-thumb-pic (get-num request :uri)))

           (GET "/filter" request
               (filter-page (request :params)))

           (POST "/update-filter" request
               (update-filter (request :params)))

           (POST "/tagupdate/:num" request
               ;(println pic-id new-tag rating params)
               (update-tags (get-num request :uri) request))

           (POST "/gridupdate/:num" request
               (update-grid (get-num request :uri)
                            (get-num request :num_per_page)
                            (:remote-addr request)))

           (POST "/rename-tag/:tag-id" request
               (rename-tag (get-num request :uri) request))

           (GET "/prev/:num" request
               (get-prev (get-num request :uri)))

           (GET "/next/:num" request
               (get-next (get-num request :uri)))

           (GET "/rndpic" request
               (get-rand-pic))

           (GET "/edit-tags" request
               (edit-tags))

           (GET "/delete-tag/:num" request
               (delete-tag (get-num request :uri)))

           (GET "/rotate-left/:num" request
               (rotate-left (get-num request :uri)))

           (GET "/rotate-right/:num" request
               (rotate-right (get-num request :uri)))

           (GET "/finddate/:num" request
               (find-date (get-num request :uri)
                          (:remote-addr request)))

           ;(GET "/css/style.css" []
           ;    (get-css))
           ;
           ;(GET "/css/w3.css" []
           ;    (get-css2))
           ;
           ;(GET "/script.js" []
           ;    (get-script))

           (route/resources "/")
           (route/not-found
               (four-oh-four)))
