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
                               :task_group_name "csv export test"
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
  (testing "Test data writing to csv"
    ; Concurrently execute 10 tasks, each task's output file name + idx, task name + idx, check if the number of csv files is correct, and if the first line is the header
    ; Check if the number of lines in all csv files is correct
    ; Finally delete all files
    (let [future-vec (map #(future (commands/-startj (-> (edn/read-string csv-task-config-edn)
                                                         (assoc :task_group_name (str "csv export test" %))
                                                         (assoc-in [:tasks 0 :csvw :target_file] (str "/home/mawdx/桌面/demo" % ".csv"))
                                                         (str)))) (range thread-size))]
      (doseq [fut future-vec]
        (try
          (deref fut)
          (catch Exception ex
            (timbre/error "Error executing task" (.getMessage ex) ex)))))
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
            (timbre/info (str "Successfully deleted file /home/mawdx/桌面/demo" idx ".csv"))
            (catch Exception ex
              (timbre/error "Error deleting file" (.getMessage ex) ex)))))))))


(def csv-read-config-edn (str {:task_group_name "23223"
                               :nodes           "csvr->console;"
                               :sched_type      "once"
                               :datasource      {}
                               :tasks           [{
                                                  :csvr    {
                                                            :target_file "/home/mawdx/桌面/testdata.csv"
                                                            }
                                                  :console {

                                                            }
                                                  }]}))

(deftest csv-read-test
  (testing "load data from csv"
    (commands/-startj csv-read-config-edn)
    (is true)))
