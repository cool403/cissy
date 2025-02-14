(ns db-task-test
  (:require [cissy.commands :as commands]
            [clojure.test :refer :all]
            [cissy.init :as init]))


(def task-config-str (str {
                           :task_group_name "mysql sync test"
                           ;; Database nodes currently support drn and dwn, drn is responsible for loading data, dwn is responsible for writing data
                           :nodes           "drn->dwn;"
                           ;; Execution mode, the default is always execute, configure to once, it will only execute the task once and then end
                           ;;  :sched_type      "once"
                           :datasource      {
                                             :db1 {
                                                   :host     "localhost"
                                                   :dbname   "test1"
                                                   :password "123456"
                                                   :port     4000
                                                   :user     "root"
                                                   ;; dbtype currently supports sqlite, mysql, oracle, postgresql
                                                   :dbtype   "mysql"
                                                   }
                                             :db2 {
                                                   :host     "localhost"
                                                   :dbname   "postgres"
                                                   :password "123456"
                                                   :port     5432
                                                   :user     "postgres"
                                                   :dbtype   "postgresql"
                                                   }
                                             }
                           ;; When customizing task scripts, you need to explicitly declare here, note that you need to write the absolute path here
                           ;; Also supports zip format task loading
                           ;;  :entry_script    ["xx.clj"]
                           ;; Test
                           :drn             {
                                             :from_db   "db1"
                                             :page_size 1000
                                             ;; Number of threads to start at one time
                                             :threads   20
                                             }
                           :dwn             {
                                             :to_db   "db2"
                                             ;;  :page_size 1000
                                             :threads   40
                                             }
                           :tasks           [{
                                              ;; The configuration in drn and the outer drn configuration have an override relationship, the subtask configuration takes precedence
                                              :drn {
                                                    :from_table   "users"
                                                    ;; When customizing sql sync, you can set this option
                                                    :sql_template "select * from users u order by id "
                                                    }
                                              :dwn {
                                                    :to_table "users"
                                                    :threads  42
                                                    }
                                              }]
                           }))

(deftest db-task-test
  (testing "Data sync"
    (commands/-startj task-config-str)
    (is (true? true))))
