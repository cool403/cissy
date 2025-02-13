(ns db-task-test
  (:require [cissy.commands :as commands]
            [clojure.test :refer :all]
            [cissy.init :as init]))


(def task-config-str (str {
                           :task_group_name "mysql同步测试"
                           ;;数据库节点目前支持drn和dwn,drn负责加载数据,dwn负责写数据
                           :nodes           "drn->dwn;"
                           ;;执行模式，默认是一直执行，配置成once，只会执行一次任务就结束
                           ;;  :sched_type      "once"
                           :datasource      {
                                             :db1 {
                                                   :host     "localhost"
                                                   :dbname   "test1"
                                                   :password "123456"
                                                   :port     4000
                                                   :user     "root"
                                                   ;;dbtype 目前支持sqlite,mysql,oracle,postgresql
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
                           ;;自定义任务脚本时,需要这里显式声明,注意这里要写绝对路径
                           ;;也支持zip格式任务加载
                           ;;  :entry_script    ["xx.clj"]
                           ;;测试
                           :drn             {
                                             :from_db   "db1"
                                             :page_size 1000
                                             ;;一次启动多少个线程
                                             :threads   20
                                             }
                           :dwn             {
                                             :to_db   "db2"
                                             ;;  :page_size 1000
                                             :threads   40
                                             }
                           :tasks           [{
                                              ;;drn中的配置和外层的drn配置存在覆盖关系，优先使用子任务的配置
                                              :drn {
                                                    :from_table   "users"
                                                    ;;自定义sql同步时可设置这个选项
                                                    :sql_template "select * from users u order by id "
                                                    }
                                              :dwn {
                                                    :to_table "users"
                                                    :threads  42
                                                    }
                                              }]
                           }))

(deftest db-task-test
  (testing "数据同步"
    (commands/-startj task-config-str)
    (is (true? true))))
