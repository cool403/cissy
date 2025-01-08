(ns dong 
  (:require
    [cissy.task :as task]
    [cissy.registry :as register]
    [taoensso.timbre :as timbre]))


(defn dong [task-node-execution-info]
   (let [{node-result-dict :node-result-dict} @task-node-execution-info]
     (timbre/info "收到消息" (:ding @node-result-dict)))
  )

(register/regist-node-fun "dong" dong)