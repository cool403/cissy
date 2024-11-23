(ns cissy.core
  (:require
   [cissy.executions :refer [TaskExecutionInfo]]
   [cissy.task :as task]
   [taoensso.timbre :as timbre]))

(comment
  (defprotocol Human
    (age []))
  (deftype Jissy [name age]
    Human
    (age [this] age))
  (def j (->Jissy "张三" 23))
  (age j))


;A----->B---------->F
;|                  |
; ----->C---->D------
;|            |
; ----->E------
(defn run-task-in-local
  "docstring"
  [^TaskExecutionInfo task-execution-info]
  (timbre/info "start to get startup nodes for task")
  (let [{^task/TaskInfo task-info  :task-info} @task-execution-info
        {^task/TaskNodeGraph node-graph :node-graph} task-info
        startup-nodes (get-startup-nodes node-graph)]
    (if (<= (count startup-nodes) 0) (timbre/warn "未匹配到启动节点")
        (for [startup-node startup-nodes]
          ;获取注册节点配置
          ()))))