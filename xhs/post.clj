(ns xhs.post 
  (:require
   [cissy.executions :refer [NodeExecutionInfo]]
   [cissy.registry :refer [defnode]]
   [taoensso.timbre :as timbre]))

(defnode get-posts [^NodeExecutionInfo node-exec-info]
  (let [{:keys [task-execution-info node-result-dict node-execution-dict]} @node-exec-info
        {:keys [task-info task-execution-dict]} @task-execution-info
        {:keys [task-idx task-name task-config node-graph]} @task-info
        {:keys [get-posts]} task-config
        {:keys [thread-idx]} @node-execution-dict
        ;someone's profile page
        {:keys [seed_url]} get-posts]
        (timbre/info (str "task-name=" task-name ",thread-idx=" thread-idx ",seed_url=" seed_url))))