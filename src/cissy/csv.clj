(ns cissy.csv
  (:require [cissy.registry :refer [defnode]]
            [taoensso.timbre :as timbre]
            [cissy.helpers :as helpers]
            [clojure.java.io :as io]
            [clojure.data.csv :as csv]
            [cissy.task :as task])
  (:import (cissy.executions NodeExecutionInfo)))

;(def target-file "/home/mawdx/桌面/demo.csv")

;; 写出到csv文件
(defnode csvw [^NodeExecutionInfo node-exec-info]
  (let [{task-execution-info :task-execution-info
         node-param-dict     :node-param-dict
         node-result-dict    :node-result-dict} @node-exec-info
        {task-execution-dict :task-execution-dict} @task-execution-info
        drn-res (get @node-result-dict :drn)
        target-file (get-in (deref (:task-info @task-execution-info)) [:task-config :csvw :target_file] "/tmp/result.csv")]
    (if (or (nil? drn-res) (= (count drn-res) 0))
      (do
        (timbre/info "当前节点未读取到数据，不执行csvw节点")
        (helpers/curr-node-done node-exec-info))
      (let [rows (vec (map #(vec (vals %)) drn-res))]
        ;追加写入,不覆盖
        (with-open [wrt (io/writer target-file :append true)]
          (csv/write-csv wrt rows)
          (timbre/info (str "已写入" (deref (:sync-count @task-execution-dict)) "条记录到文件:" target-file)))))))
