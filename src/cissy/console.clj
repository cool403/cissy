(ns cissy.console 
  (:require
   [cissy.helpers :as helpers]
   [cissy.registry :refer [defnode]]
   [cissy.task :as task]
   [taoensso.timbre :as timbre]))

(defnode console [node-exec-info]
  (let [{:keys [task-execution-info node-result-dict node-execution-dict]} @node-exec-info
        {:keys [task-info task-execution-dict]} @task-execution-info
        {:keys [task-idx task-name task-config node-graph]} @task-info
        {:keys [console]} task-config
        {:keys [thread-idx]} @node-execution-dict
        node-result-lst (get @node-result-dict (keyword (helpers/parent-node-id-fn node-graph "console")))]
    (timbre/info (str "task-name=" task-name ",thread-idx=" thread-idx ",node-result-lst=" node-result-lst))))