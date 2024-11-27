(ns cissy.task
  #_{:clj-kondo/ignore [:syntax]}
  (:import (java.lang.String)
           (java.util ArrayList))
  (:require [clojure.set :as set]))

(defprotocol TaskNodeGraphDef
  ;获取启动节点
  (get-startup-nodes [this])
  ;构造graph
  (build-node-tree [this])
  ;添加节点对
  (add-node-pair [this from-node to-node])
  ;获取子节点列表
  (get-child-nodes [this node-id])
  ;获取父节点列表
  (get-parent-nodes [this node-id]))

;定义一个节点类型
(defrecord TaskNodeInfo [^String node-id ^String node-name])


(defn _add-node-pair [from-node to-node d]
  (when (not (nil? from-node))
    (let [from-node-id (:node-id from-node)]
      (cond
        (nil? to-node) ()
        (contains? (get d from-node-id) (:node-id to-node)) (.add (get d from-node-id) from-node)
        :else (-> (ArrayList.)
                  (.add to-node)
                  ((fn [x] (.put d from-node-id x))))))))

;定义一个任务执行依赖
(defrecord TaskNodeGraph [child-node-map parent-node-map all-node-id-set task-node-tree]
  TaskNodeGraphDef
  (get-startup-nodes [this]
    (let [res (ArrayList.)]
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
    ;初始化tree
    (dotimes [i 10]
      (.put task-node-tree i (ArrayList.)))
    ;匹配深度,start-up-nodes,每层节点的所有直接子节点
    (loop [start-up-nodes (get-startup-nodes this)
           depth 0
           visited-nodes (atom #{})]
      (let [next-nodes (ArrayList.)]
        (when (> (count start-up-nodes) 0)
          (prn "hello" start-up-nodes)
          (doseq [tmp-node start-up-nodes
                  ;获取node-id
                  tmp-node-id (:node-id tmp-node)
                  ;获取父节点列表
                  parent-node-id-set (set (map #(:node-id %) (get-parent-nodes this tmp-node-id)))]
            (prn "---------------------")
            (prn "hello")
            ;已经被遍历过
            (cond
              (contains? @visited-nodes tmp-node-id) nil
              ;(set/superset? #{} nil) 启动节点是空的话，这个语句也是 true 的
              (set/superset? visited-nodes parent-node-id-set)
              (do
                ;注册节点
                (.add (.get task-node-tree depth) tmp-node)
                ;记录已访问节点
                (reset! visited-nodes (conj @visited-nodes tmp-node-id))
                ;记录下一层要访问的节点
                (.addAll next-nodes (get-child-nodes this tmp-node-id)))))
          ;递归
          (recur next-nodes (inc depth) visited-nodes)))
      ))
  ;注册节点对
  (add-node-pair [this from-node to-node]
    (_add-node-pair from-node to-node child-node-map)
    (_add-node-pair to-node from-node parent-node-map)
    (cond
      (not (nil? from-node)) (.add all-node-id-set from-node)
      (not (nil? to-node)) (.add all-node-id-set to-node)))
  ;获取子节点列表
  (get-child-nodes [this node-id]
    (get child-node-map node-id))
  ;获取父节点列表
  (get-parent-nodes [this node-id]
    (get parent-node-map node-id)))

;定义一个任务类型
(defrecord TaskInfo [^String task-id ^String task-name
                     ^String task-exec-type sched-info ^TaskNodeGraph node-graph task-config])