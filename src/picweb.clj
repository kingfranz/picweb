(ns picweb
    (:require [clojure.java.io :as io]
              [clojure.java.shell :refer [sh]]
              [clojure.pprint :as pp]
              [org.httpkit.server :refer [run-server]]
              [picweb.html :refer [mk-tn-name]]
              [picweb.routes :refer [app-routes]]
              [picweb.thumbnails :refer [delete-thumb get-all-thumbs
                                         load-thumbnails report-stats wait-for-it]]
              [ring.middleware.cookies :refer [wrap-cookies]]
              [ring.middleware.keyword-params :refer [wrap-keyword-params]]
              [ring.middleware.params :refer [wrap-params]]
              [ring.middleware.stacktrace :refer [wrap-stacktrace]])
    (:gen-class))

(defn- trace-call
    [handler]
    (fn [request]
        ;(println "=========================================================Incoming:")
        ;(pp/pprint request)
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

(defn- th-path
    [thumb]
    (mk-tn-name thumb))

(defn- f-path
    [thumb]
    (str (:path thumb) "/" (:filename thumb)))

(defn- th-path?
    [thumb]
    (.exists (io/file (th-path thumb))))

(defn- f-path?
    [thumb]
    (.exists (io/file (f-path thumb))))

(defn- cleanup
    []
    (let [dry-run false
          thumbs (get-all-thumbs)]
        ; are both missing?
        (doseq [t (filter #(and (not (f-path? %)) (not (th-path? %))) thumbs)]
            (println "both missing: " (f-path t))
            (when-not dry-run
                (delete-thumb (:id t))))

        ; orphaned thumbnail file
        (doseq [t (filter #(and (not (f-path? %)) (not (th-path? %))) thumbs)]
            (println "Deleting orphaned DB entry and thumbnail:" (th-path t))
            (when-not dry-run
                (.delete (io/file (th-path t)))
                (delete-thumb (:id t))))

        ; missing thumbnail file
        (doseq [t (filter #(and (not (f-path? %)) (not (th-path? %))) thumbs)]
            (println "missing  thumbnail, recreateing for:" (f-path t))
            (when-not dry-run
                (let [cmd (str "convert " (f-path t) " -auto-orient -thumbnail 150x150^ -gravity center -extent 150x150 " (th-path t))
                      res (sh "bash" "-c" cmd)]
                    (if (= 0 (:exit res))
                        (println "Thumbnail recreated successfully for" (f-path t))
                        (println "Error recreating thumbnail for" (f-path t) ":" (:err res))))
                ))
        ; delete if rated 1 (=delete it)
        (doseq [t (filter #(= (:rating %) 1) thumbs)]
            (println "Deleting:" (f-path t) (:rating t))
            (when-not dry-run
                (.delete (io/file (th-path t)))
                (.delete (io/file (f-path t)))
                (delete-thumb (:id t)))
            )))

(defn -main
    "Main entry point for the PicWeb application."
    [& args]
    ;(set! *assert* true)
    (alter-var-root #'*assert* (constantly false))
    (if (and (seq args) (= (:arg (first args)) "--cleanup"))
        (do
            (println "Running in clean mode, not starting server.")
            (cleanup)
            (System/exit 0)
            )
        (do
            (println "Starting PicWeb application...")
            (.start (Thread. #(report-stats)))
            (load-thumbnails)
            (run-server app {:join? false :port 4559})))
    (println "PicWeb is running on port 4559!")
    (println "Press Enter to stop.")
    (read-line)
    (println "Thank you! Goodbye.")
    (reset! wait-for-it false)
    )
