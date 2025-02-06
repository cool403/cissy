(ns zip-task-test
  (:require [cissy.executions :as executions]
            [cissy.loader :as loader]
            [clojure.test :refer :all]))

(def zip-task-config-edn (str {
                               :task_group_name "zip包任务加载"
                               ;;数据库节点目前支持drn和dwn,drn负责加载数据,dwn负责写数据
                               :nodes           "ding->;"
                               :datasource      {

                                                 }
                               ;;自定义任务脚本时,需要这里显式声明,注意这里要写绝对路径
                               :entry_script    ["/home/mawdx/桌面/ding.zip"]
                               :tasks           [{
                                                  :ding {

                                                         }
                                                  }]
                               }))

(deftest zip-task-test
  (testing "测试zip格式加载,包含外部依赖加载"
    (let [task-info-lst (loader/get-task-from-json zip-task-config-edn)
          task-info (first task-info-lst)
          sched-info (:sched-info @task-info)
          new-task-execution-info (executions/new-task-execution-info)]
      (reset! new-task-execution-info (assoc @new-task-execution-info :task-info task-info))
      (executions/sched-task-execution sched-info new-task-execution-info)
      (is (= "done" (:curr-task-status @new-task-execution-info))))))
