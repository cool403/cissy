(ns simple-task-test
  (:require
    [cissy.commands :as commands]
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
                       :task_group_name "simple task test"
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
  (test/testing "Simplest ding dong task test"
    ;(prn simple-task-json)
    (commands/-startj task-config)
    (test/is (= 1 1))))