(ns cissy.commands 
  (:require
   [taoensso.timbre :as timbre]
   [cissy.loader :as loader]
   [cissy.sched :as sched]
   [cissy.executions :as executions]
   [clojure.string :as str]))

(defn demo
  "任务配置json样例"
  [options]
  (println "#这是一个mysql同步demo 配置，仅供参考")
  (println (->> [
                 "{"
                 "    \"task_name\": \"mysql同步测试\","
                 "    #数据库节点目前支持drn和dwn,drn负责加载数据,dwn负责写数据"
                 "    \"nodes\": \"drn->dwn;\","
                 "    \"datasource\": {"
                 "      \"db1\": {"
                 "        \"host\": \"localhost\","
                 "        \"dbname\":\"test2\","
                 "        \"password\":\"123456\","
                 "        \"port\":4000,"
                 "        \"user\":\"root\"," 
                 "         #dbtype 目前支持sqlite,mysql,oracle,postgresql"
                 "        \"dbtype\":\"mysql\""
                 "      },"
                 "      \"db2\": {"
                 "        \"host\": \"localhost\","
                 "        \"dbname\":\"test1\","
                 "        \"password\":\"123456\","
                 "        \"port\":4002,"
                 "        \"user\":\"root\","
                 "        \"dbtype\":\"mysql\""
                 "      }"
                 "    },"
                 "    \"drn\": {"
                 "      \"from_db\": \"db1\","
                 "      \"from_table\":\"users\","
                 "      \"incr_key\": \"updated_at\","
                 "      \"incr_key_value\": \"1900-01-01\","
                 "      #pg下这个不能调的太大，会报莫名奇怪的错误还没研究"
                 "      \"page_size\": 1000,"
                 "      #自定义sql同步时可设置这个选项"
                 "      \"sql_template\":\"select * from users\""
                 "    },"
                 "    \"dwn\": {"
                 "      \"to_db\": \"db2\","
                 "      \"to_table\":\"users1\""
                 "    }"
                 "  }  "] 
  
            (str/join \newline))))
  

(defn start
  "通过配置文件启动任务"
  [options]
  ;加载注册节点drn和dwn
  (require '[cissy.dbms.dbms-core :as dbms-core])
  (timbre/info "执行任务启动命令" options)
  (when-let [{config-path :config} options]
    ;解析任务配置
    (let [task-info-vec (-> (slurp config-path) loader/get-task-from-json)
          to-future-fn (fn [task-info]
                         (let [sched-info (:sched-info @task-info)
                               new-task-execution-info (executions/new-task-execution-info)]
                           (reset! new-task-execution-info (assoc @new-task-execution-info :task-info task-info))
                           (future (executions/sched-task-execution sched-info new-task-execution-info))))
          future-vec (map to-future-fn task-info-vec)]
      (doseq [fut future-vec]
        (try
          (deref fut)
          (timbre/info (str "当前任务组全部执行完成"))
          (catch Exception ex
            (timbre/error "执行任务出错" (.getMessage ex) ex))))
      )))