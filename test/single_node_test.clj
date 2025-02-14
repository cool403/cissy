(ns single-node-test
  (:require [cissy.executions :as executions]
            [cissy.loader :as loader]
            [clojure.test :refer :all]
            [ding]
            [cissy.commands :as commands]))

(def task-config (str {
                       :datasource      {

                                         }
                       :task_group_name "single node execution test"
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
  (testing "Single node execution test"
    (commands/-startj task-config)
    (is (true? true))))
