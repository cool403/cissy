(ns single-node-test
  (:require [cissy.executions :as executions]
            [cissy.loader :as loader]
            [clojure.test :refer :all]
            [ding]
            [cissy.commands :as commands]))

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
    (commands/-startj task-config)
    (is (true? true))))
