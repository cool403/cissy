(ns cissy.ext.entry-loader
  (:require
   [clojure.java.io :as io]
   [clojure.string :as str]
   [taoensso.timbre :as timbre]))

;; #_{:clj-kondo/ignore [:earmuffed-var-not-dynamic]}
;; (def ^:private *loaded-files* (atom #{}))

;; (defn- extract-dependencies [file-content]
;;   (let [require-pattern #"\(:require \[([^\]]+)\]\)"
;;         matches (re-seq require-pattern file-content)]
;;     (map second matches)))

;; (defn- resolve-file-path [ns-sym]
;;   (-> (str/replace (name ns-sym) "." "/")
;;       (str ".clj")))

;; (defn load-file-with-deps [file-path]
;;   (when-not (contains? @*loaded-files* file-path)
;;     (println "Loading file:" file-path)
;;     (let [file-content (slurp file-path)
;;           dependencies (extract-dependencies file-content)]
;;       (doseq [dep-ns dependencies]
;;         (let [dep-file-path (resolve-file-path dep-ns)]
;;           (load-file-with-deps dep-file-path)))
;;       (load-file file-path)
;;       (swap! *loaded-files* conj file-path))))

;; ;; 使用示例
;; (load-file-with-deps "a.clj")

(defn file-exists? [path]
  (.exists (io/file path)))


(defn load-main-entry
  "加载脚本"
  [entry]
  (if (file-exists? entry)
    ;相关的注册以及加载声明都需要放entry-script脚本里
    (load-file entry)
    (timbre/error (str "脚本文件不存在:" entry))))

