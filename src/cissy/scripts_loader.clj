(ns cissy.scripts-loader
  (:require
    [clojure.java.io :as io]
    [clojure.edn :as edn]
    [clojure.tools.deps :as deps]
    [clojure.tools.deps.util.maven :as maven]
    [taoensso.timbre :as timbre]
    [cissy.helpers :as helpers])
  (:import (clojure.lang RT)
           (java.io File)
           (java.util.zip ZipEntry ZipFile)))


; 加载依赖项
(defn load-dependency [deps-map]
  (let [repo-config {:mvn/repos maven/standard-repos}
        ;添加repo配置不加会无法解析deps
        deps-tree (deps/resolve-deps (helpers/my-merge-fn deps-map repo-config) nil)
        ;解析paths
        paths (flatten (map :paths (vals deps-tree)))]
    ;; 打印所有依赖路径（包括传递依赖）
    (println "Resolved paths:" paths)
    (doseq [^String path paths]
      (RT/addURL (File. path)))))

;;从自定义目录中加载lib
(defn load-dependency-from-custom-lib [deps-map custom-lib-path]
  (let [repo-config {:mvn/repos (assoc maven/standard-repos :custom-lib {:url custom-lib-path})}
        deps-tree (deps/resolve-deps (helpers/my-merge-fn deps-map repo-config) nil)
        ;解析paths
        paths (flatten (map :paths (vals deps-tree)))]
    (doseq [^String path paths]
      (RT/addURL (File. path)))))

;加载deps,一个zip包正常应该只有一个deps.edn
(defn load-deps-edn!
  "自动加载deps"
  [^String dep-file]
  (let [deps-map (edn/read-string dep-file)]
    (load-dependency deps-map))
  )

(defn load-clj-file!
  "加载脚本文件"
  [^String clj-file]
  (load-string clj-file)
  )

;不支持嵌套脚本目录
;支持a.clj,b.clj,deps.clj一层目录的不支持;d/a.clj,d/d1/a.clj这种
(defn load-zip!
  "加载zip格式的任务"
  [^String zip-file-path]
  (let [zip-file (ZipFile. zip-file-path)
        entries (.entries zip-file)]
    ;加载deps.edn
    (when-let [deps-entry (.getEntry zip-file "deps.edn")]
      (load-deps-edn! (slurp (.getInputStream zip-file deps-entry))))
    (while (.hasMoreElements entries)
      (let [^ZipEntry entry (.nextElement entries)
            entry-name (.getName entry)]
        (when (.endsWith entry-name ".clj")
          (load-string (slurp (.getInputStream zip-file entry)))
          (timbre/info (str "加载脚本文件:" entry-name "成功.")))))))



(defn file-exists? [path]
  (.exists (io/file path)))


(defn load-main-entry
  "加载脚本"
  [main-entry]
  (if (file-exists? main-entry)
    ;相关的注册以及加载声明都需要放entry-script脚本里
    (load-file main-entry)
    (timbre/error (str "脚本文件不存在:" main-entry))))

