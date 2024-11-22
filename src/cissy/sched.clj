(ns cissy.sched
  (:import (cissy.executions TaskExecutionInfo TaskSched))
  (:require [taoensso.timbre :as timbre]))



(deftype ExecutionOnceSched []
  TaskSched
  (get-task-sched-type [this] "exec_once")
  (get-task-sched-name [this] "执行一次")
  (sched-task-execution [this ^TaskExecutionInfo task-execution-info]
    (timbre/info "execute task info local mod")
    ((let [{task-info  :task-info
            start-time :start-time} @task-execution-info]
       (timbre/info "start to get startup nodes for " (:task-exec-type task-info))
       )))
  )