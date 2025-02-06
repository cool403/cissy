(ns simple-task-test
  (:require
    [cissy.executions :as executions]
    [cissy.loader :as loader]
    [clojure.string :as str]
    [clojure.test :as test]
    [ding]
    [dong]))
(comment
  (def simple-task-json
    (str (->> ["{"
               "    \"datasource\": {"
               "    },"
               "	\"task_group\":[{"
               "		\"task_group_name\":\"hello windows\","
               "		\"nodes\": \"ding->dong;\","
               "		\"ding\": {"
               "			\"threads\":2"
               "		},"
               "		\"dong\": {"
               "			\"threads\":3"
               "		},"
               "		\"tasks\":["
               "			{"
               "				\"ding\": {"
               "					},"
               "				\"dong\": {"
               "				}"
               "			}"
               "		]}"
               "	]"
               "  }  "
               ""]
              (str/join \newline))))
  )

(def task-config (str {
                       :datasource      {

                                         }
                       :task_group_name "简单任务测试"
                       :nodes           "ding->dong;"
                       :sched_type      "once"
                       :ding            {
                                         :threads 1
                                         }
                       :dong            {
                                         :threads 1
                                         }
                       :tasks           [{
                                          :ding {

                                                 }
                                          :dong {

                                                 }
                                          }]
                       }))


(test/deftest simple-task-test
  (test/testing "最简单的ding dong任务测试"
    ;(prn simple-task-json)
    (let [task-info-lst (loader/get-task-from-json task-config)
          task-info (first task-info-lst)
          sched-info (:sched-info @task-info)
          new-task-execution-info (executions/new-task-execution-info)]
      (reset! new-task-execution-info (assoc @new-task-execution-info :task-info task-info))
      (executions/sched-task-execution sched-info new-task-execution-info)
      (test/is (= "done" (:curr-task-status @new-task-execution-info))))))