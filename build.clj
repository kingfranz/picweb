(ns build
    (:require [clojure.tools.build.api :as b]))

(def lib 'picweb)
;(def version (format "1.2.70"))
(def version (format "1.1.%s" (b/git-count-revs nil)))
(def class-dir "target/classes")
(def main 'picweb)
(def uber-file (format "target/%s-%s-standalone.jar" (name lib) version))

;; delay to defer side effects (artifact downloads)
(def basis (delay (b/create-basis {:project "deps.edn"})))

(defn clean [_]
    (b/delete {:path "target"}))

(defn- uber-opts [opts]
    (assoc opts
        :lib lib :main main
        :uber-file (format "target/%s-%s.jar" lib version)
        :basis @basis                                       ;(b/create-basis {})
        :class-dir class-dir
 ;       :src-dirs ["src/soren"]
        :ns-compile [lib]))

(defn ci "Run the CI pipeline of tests (and build the uberjar)." [opts]
    ;(test opts)
    (b/delete {:path "target"})
    (let [opts (uber-opts opts)]
        (println "\nCopying source...")
        (b/copy-dir {:src-dirs ["resources" "src"] :target-dir class-dir})
        (println (str "\nCompiling " main "..."))
        (b/compile-clj opts)
        (println "\nBuilding JAR...")
        (b/uber opts))
    opts)
