(ns cissy.scripts-loader
  (:require
    [clojure.java.io :as io]
    [taoensso.timbre :as timbre]
    [clojure.tools.deps :as deps])
  (:import (java.util.zip ZipEntry ZipFile)))

;加载deps,一个zip包正常应该只有一个deps.edn
(defn load-deps-edn!
  "自动加载deps"
  [^String dep-file]

  )

(defn load-clj-file!
  "加载脚本文件"
  [^String clj-file]
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
          (load-file (slurp (.getInputStream zip-file entry)))
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

