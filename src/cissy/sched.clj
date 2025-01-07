(ns cissy.sched
  (:require
   [cissy.core :refer [process-node-chan-based]]
   [taoensso.timbre :as timbre]
   [cissy.executions :as executions]
   [cissy.registry :as register]
   [clojure.core.async :as async :refer [>! <! go chan buffer dropping-buffer]]
   [clojure.string :as str]))



(deftype ChanBasedSched []
  executions/TaskSched
  (get-task-sched-type [this] "exec_chan_based")
  (get-task-sched-name [this] "Channel基础执行")
  (sched-task-execution [this task-execution-info]
    (timbre/info "开始以Channel策略执行任务")
    (let [{task-info :task-info task-execution-dict :task-execution-dict} @task-execution-info
          {node-graph :node-graph} @task-info
          all-node-id-set (:all-node-id-set node-graph)
          node-channels (atom {}); 存储节点ID -> channel的映射
          node-monitor-channel (chan)
          get-threads-fn (fn [node-id] (get-in @task-info [:task-config (keyword node-id) :threads] 1))
          node-thread-fn (fn [node-id] (vec (map #(str node-id "_" %) (range (get-threads-fn node-id)))))]
      ;; 设置任务状态为运行中
      (reset! task-execution-info (assoc @task-execution-info :curr-task-status "ding"))

      ;; 为每个非root节点创建channel
      (doseq [node all-node-id-set]
        (when-not (empty? (get (:parent-node-map node-graph) (:node-id node)))
          (swap! node-channels assoc (:node-id node)
                 (chan (dropping-buffer 1024)))))
      ;; 启动监控线程
      (go
        (timbre/info "启动一个监控线程，监控节点状态")
        ;comp函数从右到左执行的
        (let [node-status-map (zipmap (vec (flatten (map (comp node-thread-fn #(:node-id %)) all-node-id-set))) (repeat "ding"))]
          (loop [x node-status-map]
            (let [node-status (<! node-monitor-channel)
                  {node-id :node-id node-status :node-status thread-idx :thread-idx} node-status]
              (timbre/info (str "收到" node-id "第" thread-idx "个线程处理状态:" node-status))
              ;合并状态
              (let [y (assoc x (str node-id "_" thread-idx) node-status)
                    z (vals (filter #(str/starts-with? (key %) (str node-id "_")) y))]
                ;如果所有节点的线程任务状态为done，则这个节点标记为done
                (when (every? #(= % "done") z)
                  (timbre/info (str "节点" node-id "所有线程任务状态都为done，节点状态标记为done"))
                  (reset! task-execution-dict (assoc @task-execution-dict (keyword node-id) "done")))
                ;如果所有节点状态都为done，则任务完成
                ;; (prn y)
                (when (every? #(= % "done") (vals y))
                  (timbre/info "所有节点状态都为done，任务完成")
                  (reset! task-execution-info (assoc @task-execution-info :curr-task-status "done")))
                (recur y))))))

      ;; 启动所有节点的处理
      (doseq [node all-node-id-set]
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