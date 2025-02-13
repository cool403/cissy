(ns cissy.checker
  (:require
    [cheshire.core :as json]
    [clojure.spec.alpha :as s]
    [clojure.string :as str]))

(defn validate-node-graph
  "Validate whether the node graph is a legal multi-way tree structure"
  [node-graph]
  (let [{:keys [parent-node-map all-node-id-set]} node-graph
        root-nodes (filter #(empty? (get parent-node-map (:node-id %))) all-node-id-set)]

    ;; Check if there is only one root node
    ;; (prn root-nodes)
    ;; (prn all-node-id-set)
    (when (not= 1 (count root-nodes))
      (throw (IllegalArgumentException.
               (format "The task graph must have exactly one root node, currently there are %d root nodes: %s"
                       (count root-nodes)
                       (str/join "," (map :node-id root-nodes))))))

    ;; Check if each node has only one parent node
    (doseq [node all-node-id-set]
      (let [parent-nodes (get parent-node-map (:node-id node))]
        (when (> (count parent-nodes) 1)
          (throw (IllegalArgumentException.
                   (format "Node %s has multiple parent nodes: %s, each node can only have one parent node"
                           (:node-id node)
                           (str/join "," (map :node-id parent-nodes))))))))

    ;; Check for cycles
    (let [visited (atom #{})
          path (atom #{})
          detect-cycle (fn detect-cycle [node-id]
                         (when (contains? @path node-id)
                           (throw (IllegalArgumentException.
                                    (format "Cycle detected, involving nodes: %s"
                                            (str/join "->" (conj @path node-id))))))
                         (when-not (contains? @visited node-id)
                           (swap! path conj node-id)
                           (swap! visited conj node-id)
                           (let [child-nodes (get (:child-node-map node-graph) node-id)]
                             (when child-nodes
                               (doseq [child child-nodes]
                                 (detect-cycle (:node-id child)))))
                           (swap! path disj node-id)))]

      ;; Start detecting cycles from the root node
      (detect-cycle (:node-id (first root-nodes)))))
  true)