(ns build
  (:require [clojure.tools.build.api :as b]))

(def lib 'cissy/cissy)
(def version "0.1.0")
(def class-dir "target/classes")
(def basis (b/create-basis {:project "deps.edn"}))
(def uber-file "target/cissy.jar")

(defn clean [_]
  (b/delete {:path "target"}))

(defn compile [_]
  (b/compile-clj {:basis     basis
                  :src-dirs  ["src" "resources"]
                  :class-dir class-dir}))

(defn uber [_]
  (b/copy-dir {:src-dirs   ["src" "resources"]
               :target-dir class-dir})
  (b/compile-clj {:basis     basis
                  :src-dirs  ["src"]
                  :class-dir class-dir})
  (b/uber {:class-dir class-dir
           :uber-file uber-file
           :basis     basis
           :main      'cissy.app}))