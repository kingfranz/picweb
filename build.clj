;(ns build
;    (:require [clojure.tools.build.api :as b]))
;
;(def lib 'picweb)
;;(def version (format "1.2.70"))
;(def version (format "1.1.%s" (b/git-count-revs nil)))
;(def class-dir "target/classes")
;(def main 'picweb)
;(def uber-file (format "target/%s-%s-standalone.jar" (name lib) version))
;
;;; delay to defer side effects (artifact downloads)
;(def basis (delay (b/create-basis {:project "deps.edn"})))
;
;(defn clean [_]
;    (b/delete {:path "target"}))
;
;(defn- uber-opts [opts]
;    (assoc opts
;        :lib lib :main main
;        :uber-file (format "target/%s-%s.jar" lib version)
;        :basis @basis                                       ;(b/create-basis {})
;        :class-dir class-dir
; ;       :src-dirs ["src/soren"]
;        :ns-compile [lib]))
;
;(defn ci "Run the CI pipeline of tests (and build the uberjar)." [opts]
;    ;(test opts)
;    (b/delete {:path "target"})
;    (let [opts (uber-opts opts)]
;        (println "\nCopying source...")
;        (b/copy-dir {:src-dirs ["resources" "src"] :target-dir class-dir})
;        (println (str "\nCompiling " main "..."))
;        (b/compile-clj opts)
;        (println "\nBuilding JAR...")
;        (b/uber opts))
;    opts)

(ns build
    (:require [clojure.tools.build.api :as b]))

(def build-folder "target")
(def jar-content (str build-folder "/classes"))

(def basis (b/create-basis {:project "deps.edn"}))
(def version (format "1.1.%s" (b/git-count-revs nil)))
(def app-name "picweb")
(def uber-file-name (format "%s/%s-%s-standalone.jar" build-folder app-name version)) ; path for result uber file

(defn clean [_]
      (b/delete {:path "target"})
      (println (format "Build folder \"%s\" removed" build-folder)))

(defn uber [_]
      (clean nil)

      (b/copy-dir {:src-dirs   ["resources"]         ; copy resources
                   :target-dir jar-content})

      (b/compile-clj {:basis     basis               ; compile clojure code
                      :src-dirs  ["src"]
                      :class-dir jar-content})

      (b/uber {:class-dir jar-content                ; create uber file
               :uber-file uber-file-name
               :basis     basis
               :main      'dev.core})                ; here we specify the entry point for uberjar

      (println (format "Uber file created: \"%s\"" uber-file-name)))
