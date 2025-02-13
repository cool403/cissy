(ns cissy.commands
  (:require
    [cissy.executions :as executions]
    [cissy.loader :as loader]
    [cissy.spec :as spec]
    [clojure.spec.alpha :as s]
    [clojure.string :as str]
    [taoensso.timbre :as timbre]
    [clojure.java.io :as io]
    [cissy.helpers :as helpers])
  (:gen-class
    :name cissy.commands
    :methods [#^{:static true} [startj [java.lang.String] void]
              #^{:static true} [demo [java.util.Map] void]]))

(defn -demo
  "任务配置json样例"
  [options]
  (cond
    (:zip options) (do 
                     (println "#这是一个简单的zip格式节点任务，仅供参考")
                     (let [copy-to-bytes-fn (fn [is] (let [buffer (java.io.ByteArrayOutputStream.)]
                                                       (io/copy is buffer)
                                                       (.toByteArray buffer)))
                           ;;slurp 是读取成string的
                           is (io/input-stream (io/resource "ding.zip"))
                           file-path (helpers/write-files-to-desktop (copy-to-bytes-fn is) "ding.zip")]
                       (println (str "文件已经写入到桌面:" file-path))))
    :else
    (do 
      (println "#这是一个mysql同步demo 配置，仅供参考")
      (println (slurp (io/resource "task_config.edn"))))
    ))

(defn -startj [config-json]
  ;解析任务配置
  (let [task-info-vec (loader/get-task-from-json config-json)
        to-future-fn (fn [task-info]
                       (let [sched-info (:sched-info @task-info)
                             new-task-execution-info (executions/new-task-execution-info)]
                         (reset! new-task-execution-info (assoc @new-task-execution-info :task-info task-info))
                         (future (executions/sched-task-execution sched-info new-task-execution-info))))
        future-vec (map to-future-fn task-info-vec)]
    (doseq [fut future-vec]
      (try
        (deref fut)
        (catch Exception ex
          (timbre/error "执行任务出错" (.getMessage ex) ex))))
    (timbre/info "当前任务组全部执行完成")))

(defn valid-path?
  "docstring"
  [path]
  (not (str/blank? path)))

(defn start
  "通过配置文件启动任务"
  [options]
  ;加载注册节点drn和dwn
  (when-not s/valid? (valid-path? (:config options))
                     (timbre/error "Error: start 命令需要指定配置文件(-c)")
                     (System/exit 1))
  (let [config-path (:config options)
        config-edn (slurp config-path)]
    (require '[cissy.init :as init])
    (timbre/info "执行任务启动命令" options)
    (when-not (= (spec/valid-config-json config-edn) :ok)
      (timbre/error "任务配置json格式错误, 请检查配置文件")
      (System/exit 1))
    (-startj config-edn)))