(ns task-config-edn-test
  (:require [clojure.test :as test]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.spec.alpha :as s]
            [cissy.spec :as spec]))


(def config-path "task_config.edn")

(test/deftest task-config-edn-test
  (test/testing "Test edn format configuration loading"
    (let [cfg (edn/read-string (slurp (io/resource config-path)))]
      (println cfg)
      (println (type cfg))
      (println (first (:tasks cfg)))
      (println (prn-str cfg))
      (test/is (not (nil? cfg))))))


(def aa {
         :task_group_name "mysql sync test"
         :nodes           "drn->dwn;"
         :datasource      {
                           :db1 {
                                 :host     "localhost"
                                 :dbname   "test1"
                                 :password "123456"
                                 :port     4002
                                 :user     "root"
                                 ;; dbtype currently supports sqlite, mysql, oracle, postgresql
                                 :dbtype   "mysql"
                                 }
                           :db2 {
                                 :host     "localhost"
                                 :dbname   "test2"
                                 :password "123456"
                                 :port     4000
                                 :user     "root"
                                 :dbtype   "mysql"
                                 }
                           }

         ;; Test
         :drn             {
                           :from_db   "db1"
                           :page_size 1000
                           :threads   20
                           }
         :dwn             {
                           :from_db   "db1"
                           :page_size 1000
                           :threads   20
                           }
         :tasks           [{
                            :drn {
                                  :from_table   "users"
                                  :sql_template "select * from users1 order by id"
                                  }
                            :dwn {
                                  :to_table "users"
                                  :threads  2
                                  }
                            }]
         })

(test/deftest test-valid-config
  (test/testing "Test if the task configuration meets the format requirements"
    (test/is (= :ok (spec/valid-config-json (prn-str aa))))))
