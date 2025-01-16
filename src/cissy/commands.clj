(ns cissy.commands 
  (:require
   [taoensso.timbre :as timbre]
   [cissy.loader :as loader]
   [cissy.sched :as sched]
   [cissy.executions :as executions]
   [clojure.string :as str]
   [cissy.checker :as checker]
   [cissy.spec :as spec]
   [clojure.spec.alpha :as s])
  (:gen-class 
   :name cissy.commands
   :methods [#^{:static true} [startj [java.lang.String] void]
             #^{:static true} [demo [java.util.Map] void]]))

(defn -demo
  "任务配置json样例"
  [options]
  (println "#这是一个mysql同步demo 配置，仅供参考")
  (println (->> ["{"
                 "    \"datasource\": {"
                 "      \"db1\": {"
                 "        \"host\":  \"localhost\","
                 "        \"dbname\": \"test1\","
                 "        \"password\": \"123456\","
                 "        \"port\": 4002,"
                 "        \"user\": \"root\","
                 "		#dbtype 目前支持sqlite,mysql,oracle,postgresql"
                 "        \"dbtype\": \"mysql\""
                 "      },"
                 "      \"db2\": {"
                 "        \"host\": \"localhost\","
                 "        \"dbname\":\"test2\","
                 "        \"password\":\"123456\","
                 "        \"port\":4000,"
                 "        \"user\":\"root\","
                 "        \"dbtype\":\"mysql\""
                 "      }"
                 "    },"
                 "	\"task_group\":[{"
                 "	  \"task_group_name\":\"mysql同步测试\","
                 "	  #数据库节点目前支持drn和dwn,drn负责加载数据,dwn负责写数据"
                 "	  \"nodes\": \"drn->dwn;\","
                 "	  \"drn\": {"
                 "		#一次启动多少个线程"
                 "		\"threads\":20,"
                 "		\"from_db\":\"db1\","
                 "		\"page_size\": 2000"
                 "	  },"
                 "	  \"dwn\": {"
                 "		\"to_db\":\"db2\","
                 "		\"threads\":20"
                 "	  },"
                 "	  \"tasks\":[{"
                 "		#drn中的配置和外层的drn配置存在覆盖关系，有限使用内部的配置"
                 "		\"drn\": {"
                 "		  \"from_table\":\"users\","
                 "		  #自定义sql同步时可设置这个选项"
                 "		  \"sql_template\":\"select * from users1 order by id\""
                 "		},"
                 "		\"dwn\": {"
                 "		  \"to_table\":\"users\","
                 "		  \"threads\": 2"
                 "		}"
                 "	   }]"
                 "	}]"
                 "}"
                 ""]
            (str/join \newline))))
  
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
    (timbre/info (str "当前任务组全部执行完成"))))

(defn start
  "通过配置文件启动任务"
  [options]
  ;加载注册节点drn和dwn
  (let [config-path (:config options)]
    (if (nil? config-path)
      (timbre/error "Error: start 命令需要指定配置文件(-c)")
      (do 
        (require '[cissy.dbms.dbms-core :as dbms-core])
        (timbre/info "执行任务启动命令" options)
        (when-not (= (spec/valid-config-json (slurp config-path)) :ok)
          (timbre/error "任务配置json格式错误, 请检查配置文件")
          (System/exit 1))
        (-startj (slurp config-path))))))