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
  "获取写header的锁"
  []
  (loop []
    (if (compare-and-set! write-headers-lock false true)
      true
      (do
        (timbre/warn "获取写header的锁失败")
        (Thread/sleep 10)
        (recur))))))

(comment (defn- release-write-header-lock
  "释放写header的锁"
  []
  (reset! write-headers-lock false)))

;保证只有一个线程能写成功
(defn- writer-csv-headers [headers writers task-execution-dict thread-idx]
  ;避免后续线程再次写入header
  (let [writer-headers-idx (keyword (str "write-headers" thread-idx))]
    (when (nil? (writer-headers-idx @task-execution-dict))
      (try
        (loop []
          (if (compare-and-set! write-headers-lock false true)
            (when (nil? (writer-headers-idx @task-execution-dict))
              (csv/write-csv writers [headers])
              (reset! task-execution-dict (assoc @task-execution-dict writer-headers-idx true))
              (timbre/info (str "线程=" thread-idx "写入header成功")))
            (do
              (timbre/warn (str "线程=" thread-idx "获取写header的锁失败"))
              (Thread/sleep 10)
              (recur))))
        (finally
          ;释放写header的锁
          (reset! write-headers-lock false))))))

;; 写出到csv文件
(defnode csvw [^NodeExecutionInfo node-exec-info]
  (let [{task-execution-info :task-execution-info
         node-result-dict    :node-result-dict
         node-execution-dict :node-execution-dict} @node-exec-info
         thread-idx (get @node-execution-dict :thread-idx 0)
        {task-execution-dict :task-execution-dict} @task-execution-info
        drn-res (get @node-result-dict :drn)
        target-file (get-in (deref (:task-info @task-execution-info)) [:task-config :csvw :target_file] "/tmp/result.csv")]
    (if (or (nil? drn-res) (= (count drn-res) 0))
      (do
        (timbre/info (str "当前节点=" thread-idx "未读取到数据，不执行csvw节点"))
        (helpers/curr-node-done node-exec-info))
      (let [rows (vec (map #(vec (vals %)) drn-res))
            ;获取列信息
            headers (vec (map #(name %) (keys (first drn-res))))]
        ;追加写入,不覆盖
        (with-open [wrt (io/writer target-file :append true)]
          (writer-csv-headers headers wrt task-execution-dict thread-idx)
          (csv/write-csv wrt rows)
          (swap! (:sync-count @task-execution-dict) #(+ % (count rows)))
          (timbre/info (str "已写入" (deref (:sync-count @task-execution-dict)) "条记录到文件:" target-file)))))))
