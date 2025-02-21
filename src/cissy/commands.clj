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
  "Task configuration json example"
  [options]
  (cond
    (:zip options) (do 
                     (println "#This is a simple zip format node task, for reference only")
                     (let [copy-to-bytes-fn (fn [is] (let [buffer (java.io.ByteArrayOutputStream.)]
                                                       (io/copy is buffer)
                                                       (.toByteArray buffer)))
                           ;; slurp reads as string
                           is (io/input-stream (io/resource "zip_task_demo.zip"))
                           file-path (helpers/write-files-to-desktop (copy-to-bytes-fn is) "zip_task_demo.zip")]
                       (println (str "File has been written to desktop:" file-path))))
    :else
    (do 
      (println "#This is a mysql sync demo configuration, for reference only")
      (println (slurp (io/resource "task_config.edn"))))
    ))

(defn -startj [config-edn]
  ; Parse task configuration
  (let [task-info-vec (loader/get-task-from-edn config-edn)
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
          (timbre/error "Error executing task" (.getMessage ex) ex))))
    (timbre/info "All tasks in the current task group have been completed")))

(defn valid-path?
  "docstring"
  [path]
  (not (str/blank? path)))

(defn start
  "Start task through configuration file"
  [options]
  ; Load and register nodes drn and dwn
  (when-not s/valid? (valid-path? (:config options))
                     (timbre/error "Error: start command requires a configuration file (-c)")
                     (System/exit 1))
  (let [config-path (:config options)
        config-edn (slurp config-path)]
    (require '[cissy.init :as init])
    (timbre/info "Execute task start command" options)
    (when-not (= (spec/valid-config-edn config-edn) :ok)
      (timbre/error "Task configuration edn format error, please check the configuration file")
      (System/exit 1))
    (-startj config-edn)))