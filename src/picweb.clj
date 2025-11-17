(ns picweb
    (:require [clojure.java.io :as io]
              [clojure.java.shell :refer [sh]]
              [picweb.html :refer [mk-tn-name]]
              [picweb.routes :refer [app-routes]]
              [picweb.thumbnails :refer [delete-thumb get-all-thumbs]]
              [ring.middleware.cookies :refer [wrap-cookies]]
              [ring.middleware.keyword-params :refer [wrap-keyword-params]]
              [ring.middleware.params :refer [wrap-params]]
              [ring.middleware.resource :refer [wrap-resource]]
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

(def app
    (-> app-routes
        (wrap-keyword-params)
        (wrap-params)
        (wrap-cookies)
        (wrap-stacktrace)
        ;(wrap-resource "resources")
        (trace-call)
        ;(route/not-found (four-oh-four))
        ))

(defn- cleanup
    []
    (let [thumbs (get-all-thumbs)]
        (doseq [t thumbs
                :let [fpath (str (:path t) "/" (:filename t))
                      thpath (mk-tn-name t)]
                :when [(or (not (.exists (io/file fpath)))
                           (not (.exists (io/file thpath))))]]
            (println "Checking DB entry and thumbnail:" thpath)
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
                    (let [cmd (str "convert " fpath " -auto-orient -thumbnail 150x150^ -gravity center -extent 150x150 " thpath)
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
    ;(set! *assert* true)
    (alter-var-root #'*assert* (constantly true))
    (if (and (seq args) (= (:arg (first args)) "--cleanup"))
        (do
            (println "Running in clean mode, not starting server.")
            (cleanup)
            (System/exit 0)
            )
        (do
            (println "Starting PicWeb application...")
            (run-server app {:join? false :port 4559})))
    (println "PicWeb is running on port 4559!"))
