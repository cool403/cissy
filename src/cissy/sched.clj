(ns cissy.sched
  (:require
   [cissy.core :refer [run-task-in-local]]
   [taoensso.timbre :as timbre])
  (:import
   (cissy.executions TaskExecutionInfo TaskSched)))



(deftype ExecutionOnceSched []
  TaskSched
  (get-task-sched-type [this] "exec_once")
  (get-task-sched-name [this] "执行一次")
  (sched-task-execution [this ^TaskExecutionInfo task-execution-info]
    (timbre/info "start to execute task in once policy")
    ((let [{task-info  :task-info start-time :start-time} @task-execution-info
           {node-graph :node-graph} task-info]
       (timbre/info "start to get startup nodes for " (:task-exec-type task-info))))))

(deftype ExecutionAlwaysSched []
  TaskSched
  (get-task-sched-type [this] "exec_always")
  (get-task-sched-name [this] "循环执行")
  (sched-task-execution [this ^TaskExecutionInfo task-execution-info]
    (timbre/info "start to execute task in always policy")
    (let [{task-info           :task-info 
           start-time          :start-time 
           task-execution-dict :task-execution-dict} @task-execution-info
          {node-graph :node-graph}                                                                 task-info]
      (timbre/info "start to get startup nodes for " (:task-exec-type task-info))
       ;设置成ding
      (reset! task-execution-info (assoc @task-execution-info :curr-task-status "ding"))
      (loop [round 1] 
        (when-not (= (:curr-task-status @task-execution-info) "done")
           ;打印日志
          (timbre/info "start to run task=" (:task-name task-info) "in the " round "round")
           ;执行轮数塞到执行上下文中
          (reset! task-execution-dict (assoc @task-execution-dict :execution-round round))
          ;执行方法
          (run-task-in-local task-execution-info)
          ;随机sleep避免空转
          (Thread/sleep (rand-int 200))
          (recur (inc round)))))
    ))