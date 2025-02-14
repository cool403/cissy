(ns zip-task-demo
  (:require [cissy.registry :refer [defnode]]
            [cissy.task :as task]))

(defnode rnd [node-exec-info]
  (let [{:keys [task-execution-info node-result-dict node-execution-dict]} @node-exec-info
        {:keys [task-info task-execution-dict]} @task-execution-info
        {:keys [task-idx task-name task-config node-graph]} @task-info
        {:keys [rnd]} task-config
        {:keys [thread-idx]} @node-execution-dict])
  ["hello world"])

; The parent node is either unique or empty
(defn- parent-node-id [node-graph node-id]
  (:node-id (first (task/get-parent-nodes node-graph node-id))))

(defnode wnd [node-exec-info]
  (let [{:keys [task-execution-info node-result-dict node-execution-dict]} @node-exec-info
        {:keys [task-info task-execution-dict]} @task-execution-info
        {:keys [task-idx task-name task-config node-graph]} @task-info
        {:keys [wnd]} task-config
        {:keys [thread-idx]} @node-execution-dict
        node-result-lst (get @node-result-dict (keyword (parent-node-id node-graph "wnd")))]
    (prn node-result-lst)))