(ns cissy.sched
  (:import (cissy.executions TaskExecutionInfo TaskSched)))



(deftype ExecutionOnceSched []
  TaskSched
  (get-task-sched-type [] "exec_once")
  (get-task-sched-name [] "执行一次")
  (sched-task-execution [^TaskExecutionInfo task-execution-info]
    ())
  )