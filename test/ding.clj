(ns ding 
  (:require
   [cissy.registry :as register]))

(defn ding [task-node-execution-info]
  (Thread/sleep 2000)
   "ding! dong!"
  )

(register/regist-node-fun "ding" ding)