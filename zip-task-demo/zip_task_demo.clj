(ns zip-task-demo
  (:require [cissy.registry :refer [defnode]]
            [cissy.task :as task]
            [cissy.helpers :as helpers]))

(defnode rnd [node-exec-info]
  (let [{:keys [task-execution-info node-result-dict node-execution-dict]} @node-exec-info
        {:keys [task-info task-execution-dict]} @task-execution-info
        {:keys [task-idx task-name task-config node-graph]} @task-info
        {:keys [rnd]} task-config
        {:keys [thread-idx]} @node-execution-dict])
  ["hello world"])

(defnode wnd [node-exec-info]
  (let [{:keys [task-execution-info node-result-dict node-execution-dict]} @node-exec-info
        {:keys [task-info task-execution-dict]} @task-execution-info
        {:keys [task-idx task-name task-config node-graph]} @task-info
        {:keys [wnd]} task-config
        {:keys [thread-idx]} @node-execution-dict
        node-result-lst (get @node-result-dict (keyword (helpers/parent-node-id-fn node-graph "wnd")))]
    (prn node-result-lst)))