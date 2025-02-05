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
;全局
(def script-class-loader (DynamicClassLoader. (RT/baseLoader)))

; 加载依赖项
(defn load-dependency [deps-map ^String custom-lib-path]
  (let [default-repos maven/standard-repos
        repo-lst (if (nil? custom-lib-path) default-repos (assoc default-repos :custom-lib {:url custom-lib-path}))
        ;添加repo配置不加会无法解析deps
        deps-tree (deps/resolve-deps (helpers/my-merge-fn deps-map {:mvn/repos repo-lst}) nil)
        ;解析paths
        paths (vec (flatten (map :paths (vals deps-tree))))]
    ;; 打印所有依赖路径（包括传递依赖）
    (println "Resolved paths:" paths)
    (doseq [^String path paths]
      (RT/addURL (.toURL
                   (File. path))))))

;加载deps,一个zip包正常应该只有一个deps.edn
(defn load-deps-edn!
  "自动加载deps"
  [^String dep-file]
  (let [deps-map (edn/read-string dep-file)]
    (load-dependency deps-map nil)
    (timbre/info "依赖加载完成")))

;(defn load-clj-file!
;  "加载脚本文件"
;  [^String clj-file]
;  (load-string clj-file))

;不支持嵌套脚本目录
;支持a.clj,b.clj,deps.clj一层目录的不支持;d/a.clj,d/d1/a.clj这种
(defn load-zip!
  "加载zip格式的任务"
  [^String zip-file-path]
  (let [zip-file (ZipFile. zip-file-path)
        entries (.entries zip-file)]
    ;加载deps.edn
    ;修改classloader，只有加载deps了才需要修改
    (when-let [deps-entry (.getEntry zip-file "deps.edn")]
      ;; 设置上下文类加载器为 DynamicClassLoader
      (let [current-thread (Thread/currentThread)]
        (.setContextClassLoader current-thread script-class-loader))
      ;; 让compiler 也绑定到同一个classloader上
      (.set Compiler/LOADER script-class-loader)
      (load-deps-edn! (slurp (.getInputStream zip-file deps-entry))))
    ;(require '[clj-http.client :as client])
    (while (.hasMoreElements entries)
      (let [^ZipEntry entry (.nextElement entries)
            entry-name (.getName entry)]
        (when (.endsWith entry-name ".clj")
          (load-string (slurp (.getInputStream zip-file entry)))
          (timbre/info (str "加载脚本文件:" entry-name "成功.")))))))

(defn file-exists? [^String path]
  (.exists (io/file path)))

(defn load-main-entry
  "加载脚本"
  [^String main-entry]
  (if (file-exists? main-entry)
    ;相关的注册以及加载声明都需要放entry-script脚本里
    (cond (str/ends-with? main-entry ".clj") (load-file main-entry)
          (str/ends-with? main-entry ".zip") (load-zip! main-entry)
          :else (timbre/error "不支持的文件后缀:" main-entry))
    (timbre/error (str "脚本文件不存在:" main-entry))))

