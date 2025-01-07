(ns simple-task-test
  (:require
   [cissy.executions :as executions]
   [cissy.loader :as loader]
   [clojure.test :as test]
   [ding :as ding]
   [dong :as dong]))

(def simple-task-json
  (str
   "{\"task_name\":\"hello windows\",
\"nodes\":\"ding->dong\",
\"datasources\":{
},
\"ding\":{
    \"threads\":\"3\"
},
\"dong\":{
    \"threads\":\"2\"
}}"))

(test/deftest simple-task-test
  (test/testing "Context of the test assertions"
    (let [task-info (loader/get-task-from-json simple-task-json)
          sched-info              (:sched-info @task-info)
          new-task-execution-info (executions/new-task-execution-info)]
      (reset! new-task-execution-info (assoc @new-task-execution-info :task-info task-info))
      (executions/sched-task-execution sched-info new-task-execution-info)
      (test/is (= "done" (:curr-task-status @new-task-execution-info))))))