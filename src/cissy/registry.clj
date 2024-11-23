(ns cissy.registry
  (:require
   [clojure.core :as core]
   [taoensso.timbre :as timbre])
  (:import java.lang.IllegalArgumentException))

;注册器
(core/def task-node-register (atom {}))
;注册atom listener
;; (add-watch )


(comment
  (defn a [x] (inc x))
  (reset! task-node-register (assoc @task-node-register :12 a))
  ((:12 @task-node-register) 12))


;注册task-node
(defn regist-node-fun [node-id func]
  "docstring"
  ;如果已经注册过,不再注册
  (when-not (contains? @task-node-register (keyword node-id))
    (timbre/info "start to register node-id ", node-id)
    (compare-and-set! task-node-register @task-node-register
                      (assoc @task-node-register (keyword node-id) func))))

;获取关联函数
(defn get-node-func [node-id]
  "docstring"
  ;不包含注册函数,报错
  (if-not (contains? @task-node-register (keyword node-id))
    (throw (IllegalArgumentException. (str "没有发现node-id=" node-id "注册节点，请先注册节点")))
    (get @task-node-register (keyword node-id))))



(comment 
  (defn test-node []
    (prn "test node"))
  (regist-node-fun "test" test-node)
  ((get-node-func "test")))