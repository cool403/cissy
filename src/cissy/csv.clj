(ns cissy.csv
  (:require [cissy.registry :refer [defnode]]
            [taoensso.timbre :as timbre]
            [cissy.helpers :as helpers]
            [clojure.java.io :as io]
            [clojure.data.csv :as csv]
            [cissy.task :as task])
  (:import (cissy.executions NodeExecutionInfo)))

;(def target-file "/home/mawdx/桌面/demo.csv")

(def write-header? (ref false))

;保证只有一个线程能写成功
(defn- writer-csv-headers [headers writers]
  (dosync
   (when (not @write-header?)
     (alter write-header? (constantly true))
     (csv/write-csv writers [headers]))))

;; 写出到csv文件
(defnode csvw [^NodeExecutionInfo node-exec-info]
  (let [{task-execution-info :task-execution-info
         node-result-dict    :node-result-dict} @node-exec-info
        {task-execution-dict :task-execution-dict} @task-execution-info
        drn-res (get @node-result-dict :drn)
        target-file (get-in (deref (:task-info @task-execution-info)) [:task-config :csvw :target_file] "/tmp/result.csv")]
    (if (or (nil? drn-res) (= (count drn-res) 0))
      (do
        (timbre/info "当前节点未读取到数据，不执行csvw节点")
        (helpers/curr-node-done node-exec-info))
      (let [rows (vec (map #(vec (vals %)) drn-res))
            ;获取列信息
            headers (vec (map #(name %) (keys (first drn-res))))] 
        ;追加写入,不覆盖
        (with-open [wrt (io/writer target-file :append true)]
          (writer-csv-headers headers wrt)
          (csv/write-csv wrt rows)
          (timbre/info (str "已写入" (deref (:sync-count @task-execution-dict)) "条记录到文件:" target-file)))))))
