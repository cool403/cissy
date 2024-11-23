(ns cissy.registry)

;注册器
(def task-node-register (atom {}) )


(comment
  (defn a [x] (inc x))
  (reset! task-node-register (assoc @task-node-register :12 a))
  ((:12 @task-node-register) 12)
  )

