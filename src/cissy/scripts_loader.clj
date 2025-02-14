(ns cissy.scripts-loader
  (:require
    [cissy.helpers :as helpers]
    [clojure.edn :as edn]
    [clojure.java.io :as io]
    [clojure.string :as str]
    [clojure.tools.deps :as deps]
    [clojure.tools.deps.util.maven :as maven]
    [taoensso.timbre :as timbre])
  (:import (clojure.lang DynamicClassLoader RT)
           (java.io File)
           (java.util.zip ZipEntry ZipFile)))

; Global
(def script-class-loader (DynamicClassLoader. (RT/baseLoader)))

; Load dependencies
(defn load-dependency [deps-map]
  (let [; Add repo configuration, otherwise deps cannot be resolved
        deps-tree (deps/resolve-deps deps-map nil)
        ; Resolve paths
        paths (vec (flatten (map :paths (vals deps-tree))))]
    ;; Print all dependency paths (including transitive dependencies)
    (timbre/info "Resolved paths:" paths)
    (doseq [^String path paths]
      (RT/addURL (.toURL
                   (File. path))))))

; Load deps, a zip package should normally only have one deps.edn
(defn load-deps-edn!
  "Automatically load deps"
  [^String dep-file]
  (let [deps-map (edn/read-string dep-file)]
    (if-let [_ (get deps-map :mvn/repos)]
      (load-dependency deps-map)
      (do
        (timbre/info "No custom repos found, loading standard repos[https://repo1.maven.org/maven2/,https://clojars.org/repo/]")
        (load-dependency (assoc deps-map :mvn/repos maven/standard-repos))))
    (timbre/info "Dependencies loaded")))

;(defn load-clj-file!
;  "Load script file"
;  [^String clj-file]
;  (load-string clj-file))

; Does not support nested script directories
; Supports a.clj, b.clj, deps.clj in one level directory, does not support d/a.clj, d/d1/a.clj
(defn load-zip!
  "Load zip format task"
  [^String zip-file-path]
  (let [zip-file (ZipFile. zip-file-path)
        entries (.entries zip-file)]
    ; Load deps.edn
    ; Modify classloader, only need to modify if deps are loaded
    (when-let [deps-entry (.getEntry zip-file "deps.edn")]
      ;; Set context classloader to DynamicClassLoader
      (let [current-thread (Thread/currentThread)]
        (.setContextClassLoader current-thread script-class-loader))
      ;; Bind compiler to the same classloader
      (prn script-class-loader)
      ;; 20250206 If the current thread has no initialized bound value, directly resetting will throw an exception
      (when (.isBound Compiler/LOADER)
        (.set Compiler/LOADER script-class-loader))
      (load-deps-edn! (slurp (.getInputStream zip-file deps-entry))))
    ;(require '[clj-http.client :as client])
    (while (.hasMoreElements entries)
      (let [^ZipEntry entry (.nextElement entries)
            entry-name (.getName entry)]
        (when (.endsWith entry-name ".clj")
          (load-string (slurp (.getInputStream zip-file entry)))
          (timbre/info (str "Successfully loaded script file:" entry-name)))))))

(defn file-exists? [^String path]
  (.exists (io/file path)))

(defn load-main-entry
  "Load script"
  [^String main-entry]
  (if (file-exists? main-entry)
    ; Related registration and loading declarations need to be placed in the entry-script script
    (cond (str/ends-with? main-entry ".clj") (load-file main-entry)
          (str/ends-with? main-entry ".zip") (load-zip! main-entry)
          :else (timbre/error "Unsupported file suffix:" main-entry))
    (timbre/error (str "Script file does not exist:" main-entry))))

