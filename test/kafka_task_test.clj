(ns kafka_task_test
  (:require [cissy.commands :as commands]
            [clojure.test :refer :all]))


(def kafka-config-edn (str {:task_group_name "kafka task demo"
                            :nodes           "krn->;"
                            :datasource      {
                                              :main {
                                                     :dbtype "kafka"
                                                     "bootstrap.servers" "localhost:9092"
                                                     "group.id" "cissy1"
                                                     }
                                              }
                            :entry_script    ["/home/mawdx/桌面/kafka.zip"]
                            :tasks           [{:krn {
                                                     :topic "test-topic"
                                                     :from_db "main"
                                                     }}]}))

(deftest kafka-task-test
  (testing "load data from kafka"
    (commands/-startj kafka-config-edn)
    (is (true? true))))
