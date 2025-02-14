(ns zip-task-test
  (:require [cissy.commands :as commands]
            [cissy.executions :as executions]
            [cissy.loader :as loader]
            [clojure.test :refer :all]))

(def zip-task-config-edn (str {:task_group_name "Zip Task Loading"
                               ;; Database nodes currently support drn and dwn, drn is responsible for loading data, dwn is responsible for writing data
                               :nodes           "ding->;"
                               :datasource      {}
                               ;; When customizing task scripts, it needs to be explicitly declared here, note that the absolute path should be written here
                               :entry_script    ["/home/mawdx/Desktop/ding.zip"]
                               :tasks           [{:ding {}}]}))

(deftest zip-task-test
  (testing "Test zip format loading, including external dependency loading"
    (commands/-startj zip-task-config-edn)
    (is (true? true))))