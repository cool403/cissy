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


(defn _add-node-pair [from-node to-node d]
  (when (not (nil? from-node))
    (let [from-node-id (:node-id from-node)]
      (cond
        (nil? to-node) ()
        (contains? (get d from-node-id) (:node-id to-node)) (.add (get d from-node-id) from-node)
        :else (-> (java.util.ArrayList.)
                  (.add to-node)
                  ((fn [x] (.put d from-node-id x))))))))

;定义一个任务执行依赖
(defrecord TaskNodeGraph [child-node-map parent-node-map all-node-id-set task-node-tree]
  TaskNodeGraphDef
  (get-startup-nodes [this]
    (let [res (java.util.ArrayList.)]
      (doseq [it all-node-id-set]
        (let [parent-nodes (get parent-node-map (:node-id it))]
          (cond
            (nil? parent-nodes) (.add res it)
            (= 0 (count parent-nodes)) (.add res it))))
      res))
  ;刷新树结构,必须要节点添加完毕,到时候可以研究下一定要调用这个方法才能访问属性
  ;递归获取深度,思路：遍历所有节点判断每个节点的所有父节点是否都已经在depth<= tree_depth的树上，如果是，那么就绑定到当前深度上
  ;其他继续遍历,直至所有节点都被访问过
  (build-node-tree [this]
    (-> (.get-startup-nodes this)
        ))
  (add-node-pair [this from-node to-node]
    (_add-node-pair from-node to-node child-node-map)
    (_add-node-pair to-node from-node parent-node-map)
    (cond
      (not (nil? from-node)) (.add all-node-id-set from-node)
      (not (nil? to-node)) (.add all-node-id-set to-node)))
  (get-child-nodes [this node-id]
    (get child-node-map node-id))
  (get-parent-nodes [this node-id]
    (get parent-node-map node-id)))

;定义一个任务类型
(defrecord TaskInfo [^String task-id ^String task-name
                     ^String task-exec-type sched-info ^TaskNodeGraph node-graph task-config])