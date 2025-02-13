(ns csv-task-test
  (:require
    [cissy.commands :as commands]
    [clojure.edn :as edn]
    [clojure.java.io :as io]
    [clojure.string :as str]
    [clojure.test :refer :all]
    [cissy.init :as init]
    [taoensso.timbre :as timbre]))

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
                                                 :page_size 100000
                                                 :threads   5
                                                 }
                               :tasks           [{
                                                  :drn  {
                                                         :from_table   "users"
                                                         :sql_template "select * from users u order by id "
                                                         }
                                                  :csvw {
                                                         :target_file "/home/mawdx/桌面/demo1.csv"
                                                         :threads     20
                                                         }
                                                  }
                                                 {
                                                  :drn  {
                                                         :from_table   "orders"
                                                         :page_size    100
                                                         :sql_template "select * from orders"
                                                         }
                                                  :csvw {
                                                         :target_file "/home/mawdx/桌面/orders.csv"
                                                         :threads     7
                                                         }
                                                  }]
                               }))

(def thread-size 1)

(deftest csv-task-test
  (testing "测试数据写到csv中"
    ;并发执行10个任务，每个任务的输出文件名+idx，任务名+idx，检查csv数量是否正确，第一行是否是header
    ;检查所有csv文件行数是否正确
    ;最后删除所有文件
    (let [future-vec (map #(future (commands/-startj (-> (edn/read-string csv-task-config-edn)
                                                         (assoc :task_group_name (str "csvm导出测试" %))
                                                         (assoc-in [:tasks 0 :csvw :target_file] (str "/home/mawdx/桌面/demo" % ".csv"))
                                                         (str)))) (range thread-size))]
      (doseq [fut future-vec]
        (try
          (deref fut)
          (catch Exception ex
            (timbre/error "执行任务出错" (.getMessage ex) ex)))))
    (let [header-vec (map #(-> (slurp %)
                               (str/split-lines)
                               first) (map #(str "/home/mawdx/桌面/demo" % ".csv") (range thread-size)))
          lines-vec (map #(-> (slurp %)
                              (str/split-lines)
                              count) (map #(str "/home/mawdx/桌面/demo" % ".csv") (range thread-size)))]
      (when (and (is (every? #(str/starts-with? % "email,first_name") header-vec))
                 (is (apply = lines-vec)))
        (timbre/info (str "lines-vec:" (first lines-vec)))
        (comment (doseq [idx (range thread-size)]
          (try
            (io/delete-file (str "/home/mawdx/桌面/demo" idx ".csv"))
            (timbre/info (str "删除文件/home/mawdx/桌面/demo" idx ".csv 成功"))
            (catch Exception ex
              (timbre/error "删除文件出错" (.getMessage ex) ex)))))))))
