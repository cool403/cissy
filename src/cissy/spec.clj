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
(s/def ::from_db keyword?)
(s/def ::page_size int?)
(s/def ::to_db keyword?)
(s/def ::sql_template string?)
(s/def ::to_table string?)
(s/def ::order_by string?)
(s/def ::incr_key string?)
(s/def ::from_table string?)
(s/def ::incr_key_value string?)

(s/def ::drn
  (s/keys :opt-un [::threads ::sql_template ::order_by ::from_table ::incr_key ::incr_key_value ::page_size
                   ::from_db]))


(s/def ::dwn
  (s/keys :opt-un [::to_db ::to_table]))

;; 定义任务组配置spec
(s/def ::task_group_name string?)
(s/def ::nodes string?)
(s/def ::entry_script (s/nilable string?))
(s/def ::node-config (s/keys :opt-un [::threads]))
(s/def ::nodes-config (s/and (s/map-of keyword? ::node-config)
                             #(cond 
                                (contains? % :drn) (s/valid? ::drn (get % :drn))
                                (contains? % :drn) (s/valid? ::dwn (get % :dwn))
                                :else true)))

(s/def ::uni-node-config (s/nilable ::nodes-config))

(s/def ::tg
  (s/keys :req-un [::task_group_name ::nodes ::entry_script ::tasks]
          :opt-un [::uni-node-config]))

(s/def ::tasks 
  (s/coll-of ::task))

;; 定义任务配置spec
(s/def ::task
  (s/and (s/map-of keyword? ::node-config)
         #(cond
            (contains? % :drn) (s/valid? ::drn (get % :drn))
            (contains? % :drn) (s/valid? ::dwn (get % :dwn))
            :else true)))

;; 定义任务组数组spec
(s/def ::task_group (s/coll-of ::tg))

;; 定义整个数据结构spec
(s/def ::data
  (s/keys :req-un [::datasource ::task_group]))


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

(s/valid? ::data data)
(s/explain ::data data)