(ns cissy.scripts-loader
  (:require
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [clojure.tools.deps :as deps]
   [clojure.tools.deps.util.maven :as maven]
   [clojure.tools.namespace.parse :as parser]
   [taoensso.timbre :as timbre])
  (:import (clojure.lang DynamicClassLoader RT)
           (java.io File PushbackReader StringReader)
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
        ;; Set context classloader to DynamicClassLoader
  (let [current-thread (Thread/currentThread)]
    (.setContextClassLoader current-thread script-class-loader))
      ;; Bind compiler to the same classloader
  (prn script-class-loader)
      ;; 20250206 If the current thread has no initialized bound value, directly resetting will throw an exception
  (when (.isBound Compiler/LOADER)
    (.set Compiler/LOADER script-class-loader))
  (let [deps-map (edn/read-string dep-file)]
    (if-let [_ (get deps-map :mvn/repos)]
      (load-dependency deps-map)
      (do
        (timbre/info "No custom repos found, loading standard repos[https://repo1.maven.org/maven2/,https://clojars.org/repo/]")
        (load-dependency (assoc deps-map :mvn/repos maven/standard-repos))))
    (timbre/info "Dependencies loaded")))

;; parse clj-file
(defn- to-clj-file [file-name clj-content]
  (with-open [rdr (PushbackReader. (StringReader. clj-content))]
      ;; return a list,first element is ns,second is curr ns
    (let [ns-list (parser/read-ns-decl rdr)]
      {:curr-ns     (str (second ns-list))
       :deps-str    (str (vec (drop 2 ns-list)))
       :clj-content clj-content
       :file-name   file-name})))

;; include all the sub-dir under the specified directory
;; find all clj files
(defn- find-clj-files-in-zip [zip-file]
  ;; Get all clj files under the specified directory
  (let [clj-entries (loop [entries (enumeration-seq (.entries zip-file))
                           clj-entries []]
                      (if (empty? entries)
                        clj-entries
                        (let [entry (first entries)]
                          (if (and (not (.isDirectory entry)) (.endsWith (.getName entry) ".clj"))
                            (recur (rest entries) (conj clj-entries entry))
                            (recur (rest entries) clj-entries)))))]
    (map (fn [entry] (to-clj-file (.getName entry) (slurp (.getInputStream zip-file entry)))) clj-entries)))

(defn- find-clj-files-in-dir [dir]
  (let [file (File. dir)
        files (file-seq file)
        clj-files (filter #(and (not (.isDirectory %)) (.endsWith (.getName %) ".clj")) files)]
    (map (fn [file] (to-clj-file (.getName file) (slurp file))) clj-files)))

(defn- get-deps-vec [clj-file ns-decls]
  (filter #(str/includes? (:deps-str clj-file) %) ns-decls))

(defn- load-clj-files [clj-files]
  (timbre/info "the loading order is:" (map :file-name clj-files))
        ;; get all the ns declarations
  (let [ns-decls (map :curr-ns clj-files)
        clj-files-with-deps (map #(assoc % :deps (doall (get-deps-vec % ns-decls))) clj-files)]
    (timbre/info (str clj-files-with-deps))
    (loop [clj-files clj-files-with-deps
           loaded-ns #{}]
      (prn "----------------" loaded-ns " clj-file-size" (count clj-files))
      (if (empty? clj-files)
        (timbre/info "All scripts loaded")
              ; the script loading precedence is based on the dependency relationship
              ; if a depends on b, then b should be loaded before a
              ; if a and b have no dependency relationship, then a should be loaded before b
        (let [loadable-files (filter #(or (empty? (:deps %))
                                          (and
                                           (> (count loaded-ns) 0)
                                           (every? loaded-ns (set (:deps %))))) clj-files)
              loadable-ns (set (map :curr-ns loadable-files))]
          (doseq [clj-file loadable-files]
            (timbre/info "start to load:" (:file-name clj-file))
            (load-string (:clj-content clj-file))
            (timbre/info "load:" (:file-name clj-file) "success!"))
          (recur
           (remove #(contains? loadable-ns (:curr-ns %)) clj-files)
           (into loaded-ns loadable-ns)))))))

; Does not support nested script directories
; Supports a.clj, b.clj, deps.clj in one level directory, does not support d/a.clj, d/d1/a.clj
(defn load-zip!
  "Load zip format task"
  [^String zip-file-path]
  (let [zip-file (ZipFile. zip-file-path)]
    ; Load deps.edn
    ; Modify classloader, only need to modify if deps are loaded
    (when-let [deps-entry (.getEntry zip-file "deps.edn")]
      (load-deps-edn! (slurp (.getInputStream zip-file deps-entry))))
    ; Load scripts
    (let [clj-files (find-clj-files-in-zip zip-file)]
      (load-clj-files clj-files))))

(defn file-exists? [^String path]
  (.exists (io/file path)))

(defn load-dir!
  "load script in dir"
  [^String dir-path]
  ;; check current file is dir
  (when-not (.isDirectory (io/file dir-path))
    (throw (Exception. (str dir-path " is not a dir"))))
  ;; check if the deps.edn exists
  (let [deps-edn-path (str dir-path "/deps.edn")]
    (when (file-exists? deps-edn-path)
      (load-deps-edn! (slurp deps-edn-path))))
  (let [clj-files (find-clj-files-in-dir dir-path)]
    (load-clj-files clj-files)))

(defmulti load-ext-script (fn [param] (:type param)))

;; clj file
(defmethod load-ext-script :clj
  [^{:type :clj} param]
  (timbre/info "load clj:" (:file-path param))
  (load-string (slurp (:file-path param))))

;; zip file
(defmethod load-ext-script :zip
  [^{:type :zip} param]
  (timbre/info "load zip:" (:file-path param))
  (load-zip! (:file-path param)))

;; dir
(defmethod load-ext-script :dir
  [^{:type :dir} param]
  (timbre/info "load dir:" (:file-path param))
  (load-dir! (:file-path param)))

(defn load-main-entry
  "Load script"
  [^String main-entry]
  (if (file-exists? main-entry)
    ; Related registration and loading declarations need to be placed in the entry-script script
    (cond (str/ends-with? main-entry ".clj") (load-ext-script {:type :clj, :file-path main-entry})
          (str/ends-with? main-entry ".zip") (load-ext-script {:type :zip, :file-path main-entry})
          :else (load-ext-script {:type :dir, :file-path main-entry}))
    (timbre/error (str "Script file does not exist:" main-entry))))