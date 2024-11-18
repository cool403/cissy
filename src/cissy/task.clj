(ns cissy.task #_{:clj-kondo/ignore [:syntax]}
               (:import [java.lang.String]))

;定义一个节点类型
(defrecord TaskNodeInfo [^String node-id ^String node-name])

;定义一个任务执行依赖
(deftype TaskNodeGraph [child-node-map parent-node-map all-node-id-set task-node-tree] 

  ;获取启动节点
  Object
  (get-startup-nodes [this]
    (prn "hello"))
  )

;定义一个任务类型
(defrecord TaskInfo [^String task-id ^String task-name
                     ^String task-exec-type sched-info ^TaskNodeGraph node-graph task-config]
  )