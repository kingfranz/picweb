(ns picweb
    (:require [compojure.core :refer [defroutes GET POST]]
              [compojure.route :as route]
              [clojure.java.io :as io]
              [clojure.java.shell :refer [sh]]
              [picweb.html :refer [four-oh-four mk-tn-name]]
              [picweb.routes :refer [app-routes]]
              [picweb.thumbnails :refer [get-all-thumbs delete-thumb]]
              [ring.middleware.cookies :refer [wrap-cookies]]
              [ring.middleware.keyword-params :refer [wrap-keyword-params]]
              [ring.middleware.params :refer [wrap-params]]
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

(defn- cleanup
    []
    (let [thumbs (get-all-thumbs)]
        (doseq [t thumbs
                :let [fpath (str (:path t) "/" (:filename t))
                      thpath (mk-tn-name t)]
                :when [(or (not (.exists (io/file fpath)))
                           (not (.exists (io/file thpath))))]]
            (cond
                (and (.exists (io/file thpath)) (not (.exists (io/file fpath))))
                (do
                    (println "Deleting orphaned DB entry and thumbnail:" thpath)
                    (.delete (io/file thpath))
                    (delete-thumb (:id t))
                    )
                (and (.exists (io/file fpath)) (not (.exists (io/file thpath))))
                (do
                    (println "missing  thumbnail, but no action for now")
                    (let [cmd (str "convert " fpath " -auto-orient -thumbnail 200x200^ -gravity center -extent 200x200 " thpath)
                          res (sh "bash" "-c" cmd)
                          ]
                        (if (= 0 (:exit res))
                            (println "Thumbnail recreated successfully for" fpath)
                            (println "Error recreating thumbnail for" fpath ":" (:err res))))
                )
                (and (not (.exists (io/file fpath))) (not (.exists (io/file thpath))))
                (do
                    (println "both missing: " fpath " " (.exists (io/file fpath)))
                    (println "both missing: " thpath " " (.exists (io/file thpath)))
                    (delete-thumb (:id t))
                    )))))

(defn -main
    "Main entry point for the PicWeb application."
    [& args]
    (set! *assert* true)
    (if (and (seq args) (= (:arg (first args)) "--cleanup"))
        (do
            (println "Running in clean mode, not starting server.")
            (cleanup)
            (System/exit 0)
        )
    (do
    (println "Starting PicWeb application...")
    (-> all-routes
        (wrap-keyword-params)
        (wrap-params)
        (wrap-cookies)
        (wrap-stacktrace)
        (trace-call)
        (run-server {:port 4559})
        )
    (println "PicWeb is running on port 4559!"))))
