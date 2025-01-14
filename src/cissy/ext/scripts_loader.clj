(ns cissy.ext.scripts-loader
  (:require
   [clojure.java.io :as io]
   [clojure.string :as str]
   [taoensso.timbre :as timbre]))

(def ^:private *loaded-files* (atom #{}))

(defn- extract-dependencies [file-content]
  (let [require-pattern #"\(:require \[([^\]]+)\]\)"
        matches (re-seq require-pattern file-content)]
    (map second matches)))

(defn- resolve-file-path [ns-sym]
  (-> (str/replace (name ns-sym) "." "/")
      (str ".clj")))

(defn load-file-with-deps [file-path]
  (when-not (contains? @*loaded-files* file-path)
    (println "Loading file:" file-path)
    (let [file-content (slurp file-path)
          dependencies (extract-dependencies file-content)]
      (doseq [dep-ns dependencies]
        (let [dep-file-path (resolve-file-path dep-ns)]
          (load-file-with-deps dep-file-path)))
      (load-file file-path)
      (swap! *loaded-files* conj file-path))))

;; 使用示例
(load-file-with-deps "a.clj")

(defn directory-exists? [path]
  (.exists (io/file path)))

(defn create-directory [path]
  (.mkdirs (io/file path)))

(defn get-clj-files [path]
  (filter #(.endsWith (.getName %) ".clj")
          (file-seq (io/file path))))


(defn load-scripts
  "加载脚本"
  [scripts-dir]
  )


(defn list-scripts-fn
  "列出脚本"
  [scripts-dir]
  (if (directory-exists? scripts-dir)
    (do
      (timbre/info (str scripts-dir "目录已存在,读取目录下所有的.clj脚本文件"))
      (doseq [file (get-clj-files scripts-dir)]
        (timbre/info "找到文件:" (.getAbsolutePath file))))
    (do
      (timbre/info (str scripts-dir "目录不存在，创建目录..."))
      (create-directory scripts-dir)
      (timbre/info (str "创建目录" scripts-dir "成功")))))