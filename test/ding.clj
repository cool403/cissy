(ns ding
  (:require
    [cissy.registry :as register]
    [taoensso.timbre :as timbre]))

(defn ding [task-node-execution-info]
  (Thread/sleep 2000)
  (timbre/info "发送消息")
  (reset! task-node-execution-info (assoc @task-node-execution-info :curr-node-status "done"))
  ["ding! dong!"]
  )

(register/regist-node-fun "ding" ding)