(ns picweb
    (:require [compojure.core :refer :all]
              [compojure.route :as route]
              [picweb.html :refer [four-oh-four]]
              [picweb.routes :refer [app-routes]]
              [picweb.store :refer :all]
              ;[ring.middleware.defaults :refer [wrap-defaults site-defaults]]
              [ring.middleware.cookies :refer [wrap-cookies]]
              [ring.middleware.keyword-params :refer [wrap-keyword-params]]
              [ring.middleware.params :refer [wrap-params]]
        ;[ring.util.http-response :as response]
              [clojure.pprint :as pp]
              [ring.middleware.session :refer [wrap-session]]
              [ring.middleware.stacktrace :refer [wrap-stacktrace]])
    (:use [org.httpkit.server :only [run-server]])
    (:gen-class))

(defn- trace-call
    [handler]
    (fn [request]
       ; (println "Incoming:")
       ; (pp/pprint request)
        (let [ret (handler request)]
            ret)))

(defroutes all-routes
           app-routes
           (route/not-found (four-oh-four)))

(defn -main
    "Main entry point for the PicWeb application."
    [& args]
    (println "Starting PicWeb application...")
    (-> all-routes
        (wrap-session {:store (->ShopStore)})
        (wrap-keyword-params)
        (wrap-params)
        (wrap-cookies)
        (wrap-stacktrace)
        (trace-call)
        ;(wrap-defaults site-defaults)
        (run-server {:port 4559})
        )
    (println "PicWeb is running!"))