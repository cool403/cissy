(ns cissy.task
  (:import (java.lang String)
           (java.util ArrayList HashSet HashMap))
  (:require [clojure.set :as set]))

(defprotocol TaskNodeGraphDef
  (get-startup-nodes [this]
    "获取启动节点")
  (build-node-tree [this]
    "构造graph")
  (add-node-pair [this from-node to-node]
    "添加节点对")
  (get-child-nodes [this node-id]
    "获取子节点列表")
  (get-parent-nodes [this node-id]
    "获取父节点列表"))

;定义一个节点类型
(defrecord TaskNodeInfo [^String node-id ^String node-name])

(defn- _add-node-pair [from-node to-node d]
  (when (not (nil? from-node))
    (let [from-node-id (:node-id from-node)]
      (cond
        (nil? to-node) ()
        (.containsKey d from-node-id)
        (do
          ;; (prn "11111" from-node-id to-node)
          (.put d from-node-id (conj (get d from-node-id) to-node)))
        :else
        (do
          ;; (prn "22222" from-node-id to-node)
          (.put d from-node-id (conj #{} to-node)))))))

;定义一个任务执行依赖
(defrecord TaskNodeGraph [child-node-map parent-node-map all-node-id-set task-node-tree]
  TaskNodeGraphDef
  (get-startup-nodes [this]
    (let [res (ArrayList.)]
      (doseq [it all-node-id-set]
        (let [parent-nodes (get parent-node-map (:node-id it))]
          (cond
            (nil? parent-nodes) (.add res it)
            :else ())))
      res))

  ;刷新树结构
  (build-node-tree [this]
    ;初始化tree
    (dotimes [i 10]
      (.put task-node-tree i (ArrayList.)))
    ;匹配深度,start-up-nodes,每层节点的所有直接子节点
    (loop [start-up-nodes (get-startup-nodes this)
           depth 0
           visited-nodes (atom #{})]
      (let [next-nodes (ArrayList.)]
        (when (> (.size start-up-nodes) 0)
          (doseq [tmp-node start-up-nodes]
            (let [tmp-node-id (:node-id tmp-node)
                  parent-node-id-set (set (map #(:node-id %) (get-parent-nodes this tmp-node-id)))]
              (cond
                (contains? @visited-nodes tmp-node-id) nil
                (or (= 0 (count parent-node-id-set)) (set/superset? @visited-nodes parent-node-id-set))
                (do
                  (.add (.get task-node-tree depth) tmp-node)
                  (reset! visited-nodes (conj @visited-nodes tmp-node-id))
                  (.addAll next-nodes (get-child-nodes this tmp-node-id))))))
          (recur next-nodes (inc depth) visited-nodes)))))


  ;; (prn "Ok")


  ;; (prn "Ok")

  ;注册节点对
  (add-node-pair [this from-node to-node]
    (_add-node-pair from-node to-node child-node-map)
    (_add-node-pair to-node from-node parent-node-map)
    (when (some? from-node)
      (.add ^HashSet all-node-id-set from-node))
    (when (some? to-node)
      (.add ^HashSet all-node-id-set to-node)))

  ;获取子节点列表
  ;;  (prn child-node-map)
  (get-child-nodes [this node-id]
    ;;  (prn child-node-map)
    (get child-node-map node-id #{}))

  ;获取父节点列表
  (get-parent-nodes [this node-id]
    (get parent-node-map node-id #{})))

;定义一个任务类型
(defrecord TaskInfo [^String task-id ^String task-name
                     ^String task-exec-type sched-info ^TaskNodeGraph node-graph task-config])

;创建 TaskNodeGraph 的工厂函数
(defn create-task-node-graph []
  (->TaskNodeGraph (HashMap.)
                   (HashMap.)
                   (HashSet.)                               ;; 使用 HashSet 替代 ArrayList
                   (HashMap.)))