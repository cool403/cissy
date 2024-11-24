(ns cissy.core
  (:require
    ;; [cissy.executions :refer [TaskExecutionInfo]]
   [cissy.task :as task]
   [cissy.executions :as exeuctions]
   [cissy.registry :as register]
   [clojure.string :as str]
   [taoensso.timbre :as timbre]))

(def DB_SUFFIX_KEY "_db")

(comment
  (defprotocol Human
    (age []))
  (deftype Jissy [name age]
    Human
    (age [this] age))
  (def j (->Jissy "张三" 23))
  (age j))

;填充执行参数
(defn- fill-node-param [node-execution-info curr-node-id task-config]
  (let [node-rel-config ((keyword curr-node-id) task-config)
        db-keys (filter #(str/ends-with? % DB_SUFFIX_KEY) (keys node-rel-config))]
    (doseq [db-key db-keys
            db-ref-key (get node-rel-config db-key)
            db-ins (register/get-datasource-ins db-ref-key)]
      (timbre/info "node=" curr-node-id "依赖数据源配置:" db-ref-key "添加")
      (-> (:node-param-dict node-execution-info)
          (#(reset! % (assoc (deref %) (keyword db-ref-key) db-ins))))))
  node-execution-info)

;填充执行结果集
(defn- fill-node-result-cxt [node-execution-info]
  ())


;A----->B---------->F
;|                  |
; ----->C---->D------
;|            |
; ----->E------
(defn run-task-in-local
  "docstring"
  [task-execution-info]
  (timbre/info "start to get startup nodes for task")
  (let [{^task/TaskInfo task-info :task-info}   @task-execution-info
        {^task/TaskNodeGraph node-graph :node-graph} task-info
        startup-nodes            (task/get-startup-nodes node-graph)]
    (if (<= (count startup-nodes) 0) (timbre/warn "未匹配到启动节点")
        ;从深度遍历执行
        (loop [depth      0]
          (timbre/info "开始迭代执行depth=" depth "节点列表")
          ;future-list 采集所有的future,用于结果处理
          (let [future-list (atom #{})
                iter-nodes  (get (:task-node-tree node-graph) depth)]
            (when (> (count iter-nodes) 0)
              (for [tmp-node    iter-nodes
                    tmp-node-id (:node-id tmp-node)]
                (-> (exeuctions/new-node-execution-info tmp-node-id task-execution-info)
                    ()))
              (recur (inc depth))))))))