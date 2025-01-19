(ns ding
  (:require
    [cissy.registry :as register]
    [taoensso.timbre :as timbre]
    [cissy.helpers :as helpers]))

(register/defnode ding [task-node-execution-info]
  (Thread/sleep 2000)
  (timbre/info "发送消息")
  (helpers/curr-node-done task-node-execution-info)
  ["ding! dong!"]
  )

