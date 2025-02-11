(ns cissy.helpers
  (:require [cissy.executions :as executions]))


(defn my-merge-fn [m1 m2]
  ;自定义合并，存在于m1,也存在于m2,保留m1；不存在于m1,存在于m2,添加
  (reduce (fn [acc [k v]]
            (if (contains? acc k)
              acc
              (assoc acc k v)))
          m1
          m2))


(defn curr-node-done
  "当前节点标记完成"
  [task-node-execution-info]
  (if (or (nil? task-node-execution-info) (not (instance? clojure.lang.Atom task-node-execution-info)))
    (throw (IllegalArgumentException. "类型必须是atom类型，且不能为nil"))
    (cond (instance? cissy.executions.NodeExecutionInfo @task-node-execution-info)
          (reset! task-node-execution-info (assoc @task-node-execution-info :curr-node-status "done")))))