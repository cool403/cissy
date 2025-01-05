(ns cissy.sched
  (:require
   [cissy.core :refer [process-node-chan-based]]
   [taoensso.timbre :as timbre]
   [cissy.executions :as executions]
   [cissy.registry :as register]
   [clojure.core.async :as async :refer [>! <! go chan buffer dropping-buffer]]))



(deftype ChanBasedSched []
  executions/TaskSched
  (get-task-sched-type [this] "exec_chan_based")
  (get-task-sched-name [this] "Channel基础执行")
  (sched-task-execution [this task-execution-info]
    (timbre/info "开始以Channel策略执行任务")
    (let [{task-info :task-info} @task-execution-info
          {node-graph :node-graph} @task-info
          node-channels (atom {}); 存储节点ID -> channel的映射
          node-monitor-channel (chan)]

      ;; 设置任务状态为运行中
      (reset! task-execution-info (assoc @task-execution-info :curr-task-status "ding"))

      ;; 为每个非root节点创建channel
      (doseq [node (:all-node-id-set node-graph)]
        (when-not (empty? (get (:parent-node-map node-graph) (:node-id node)))
          (swap! node-channels assoc (:node-id node)
                 (chan (dropping-buffer 1024)))))

      ;; 启动监控线程
      (go
        (timbre/info "启动一个监控线程，监控节点状态")
        (let [node-status-map (zipmap (vec (map :node-id (:all-node-id-set node-graph))) (repeat "ding"))]
          (loop [x node-status-map]
            (let [node-status (<! node-monitor-channel)
                  {node-id :node-id node-status :node-status} node-status]
              (timbre/info (str "收到节点状态:" node-id "状态:" node-status))
              ;合并状态
              (let [y (assoc x node-id node-status)]
                ;如果所有节点状态都为done，则任务完成
                (when (every? #(= % "done") (vals y))
                  (timbre/info "所有节点状态都为done，任务完成")
                  (reset! task-execution-info (assoc @task-execution-info :curr-task-status "done")))
                (recur y))))))

      ;; 启动所有节点的处理
      (doseq [node (:all-node-id-set node-graph)]
        (let [node-id (:node-id node)]
          (process-node-chan-based node-id
                                   task-execution-info
                                   node-channels
                                   node-graph
                                   node-monitor-channel)))
      ;; 等待任务完成或被中断
      (loop []
        (let [status (:curr-task-status @task-execution-info)]
          (cond
            ;; 任务完成
            (= status "done")
            nil
            ;; 任务被中断
            (= status "interrupted")
            (do
              (timbre/info "任务被外部中断")
              (reset! task-execution-info
                      (assoc @task-execution-info :curr-task-status "done")))
            ;; 继续等待
            :else
            (do
              (Thread/sleep 1000)
              (recur)))))

      ;; 记录执行结束时间
      (reset! task-execution-info
              (assoc @task-execution-info
                     :stop-time (System/currentTimeMillis)))

      (timbre/info (str "任务执行完成，耗时:"
                        (- (:stop-time @task-execution-info)
                           (:start-time @task-execution-info))
                        "毫秒")))))