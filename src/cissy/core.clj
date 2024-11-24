(ns cissy.core
  (:require
    ;; [cissy.executions :refer [TaskExecutionInfo]]
   [cissy.task :as task]
   [cissy.registry :as register]
   [taoensso.timbre :as timbre]))

(comment
  (defprotocol Human
    (age []))
  (deftype Jissy [name age]
    Human
    (age [this] age))
  (def j (->Jissy "张三" 23))
  (age j))

;to-future


;A----->B---------->F
;|                  |
; ----->C---->D------
;|            |
; ----->E------
(defn run-task-in-local
  "docstring"
  [task-execution-info]
  (timbre/info "start to get startup nodes for task")
  (let [{^task/TaskInfo task-info :task-info}   @task-execution-info
        {^task/TaskNodeGraph node-graph :node-graph} task-info
        startup-nodes            (task/get-startup-nodes node-graph)]
    (if (<= (count startup-nodes) 0) (timbre/warn "未匹配到启动节点")
        ;从深度遍历执行
        (loop [iter-nodes (get (:task-node-tree node-graph) 0) 
               depth      0]
          ;future-list 采集所有的future,用于结果处理
          (let [future-list (atom #{})]
            (when (> (count iter-nodes) 0)
              (for [tmp-node    iter-nodes
                    tmp-node-id (:node-id tmp-node)]
                (prn "ok"))
              (recur nil (inc depth))))))))