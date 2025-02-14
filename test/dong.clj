(ns dong
  (:require
    [cissy.task :as task]
    [cissy.registry :as register]
    [taoensso.timbre :as timbre]))

(register/defnode dong [task-node-execution-info]
  (let [{node-result-dict :node-result-dict} @task-node-execution-info]
    (timbre/info "Message received" (:ding @node-result-dict)))
  )
