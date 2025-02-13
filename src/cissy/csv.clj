(ns cissy.csv
  (:require [cissy.registry :refer [defnode]]
            [taoensso.timbre :as timbre]
            [cissy.helpers :as helpers]
            [clojure.java.io :as io]
            [clojure.data.csv :as csv]
            [cissy.task :as task])
  (:import (cissy.executions NodeExecutionInfo)))

;(def target-file "/home/mawdx/桌面/demo.csv")

(def write-headers-lock (atom false))

(comment (defn- get-write-header-lock
  "Get the lock for writing headers"
  []
  (loop []
    (if (compare-and-set! write-headers-lock false true)
      true
      (do
        (timbre/warn "Failed to get the lock for writing headers")
        (Thread/sleep 10)
        (recur))))))

; Ensure that only one thread can write successfully
(defn- writer-csv-headers [headers writers task-execution-dict thread-idx task-idx]
  ; Avoid subsequent threads writing headers again
  (let [writer-headers-idx (keyword (str "write-headers-" task-idx))]
    (when (nil? (writer-headers-idx @task-execution-dict))
      (try
        (loop []
          (if (compare-and-set! write-headers-lock false true)
            (when (nil? (writer-headers-idx @task-execution-dict))
              (csv/write-csv writers [headers])
              (reset! task-execution-dict (assoc @task-execution-dict writer-headers-idx true))
              (timbre/info (str "Thread=" thread-idx "successfully wrote headers")))
            (do
              (timbre/warn (str "Thread=" thread-idx "failed to get the lock for writing headers"))
              (Thread/sleep 10)
              (recur))))
        (finally
          ; Release the lock for writing headers
          (reset! write-headers-lock false))))))

(defn- target-file-fn [task-info]
  (let [task-file-config (get-in @task-info [:task-config :csvw :target_file] nil) 
        task-name (:task-name @task-info)]
    (if (nil? task-file-config)
      (str (helpers/get-desktop-path) "/" task-name ".csv")
      task-file-config)))

; Unique or empty parent node
(defn- parent-node-id [node-graph node-id]
       (:node-id (first (task/get-parent-nodes node-graph node-id))))

;; Write to csv file
(defnode csvw [^NodeExecutionInfo node-exec-info]
  (let [{:keys [task-execution-info node-result-dict node-execution-dict]} @node-exec-info
        {:keys [task-info task-execution-dict]} @task-execution-info
        {:keys [task-idx task-name task-config node-graph]} @task-info
        {:keys [csvw]} task-config
        {:keys [thread-idx]} @node-execution-dict
        node-result-lst (get @node-result-dict (keyword (parent-node-id node-graph "csvw")))
        target-file (target-file-fn task-info)]
    (if (or (nil? node-result-lst) (= (count node-result-lst) 0))
      (do
        (timbre/info (str "Current node=" thread-idx "did not read data, do not execute csvw node"))
        (helpers/curr-node-done node-exec-info))
      (let [rows (vec (map #(vec (vals %)) node-result-lst))
            ; Get column information
            headers (vec (map #(name %) (keys (first node-result-lst))))]
        ; Append write, do not overwrite
        (with-open [wrt (io/writer target-file :append true)]
          (writer-csv-headers headers wrt task-execution-dict thread-idx task-idx)
          (csv/write-csv wrt rows)
          (swap! (:sync-count @task-execution-dict) #(+ % (count rows)))
          (timbre/info (str "Written " (deref (:sync-count @task-execution-dict)) " records to file:" target-file)))))))
