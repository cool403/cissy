(ns single-node-test
  (:require [cissy.executions :as executions]
            [cissy.loader :as loader]
            [clojure.test :refer :all]
            [ding]))

(def task-config (str {
                       :datasource      {

                                         }
                       :task_group_name "单节点执行测试"
                       :nodes           "ding->;"
                       :sched_type      "once"
                       :ding            {
                                         :threads 1
                                         }
                       :tasks           [{
                                          :ding {

                                                 }
                                          }]
                       }))

(deftest single-node-test
  (testing "单节点执行测试"
    (let [task-info-lst (loader/get-task-from-json task-config)
          task-info (first task-info-lst)
          sched-info (:sched-info @task-info)
          new-task-execution-info (executions/new-task-execution-info)]
      (reset! new-task-execution-info (assoc @new-task-execution-info :task-info task-info))
      (executions/sched-task-execution sched-info new-task-execution-info)
      (is (= "done" (:curr-task-status @new-task-execution-info))))))
