(ns kafka-task-test
  (:require [cissy.commands :as commands]
            [clojure.test :refer :all]
            [cissy.init :as init]))


(def kafka-config-edn (str {:task_group_name "kafka task demo"
                            :nodes           "krn->kwn;"
                            :datasource      {
                                              :main {
                                                     :dbtype             "kafka"
                                                     "bootstrap.servers" "localhost:9092"
                                                     "group.id"          "cissy1"
                                                     }
                                              }
                            :entry_script    ["/home/mawdx/桌面/kafka.zip"]
                            :tasks           [{:krn {
                                                     :topic   "test-topic"
                                                     :from_db "main"
                                                     :threads 5
                                                     }
                                               :kwn {
                                                     :threads 1
                                                     :topic   "test-topic"
                                                     :to_db   "main"
                                                     }}]}))

(deftest kafka-task-test
  (testing "load data from kafka"
    (commands/-startj kafka-config-edn)
    (is (true? true))))
