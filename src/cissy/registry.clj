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
  ;如果已经注册过,不再注册
  (when-not (contains? @task-node-register (keyword node-id))
    (timbre/info "start to register node-id ", node-id)
    (compare-and-set! task-node-register @task-node-register
                      (assoc @task-node-register (keyword node-id) func))))

;获取关联函数
(defn get-node-func [node-id]
  ;不包含注册函数,报错
  (if-not (contains? @task-node-register (keyword node-id))
    (throw (IllegalArgumentException. (str "没有发现node-id=" node-id "注册节点，请先注册节点")))
    (get @task-node-register (keyword node-id))))



(comment 
  (defn test-node []
    (prn "test node"))
  (regist-node-fun "test" test-node)
  ((get-node-func "test")))


;db register
(core/def datasource-ins-register (atom {}))

(defn register-datasource [^String db-sign datasource-config]
  ;注册一个数据源
  (when-not (contains? @datasource-ins-register (keyword db-sign))
    (prn "TBD"))
  )


(defn get-datasource-ins [^String db-sign]
  ;获取注册数据源
  (if-not (contains? @datasource-ins-register (keyword db-sign)) (throw (IllegalArgumentException. (str "未发现db-sign" db-sign "注册数据源")))
          (get @datasource-ins-register (keyword db-sign)))
  )

(comment (get-datasource-ins "21"))
