(ns cissy.helpers
  (:require [cissy.executions :as executions]
            [clojure.java.io :as io])
  (:import (java.io File)
           (java.nio.file Files Path Paths StandardOpenOption)))


(defn my-merge-fn [m1 m2]
  ;自定义合并，存在于m1,也存在于m2,保留m1；不存在于m1,存在于m2,添加
  (reduce (fn [acc [k v]]
            (if (contains? acc k)
              acc
              (assoc acc k v)))
          m1
          m2))


(defn curr-node-done
  "当前节点标记完成"
  [task-node-execution-info]
  (if (or (nil? task-node-execution-info) (not (instance? clojure.lang.Atom task-node-execution-info)))
    (throw (IllegalArgumentException. "类型必须是atom类型，且不能为nil"))
    (cond (instance? cissy.executions.NodeExecutionInfo @task-node-execution-info)
          (reset! task-node-execution-info (assoc @task-node-execution-info :curr-node-status "done")))))

; 获取桌面路径
(defn get-desktop-path []
  (let [home-dir (System/getProperty "user.home")
        first-desktop-dir (File. home-dir "桌面")] ; 获取用户主目录
    (if (.exists first-desktop-dir)
      (.toPath first-desktop-dir)
      (Paths/get home-dir (into-array ["Desktop"])))))

; 写文件到桌面
(defn write-files-to-desktop [file-bytes file-name]
  (let [desktop-path (get-desktop-path) ; 获取桌面目录路径
        file-path (.resolve desktop-path file-name)] ; 构建文件路径
    (Files/write file-path file-bytes
                 (into-array [StandardOpenOption/CREATE StandardOpenOption/TRUNCATE_EXISTING]))
    (str file-path)))