(ns spec-test
  (:require [clojure.spec.alpha :as s]
           [clojure.test :as test]
           [cissy.spec :as spec]))
(def data
  {:datasource
   {:db1
    {:host "localhost"
     :dbname "test1"
     :password "123456"
     :port 4002
     :user "root"
     :dbtype "mysql"}
    :db2
    {:host "localhost"
     :dbname "test2"
     :password "123456"
     :port 4000
     :user "root"
     :dbtype "mysql"}}
   :task_group
   [{:task_group_name "mysql同步测试"
     :nodes "drn->dwn;"
     :entry_script "/home/mawdx/Desktop/hello.clj"
     :drn
     {:threads 20
      :from_db "db1"
      :page_size 2000}
     :dwn
     {:to_db "db2"
      :threads 40}
     :tasks
     [{:drn
       {:from_table "users"
        :sql_template "select * from users1 order by id"}
       :dwn
       {:to_table "users"
        :threads 2}}]}]})

(test/deftest task-json-1
  (test/testing "验证正常的任务配置格式"
    (test/is (true? (s/valid? (s/get-spec 'cissy.spec/data) data)))))

