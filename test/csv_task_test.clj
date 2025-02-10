(ns csv-task-test
  (:require [cissy.executions :as executions]
            [cissy.loader :as loader]
            [clojure.test :refer :all]
            [cissy.init :as init]))

(def csv-task-config-edn (str {
                               :task_group_name "csvm导出测试"
                               :nodes           "drn->csvw;"
                               :datasource      {
                                                 :db1 {
                                                       :host     "localhost"
                                                       :dbname   "test1"
                                                       :password "123456"
                                                       :port     4000
                                                       :user     "root"
                                                       :dbtype   "mysql"
                                                       }
                                                 }
                               :drn             {
                                                 :from_db   "db1"
                                                 :page_size 10000
                                                 :threads   5
                                                 }
                               :tasks           [{
                                                  :drn  {
                                                         :from_table   "users"
                                                         :sql_template "select * from users u order by id "
                                                         }
                                                  :csvw {
                                                         :target_file "/home/mawdx/桌面/demo1.csv"
                                                         :threads 20
                                                         }
                                                  }]
                               }))


(deftest csv-task-test
  (testing "测试数据写到csv中"
    (let [task-info-lst (loader/get-task-from-json csv-task-config-edn)
          task-info (first task-info-lst)
          sched-info (:sched-info @task-info)
          new-task-execution-info (executions/new-task-execution-info)]
      (reset! new-task-execution-info (assoc @new-task-execution-info :task-info task-info))
      (executions/sched-task-execution sched-info new-task-execution-info)
      (is (= "done" (:curr-task-status @new-task-execution-info))))))