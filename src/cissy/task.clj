(ns cissy.task #_{:clj-kondo/ignore [:syntax]}
    (:import [java.lang.String]))

(defprotocol TaskNodeGraphDef
  ;获取启动节点
  (get-startup-nodes [])
  ;构造graph
  (build-node-tree [])
  ;添加节点对
  (add-node-pair [from-node to-node])
  ;获取子节点列表
  (get-child-nodes [node-id])
  ;获取父节点列表
  (get-parent-nodes [node-id]))

;定义一个节点类型
(defrecord TaskNodeInfo [^String node-id ^String node-name])

;定义一个任务执行依赖
(deftype TaskNodeGraph [child-node-map parent-node-map all-node-id-set task-node-tree]
  TaskNodeGraphDef
  (get-startup-nodes [this]
    (let [res (java.util.ArrayList.)]
      (for [it all-node-id-set]
        (let [parent-nodes (get parent-node-map (:node-id it))]
          (cond (or (nil? parent-nodes) (= 0 (count parent-nodes)))
                (.add res it))))
      res))
  (build-node-tree [this])
  (add-node-pair [this from-node to-node])
  (get-child-nodes [this])
  (get-parent-nodes [this]))

;定义一个任务类型
(defrecord TaskInfo [^String task-id ^String task-name
                     ^String task-exec-type sched-info ^TaskNodeGraph node-graph task-config])