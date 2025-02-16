(ns sqlite-task-test
  (:require
    [cissy.commands :as commands]
    [clojure.edn :as edn]
    [clojure.java.io :as io]
    [clojure.string :as str]
    [clojure.test :refer :all]
    [cissy.init :as init]
    [taoensso.timbre :as timbre]))


(def sqlite-config-edn (str {:task_group_name "load data to sqlite from kafka"
                             :nodes           "krn->dwn;"
                             :datasource      {:main        {:dbtype             "kafka"
                                                             "bootstrap.servers" "localhost:9092"
                                                             "group.id"          "cissy1"}
                                               :sqlite-test {:dbtype "sqlite"
                                                             :dbname "/home/mawdx/sqlite-demo.db"}}
                             :entry_script    ["/home/mawdx/桌面/kafka.zip"]
                             :tasks           [{:krn {:topic   "test-topic"
                                                      :from_db "main"
                                                      :threads 5}
                                                :dwn {:threads 2
                                                      :to_db   "sqlite-test"
                                                      :to_table "Users"}}]}))

(deftest sqlite-task-test
  (testing "load data to sqlite from kafka"
    (commands/-startj sqlite-config-edn)
    (is (true? true))))
