(ns cissy.helpers
  (:require [cissy.executions :as executions]
            [clojure.java.io :as io]
            [cissy.task :as task])
  (:import (java.io File)
           (java.nio.file Files Path Paths StandardOpenOption)))


(defn my-merge-fn [m1 m2]
  ; Custom merge, if exists in m1 and m2, keep m1; if not exists in m1, add from m2
  (reduce (fn [acc [k v]]
            (if (contains? acc k)
              acc
              (assoc acc k v)))
          m1
          m2))

(defn curr-node-done
  "Mark the current node as done"
  [task-node-execution-info]
  (if (or (nil? task-node-execution-info) (not (instance? clojure.lang.Atom task-node-execution-info)))
    (throw (IllegalArgumentException. "Type must be atom and cannot be nil"))
    (cond (instance? cissy.executions.NodeExecutionInfo @task-node-execution-info)
          (reset! task-node-execution-info (assoc @task-node-execution-info :curr-node-status "done")))))

; Get desktop path
(defn get-desktop-path []
  (let [home-dir (System/getProperty "user.home")
        first-desktop-dir (File. home-dir "桌面")] ; 获取用户主目录
    (if (.exists first-desktop-dir)
      (.toPath first-desktop-dir)
      (Paths/get home-dir (into-array ["Desktop"])))))

; Write file to desktop
(defn write-files-to-desktop [file-bytes file-name]
  (let [desktop-path (get-desktop-path) ; Get desktop directory path
        file-path (.resolve desktop-path file-name)] ; Build file path
    (Files/write file-path file-bytes
                 (into-array [StandardOpenOption/CREATE StandardOpenOption/TRUNCATE_EXISTING]))
    (str file-path)))

; Unique or empty parent node
(defn parent-node-id-fn [node-graph node-id]
  (:node-id (first (task/get-parent-nodes node-graph node-id))))