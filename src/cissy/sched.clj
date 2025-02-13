(ns cissy.sched
  (:require
    [cissy.core :refer [process-node-chan-loop process-node-chan]]
    [taoensso.timbre :as timbre]
    [cissy.executions :as executions]
    [cissy.registry :as register]
    [clojure.core.async :as async :refer [>! <! go chan buffer dropping-buffer]]
    [clojure.string :as str])
  (:import (cissy.executions TaskExecutionInfo)))

(deftype ChanBasedSched []
  executions/TaskSched
  (get-task-sched-type [this] "exec_chan_based")
  (get-task-sched-name [this] "Channel based execution")
  (sched-task-execution [this task-execution-info]
    (timbre/info "Start executing task with Channel strategy")
    (let [{task-info :task-info task-execution-dict :task-execution-dict} @task-execution-info
          {node-graph :node-graph task-config :task-config} @task-info
          ; Get execution strategy, whether to execute once or always execute
          sched-type (:sched_type task-config)
          all-node-id-set (:all-node-id-set node-graph)
          node-channels (atom {})                           ; Store the mapping of node ID -> channel
          node-monitor-channel (chan)
          get-threads-fn (fn [node-id] (get-in @task-info [:task-config (keyword node-id) :threads] 1))
          node-thread-fn (fn [node-id] (vec (map #(str node-id "_" %) (range (get-threads-fn node-id)))))]

      ;; Set task status to running
      (reset! task-execution-info (assoc @task-execution-info :curr-task-status "ding"))

      ;; Create channel for each non-root node
      (doseq [node all-node-id-set]
        (when-not (empty? (get (:parent-node-map node-graph) (:node-id node)))
          (swap! node-channels assoc (:node-id node)
                 (chan (dropping-buffer 1024)))))

      ;; Start monitoring thread
      (go
        (timbre/info "Start a monitoring thread to monitor node status")
        ; comp function executes from right to left
        (let [node-status-map (zipmap (vec (flatten (map (comp node-thread-fn #(:node-id %)) all-node-id-set))) (repeat "ding"))]
          (loop [x node-status-map]
            (let [node-status (<! node-monitor-channel)
                  {node-id :node-id node-status :node-status thread-idx :thread-idx} node-status]
              (timbre/info (str "Received status of thread " thread-idx " of node " node-id ": " node-status))
              ; Merge status
              (let [y (assoc x (str node-id "_" thread-idx) node-status)
                    z (vals (filter #(str/starts-with? (key %) (str node-id "_")) y))]
                ; If the status of all threads of the node is done, mark the node status as done
                (when (every? #(= % "done") z)
                  (timbre/info (str "All thread statuses of node " node-id " are done, mark node status as done"))
                  (reset! task-execution-dict (assoc @task-execution-dict (keyword node-id) "done")))
                ; If the status of all nodes is done, the task is completed
                ;; (prn y)
                (when (every? #(= % "done") (vals y))
                  (timbre/info "All node statuses are done, task completed")
                  (reset! task-execution-info (assoc @task-execution-info :curr-task-status "done")))
                (recur y))))))

      ;; Start processing all nodes
      (doseq [node all-node-id-set]
        (let [node-id (:node-id node)]
          (if (and (not (nil? sched-type)) (= sched-type "once"))
            ; If sched_type is configured as once, execute once
            (do
              (timbre/info (str "Node " node-id " will only execute once"))
              (process-node-chan node-id
                                 task-execution-info
                                 node-channels
                                 node-graph
                                 node-monitor-channel))
            (process-node-chan-loop node-id
                                    task-execution-info
                                    node-channels
                                    node-graph
                                    node-monitor-channel))))

      ;; Wait for task completion or interruption
      (loop []
        (let [status (:curr-task-status @task-execution-info)]
          (cond
            ;; Task completed
            (= status "done")
            nil
            ;; Task interrupted
            (= status "interrupted")
            (do
              (timbre/info "Task interrupted externally")
              (reset! task-execution-info
                      (assoc @task-execution-info :curr-task-status "done")))
            ;; Continue waiting
            :else
            (do
              (Thread/sleep 1000)
              (recur)))))

      ;; Record end time of execution
      (reset! task-execution-info
              (assoc @task-execution-info
                :stop-time (System/currentTimeMillis)))

      (timbre/info (str "Task execution completed, time taken: "
                        (- (:stop-time @task-execution-info)
                           (:start-time @task-execution-info))
                        " milliseconds")))))