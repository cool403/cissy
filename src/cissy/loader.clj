(ns cissy.loader
  ;;   (:import (cissy.task TaskInfo))
  (:require [cheshire.core :as json]
            [cissy.task :as task]
            [taoensso.timbre :as timbre]
            [cissy.sched :as sched]
            [cissy.registry :as registry]
            [cissy.const :as const]
            [clojure.string :as str])
  (:import (java.util ArrayList HashMap)))

;; (def demo-json "{\n    \"task_name\":\"demo\",\n    \"nodes\":\"demo->\",\n    \"demo\":{\n        \n    }\n}")

;解析json中的nodes 配置
(defn- parse-node-rel-str [s]
  (let [parts (str/split s #";")
        res (atom [])]                                      ; 按照分号切分
    (doseq [part parts]
      (let [[k v] (str/split part #"->")]                   ; 按照箭头切分
        (timbre/info k v)
        (if (empty? v)                                      ; 如果没有箭头后的值，视为nil
          (reset! res (conj @res (vector k nil)))
          (doseq [v1 (str/split v #",")]
            (reset! res (conj @res (vector k v1)))))))
    @res))                                                  ; 如果有值，按照逗号切分
(comment
  (parse-node-rel-str "a->b;a->c,d;e->"))

;//todo 初始化数据源配置
(defn- init-db-ins-from-config [db-config]
  nil)


;从 json 中解析任务
(defn get-task-from-json [^String task-json]
  ;json->keyword map,keyword map才能用(:name 方式访问)
  (timbre/info "开始解析任务配置" task-json)
  (let [task-map                                                     (json/parse-string task-json #(keyword %))
        {task-name  :task_name
         nodes      :nodes
         datasource :datasource} task-map
        node-grpah                                                   (task/->TaskNodeGraph (HashMap.) (HashMap.) (ArrayList.) (HashMap.))
        task-info                                                    (atom (task/->TaskInfo nil task-name nil (sched/->ExecutionOnceSched) node-grpah task-map))]
    ;解析节点配置nodes(a->b;)为节点对
    (doseq [[from-node-id to-node-id] (parse-node-rel-str nodes)]
      (cond
        (nil? to-node-id) (task/add-node-pair node-grpah (task/->TaskNodeInfo from-node-id nil) nil)
        :else (task/add-node-pair node-grpah (task/->TaskNodeInfo from-node-id nil) (task/->TaskNodeInfo to-node-id nil))))
    ;添加完成后构建tree
    ;; (prn node-grpah)
    (task/build-node-tree node-grpah)
    (timbre/info "解析节点关系完成")
    ;注册数据源
    (timbre/info "开始初始化数据源配置")
    (doseq [[db-sign db-config-map] datasource]
      (timbre/info "开始初始化db-sign的" db-sign "数据源")
      ;直接调用注册数据源，由register完成实例化和注册
      (registry/register-datasource db-sign db-config-map)
    (timbre/info "初始化数据源配置完成")
    (when (contains? (set (map #(:node-id %) (:all-node-id-set node-grpah))) const/DRN_NODE_NAME)
      (timbre/info "由于当前任务配置包含drn 节点，自动切换为ExecutionAlwaysSched 策略")
      (reset! task-info (assoc @task-info :sched-info (sched/->ExecutionAlwaysSched)))))
    task-info))



;测试
;; (def demo-json (slurp "/home/mawdx/Desktop/task_config.json"))
;; (get-task-from-json demo-json)