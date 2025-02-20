(ns xhs.get-posts
  (:require
   [cissy.executions :refer [NodeExecutionInfo]]
   [cissy.registry :refer [defnode]]
   [taoensso.timbre :as timbre]
   [xhs.initialization :refer [init-db]]))



; init get-posts url
; read people's profile page from database
(defnode get-posts [^NodeExecutionInfo node-exec-info]
  (timbre/info "start get-posts")
  (let [{:keys [task-execution-info node-result-dict node-execution-dict]} @node-exec-info
        {:keys [task-info task-execution-dict]} @task-execution-info
        {:keys [task-idx task-name task-config node-graph]} @task-info
        {:keys [get-posts]} task-config
        {:keys [thread-idx]} @node-execution-dict
        {:keys [seed_url db_file]} get-posts
        db-spec {:dbtype "sqlite" :dbname db_file}]
      ;as default first row is column row
    (timbre/info (str "thread-idx=" thread-idx ",seed_url=" seed_url ",db_file=" db_file))
    ;init db
    (init-db db-spec seed_url)))