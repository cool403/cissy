(ns cissy.commands 
  (:require
   [taoensso.timbre :as timbre]
   [cissy.loader :as loader]))

(defn demo
  "任务配置json样例"
  [options]
  )

(defn start
  "通过配置文件启动任务"
  [options]
  (timbre/info "执行任务启动命令" options)
  (-> ())
  )