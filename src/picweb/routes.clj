(ns picweb.routes
    (:require [compojure.coercions :refer [as-int]]
              [compojure.core :refer [defroutes GET POST ANY]]
              [compojure.route :as route]
              [picweb.html :refer [get-next get-rand-pic
                                   get-pic get-prev get-thumb-pic four-oh-four
                                   pic-page update-tags rotate-left rotate-right]]
              [picweb.tags :refer [edit-tags rename-tag delete-tag]]
              [picweb.sheet :refer [contact-page update-grid]]
              [picweb.filter :refer [filter-page update-filter]]
              [picweb.utils :refer [min-start fix-time]]
              [picweb.bulk :refer [bulk-page bulk-update]]))

(defroutes app-routes
           (GET "/" request
               (contact-page (fix-time min-start) (:remote-addr request)))

           (GET "/contact" [start :as {remote :remote-addr}]
               (contact-page (fix-time start) remote))

           ;-------------------------------------------------

           (GET "/pic" [pic-id :<< as-int]
               (pic-page pic-id))

           (GET "/picture" [pic-id :<< as-int]
               (get-pic pic-id))

           (GET "/thumb" [pic-id :<< as-int]
               (get-thumb-pic pic-id))

           ;-------------------------------------------------

           (GET "/bulk" [start :as {remote :remote-addr}]
               (bulk-page (fix-time start) remote))

           (GET "/bulk" request
               (bulk-page (fix-time min-start) (:remote-addr request)))

           (POST "/bulk-tag-update" request
               (bulk-update (request :params)))

           ;-------------------------------------------------

           (GET "/filter" request
               (filter-page (fix-time min-start) (request :params) (:remote-addr request)))

           (GET "/filter" [start :as {remote :remote-addr params :params}]
               (filter-page (fix-time start) params remote))

           (POST "/update-filter" request
               (update-filter (request :params)))

           ;-------------------------------------------------

           (POST "/gridupdate" request
               (update-grid request))

           ;-------------------------------------------------

           (GET "/prev" [pic-id :<< as-int]
               (get-prev pic-id))

           (GET "/next" [pic-id :<< as-int]
               (get-next pic-id))

           (GET "/rndpic" []
               (get-rand-pic))

           ;-------------------------------------------------

           (POST "/tagupdate" [pic-id :<< as-int :as request]
               (update-tags pic-id request))

           (POST "/rename-tag" [tag-id :<< as-int :as request]
               (rename-tag tag-id request))

           (GET "/edit-tags" []
               (edit-tags))

           (GET "/delete-tag" [tag-id :<< as-int]
               (delete-tag tag-id))

           ;-------------------------------------------------

           (GET "/rotate-left" [pic-id :<< as-int]
               (rotate-left pic-id))

           (GET "/rotate-right" [pic-id :<< as-int]
               (rotate-right pic-id))

           ;-------------------------------------------------

           (route/resources "/")

           (ANY "*" [] (route/not-found (four-oh-four))))
