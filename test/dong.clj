(ns dong 
  (:require
    [cissy.task :as task]
    [cissy.registry :as register]))


(defn dong [task-node-execution-info]
   (let [{node-result-dict :node-result-dict} @task-node-execution-info]
     (prn (:ding @node-result-dict)))
  )

(register/regist-node-fun "dong" dong)