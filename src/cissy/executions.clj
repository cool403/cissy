(ns cissy.executions
  (:import (java.util UUID)))

; Task execution intermediate variable
(defrecord TaskExecutionInfo [task-execution-id
                              task-info
                              start-time
                              stop-time
                              curr-task-status
                              task-param-dict
                              task-execution-dict])
; Initialize a task execution variable
(defn new-task-execution-info [] (atom (->TaskExecutionInfo
                                         (UUID/randomUUID)
                                         nil
                                         (System/currentTimeMillis)
                                         nil
                                         "wait"
                                         (atom {})
                                         (atom {:sync-count (atom 0)}))))

; Task node execution intermediate variable
(defrecord NodeExecutionInfo [node-execution-id
                              task-execution-info
                              node-id
                              curr-node-status
                              ; Parameters required for node execution, initialized
                              node-param-dict
                              ; Execution results of dependent nodes
                              node-result-dict
                              ; Execution context, temporary variable area
                              node-execution-dict
                              start-time
                              stop-time])
; Initialize task node execution intermediate variable
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
  "Task execution scheduling interface definition"
  (get-task-sched-type [this] "Get scheduling type")
  (get-task-sched-name [this] "Get scheduling name")
  (sched-task-execution [this task-execution-info] "Schedule task execution"))

