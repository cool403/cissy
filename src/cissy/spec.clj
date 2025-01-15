(ns cissy.spec
  (:require [clojure.spec.alpha :as s]
            [clojure.string :as string]))

;; 定义数据库配置spec
(s/def ::host string?)
(s/def ::dbname string?)
(s/def ::password string?)
(s/def ::port int?)
(s/def ::user string?)
(s/def ::dbtype #{"sqlite" "mysql" "oracle" "postgresql"})

(s/def ::db-config
  (s/keys :req-un [::host ::dbname ::password ::port ::user ::dbtype]))

;; 定义datasource spec
(s/def ::datasource
  (s/map-of keyword? ::db-config))

;; 定义节点配置spec
(s/def ::threads (s/nilable int?))
(s/def ::from-db keyword?)
(s/def ::page-size int?)
(s/def ::to-db keyword?)
(s/def ::sql-template string?)
(s/def ::to-table string?)
(s/def ::order-by string?)
(s/def ::incr-key string?)
(s/def ::from-table string?)
(s/def ::incr-key-value string?)

(s/def ::drn
  (s/nilable (s/keys :opt-un [::threads ::sql-template ::order-by ::from-table ::incr-key ::incr-key-value]
          :req-un [::from-db ::page-size])))


(s/def ::dwn
  (s/nilable (s/keys :req-un [::to-table ::to-db])))

;; 定义任务组配置spec
(s/def ::task-group-name string?)
(s/def ::nodes string?)
(s/def ::entry-script (s/nilable string?))
(s/def ::nodes-config (s/map-of keyword? ::node-config))

(s/def ::task-group
  (s/keys :req-un [::task-group-name ::nodes ::entry-script ::tasks]))

(s/def ::tasks 
  (s/coll-of ::task))

;; 定义任务配置spec
(s/def ::task
  (s/map-of keyword?
            (s/keys :req-un [::from-db ::sql-template ::to-table ::threads])))

;; 定义任务组数组spec
(s/def ::task-group-vec (s/coll-of ::task-group))

;; 定义整个数据结构spec
(s/def ::data
  (s/keys :req-un [::datasource]
          :req [::task-group-vec]))


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
   :task-group
   [{:task_group_name "mysql同步测试"
     :nodes "drn->dwn;"
     :entry_script "/home/mawdx/Desktop/hello.clj"
     :nodes-config
     {:drn
      {:threads 20
       :from_db "db1"
       :page_size 2000}
      :dwn
      {:to_db "db2"
       :threads 40}}
     :tasks
     [{:drn
       {:from_table "users"
        :sql_template "select * from users1 order by id"}
       :dwn
       {:to_table "users"
        :threads 2}}]}]})

(s/valid? ::data data)
(s/explain ::data data)