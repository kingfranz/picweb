(ns picweb.routes
    (:require [clojure.string :as str]
              [compojure.coercions :refer :all]
              [compojure.core :refer :all]
              [compojure.route :as route]
              [picweb.html :refer [get-next sheet-at get-rand-pic
                                   get-pic get-prev get-thumb-pic four-oh-four
                                   pic-page update-tags rotate-left rotate-right]]
              [picweb.tags :refer [edit-tags rename-tag delete-tag]]
              [picweb.sheet :refer [contact-page update-grid]]
              [picweb.filter :refer [filter-page update-filter]]
              [picweb.thumbnails :refer [get-closest-thumb]]
              [picweb.bulk :refer [bulk-page bulk-update]]))

(defn- thumb2int
    [thumb]
    (Integer/parseInt (str (subs (:timestr thumb) 0 4)
                           (subs (:timestr thumb) 5 7)
                           (subs (:timestr thumb) 8 10))))

(defroutes app-routes
           (GET "/" request
               (contact-page 0 :offset (:remote-addr request)))

           (GET "/offset" [offset :<< as-int :as {remote :remote-addr}]
               (contact-page offset :offset remote))

           (GET "/finddate" [target :<< as-int :as {remote :remote-addr}]
               (contact-page target :date remote))

           (GET "/findimg" [target :<< as-int :as {remote :remote-addr}]
               (when-let [tn (get-closest-thumb target)]
                   (contact-page (thumb2int tn) :date remote)))

           (GET "/findpage" [target :<< as-int :as {remote :remote-addr}]
               (contact-page target :page remote))

           (GET "/sheetat" [pic-id :<< as-int :as {remote :remote-addr}]
               (sheet-at pic-id remote))

           (GET "/pic" [pic-id :<< as-int]
               (pic-page pic-id))

           (GET "/picture" [pic-id :<< as-int]
               (get-pic pic-id))

           (GET "/thumb" [pic-id :<< as-int]
               (get-thumb-pic pic-id))

           (GET "/bulk" [offset :<< as-int :as {remote :remote-addr}]
               (bulk-page offset remote))

           (GET "/bulk" request
               (bulk-page 0 (:remote-addr request)))

           (POST "/bulk-tag-update" request
               (bulk-update (request :params)))

           (GET "/filter" request
               (filter-page (request :params)))

           (POST "/update-filter" request
               (update-filter (request :params)))

           (POST "/tagupdate" [pic-id :<< as-int :as request]
               (update-tags pic-id request))

           (POST "/gridupdate" request
               (let [params (request :params)
                     offset (Integer/parseInt (get params :offset 0))
                     num_per_page (Integer/parseInt (get params :num_per_page "25"))
                     remote-addr (:remote-addr request)
                     owner (get params :owner "unknown")]
               (update-grid offset num_per_page remote-addr owner)))

           (POST "/rename-tag" [tag-id :<< as-int :as request]
               (rename-tag tag-id request))

           (GET "/prev" [pic-id :<< as-int]
               (get-prev pic-id))

           (GET "/next" [pic-id :<< as-int]
               (get-next pic-id))

           (GET "/rndpic" request
               (get-rand-pic))

           (GET "/edit-tags" request
               (edit-tags))

           (GET "/delete-tag" [tag-id :<< as-int]
               (delete-tag tag-id))

           (GET "/rotate-left" [pic-id :<< as-int]
               (rotate-left pic-id))

           (GET "/rotate-right" [pic-id :<< as-int]
               (rotate-right pic-id))

           (route/resources "/")

           (ANY "*" [] (route/not-found (four-oh-four))))
