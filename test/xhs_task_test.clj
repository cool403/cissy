(ns xhs_task_test
  (:require [cissy.commands :as commands]
            [clojure.test :refer :all]))

(def zip-task-config-edn (str {:task_group_name "Zip Task Loading"
                               ;; Database nodes currently support drn and dwn, drn is responsible for loading data, dwn is responsible for writing data
                               :nodes           "get-posts->;"
                               :datasource      {}
                               :sched_type      "once"
                               ;; When customizing task scripts, it needs to be explicitly declared here, note that the absolute path should be written here
                               :entry_script    ["/home/mawdx/mywork/cissy/xhs"]
                               :tasks           [{:get-posts {
                                                              :db_file     "/home/mawdx/桌面/xhs1.db"
                                                              :seed_url    "https://chat.deepseek.com/"
                                                              ;:cookie_file "/home/mawdx/桌面/xhs.zip"
                                                              }}]}))


(deftest xhs-task-test
  (testing "xhs crawler"
    (commands/-startj zip-task-config-edn)
    (is (true? true))))
