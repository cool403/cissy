(ns cissy.sched
  (:require
   [cissy.core :refer [run-task-in-local]]
   [taoensso.timbre :as timbre]
   [cissy.executions :as executions]))


;单次执行
(deftype ExecutionOnceSched []
  executions/TaskSched
  (get-task-sched-type [this] "exec_once")
  (get-task-sched-name [this] "执行一次")
  (sched-task-execution [this ^executions/TaskExecutionInfo task-execution-info]
    (timbre/info "开始执行单次任务")
    ((let [{task-info  :task-info} @task-execution-info]
       (timbre/info "开始执行单次任务" (:task-exec-type task-info))
      ;设置成ding
       (reset! task-execution-info (assoc @task-execution-info :curr-task-status "ding"))
       (run-task-in-local task-execution-info)
       (timbre/info "任务执行完成")
       (reset! task-execution-info 
               (assoc @task-execution-info :stop-time (System/currentTimeMillis)
                      :curr-task-status "done"))))))
;while true一直执行
(deftype ExecutionAlwaysSched []
  executions/TaskSched
  (get-task-sched-type [this] "exec_always")
  (get-task-sched-name [this] "循环执行")
  (sched-task-execution [this ^executions/TaskExecutionInfo task-execution-info]
    (timbre/info "开始以循环策略执行任务")
    (let [{task-info           :task-info 
           task-execution-dict :task-execution-dict} @task-execution-info]
      (timbre/info "获取任务启动节点: " (:task-id task-info))
       ;设置成ding
      (reset! task-execution-info (assoc @task-execution-info :curr-task-status "ding"))
      (loop [round 1] 
        (when-not (= (:curr-task-status @task-execution-info) "done")
           ;打印日志
          (timbre/info "开始执行 task=" (:task-id task-info) "in the 【" round "】round")
           ;执行轮数塞到执行上下文中
          (reset! task-execution-dict (assoc @task-execution-dict :execution-round round))
          ;执行方法
          (run-task-in-local task-execution-info)
          ;随机sleep避免空转
          (Thread/sleep (rand-int 200))
          (recur (inc round))))
      ;纪录执行结束时间
      (reset! task-execution-info (assoc @task-execution-info :stop-time (System/currentTimeMillis))))
    ))