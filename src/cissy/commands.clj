(ns cissy.commands 
  (:require
   [taoensso.timbre :as timbre]
   [cissy.loader :as loader]
   [cissy.sched :as sched]
   [cissy.executions :as executions]))

(defn demo
  "任务配置json样例"
  [options]
  )

(defn start
  "通过配置文件启动任务"
  [options]
  (timbre/info "执行任务启动命令" options)
  (when-let [{config-path :config} options]
    ;解析任务配置
    (let [task-info               (-> (slurp config-path)
                                      (loader/get-task-from-json))
          sched-info              (:sched-info @task-info)
          new-task-execution-info (executions/new-task-execution-info)]
      ;初始化执行上下文
      (reset! new-task-execution-info (assoc @new-task-execution-info :task-info task-info))
      ;; (prn (type sched-info))
      (.sched-task-execution sched-info new-task-execution-info))))