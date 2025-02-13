(ns cissy.task
  (:import (java.lang String)
           (java.util ArrayList HashSet HashMap))
  (:require [clojure.set :as set]))

(defprotocol TaskNodeGraphDef
  (get-startup-nodes [this]
    "Get startup nodes")
  (build-node-tree [this]
    "Build graph")
  (add-node-pair [this from-node to-node]
    "Add node pair")
  (get-child-nodes [this node-id]
    "Get child node list")
  (get-parent-nodes [this node-id]
    "Get parent node list"))

; Define a node type
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

; Define a task execution dependency
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

  ; Refresh tree structure
  (build-node-tree [this]
    ; Initialize tree
    (dotimes [i 10]
      (.put task-node-tree i (ArrayList.)))
    ; Match depth, start-up-nodes, all direct child nodes of each layer
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

  ; Register node pair
  (add-node-pair [this from-node to-node]
    (_add-node-pair from-node to-node child-node-map)
    (_add-node-pair to-node from-node parent-node-map)
    (when (some? from-node)
      (.add ^HashSet all-node-id-set from-node))
    (when (some? to-node)
      (.add ^HashSet all-node-id-set to-node)))

  ; Get child node list
  ;;  (prn child-node-map)
  (get-child-nodes [this node-id]
    ;;  (prn child-node-map)
    (get child-node-map node-id #{}))

  ; Get parent node list
  (get-parent-nodes [this node-id]
    (get parent-node-map node-id #{})))

; Define a task type
(defrecord TaskInfo [^String task-id ^String task-name
                     ^String task-exec-type sched-info ^TaskNodeGraph node-graph task-config])

; Factory function to create TaskNodeGraph
(defn create-task-node-graph []
  (->TaskNodeGraph (HashMap.)
                   (HashMap.)
                   (HashSet.)                               ;; Use HashSet instead of ArrayList
                   (HashMap.)))