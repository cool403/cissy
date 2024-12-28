(ns cissy.executions
  (:import (java.util UUID)))


;任务执行中间变量
(defrecord TaskExecutionInfo [task-execution-id
                              task-info
                              start-time
                              stop-time
                              curr-task-status
                              task-param-dict
                              task-execution-dict])
;初始化一个任务执行变量
(defn new-task-execution-info [] (atom (->TaskExecutionInfo
                                         (UUID/randomUUID)
                                         nil
                                         (System/currentTimeMillis)
                                         nil
                                         "wait"
                                         (atom {})
                                         (atom {:sync-count (atom 0)}))))

;任务节点执行中间变量
(defrecord NodeExecutionInfo [node-execution-id
                              task-execution-info
                              node-id
                              curr-node-status
                              ;节点执行要的参���,初始化用
                              node-param-dict
                              ;依赖节点的执行结果
                              node-result-dict
                              ;执行上下文,临时变量区
                              node-execution-dict
                              start-time
                              stop-time])
;初始化任务节点执行中间变量
(defn new-node-execution-info
  "docstring"
  [node-id task-execution-info]
  (atom (->NodeExecutionInfo (UUID/randomUUID)
                             task-execution-info
                             node-id
                             "wait"
                             (atom {})
                             (atom {})
                             (atom {})
                             (System/currentTimeMillis)
                             (System/currentTimeMillis))))

(defprotocol TaskSched
  "任务执行调度接口定义"
  (get-task-sched-type [this]      "获取调度类型")
  (get-task-sched-name [this]      "获取调度名称")
  (sched-task-execution [this task-execution-info] "调度任务执行"))

