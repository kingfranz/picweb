(ns picweb
    (:require [compojure.core :refer [defroutes GET POST]]
              [compojure.route :as route]
              [picweb.html :refer [four-oh-four]]
              [picweb.routes :refer [app-routes]]
              [ring.middleware.cookies :refer [wrap-cookies]]
              [ring.middleware.keyword-params :refer [wrap-keyword-params]]
              [ring.middleware.params :refer [wrap-params]]
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
    [& _args]
    (set! *assert* true)
    (println "Starting PicWeb application...")
    (-> all-routes
        (wrap-keyword-params)
        (wrap-params)
        (wrap-cookies)
        (wrap-stacktrace)
        (trace-call)
        (run-server {:port 4559})
        )
    (println "PicWeb is running on port 4559!"))