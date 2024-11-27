(ns cissy.core
  (:require
    ;; [cissy.executions :refer [TaskExecutionInfo]]
   [cissy.task :as task]
   [cissy.executions :as executions]
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
(defn- fill-node-result-cxt [node-execution-info curr-node-id node-graph may-used-node-res]
  ;获取父节点列表
  #_{:clj-kondo/ignore [:missing-else-branch]}
  (if-let [parent-node-list (task/get-parent-nodes node-graph curr-node-id)]
    (doseq [parent-node parent-node-list
            parent-node-id (:node-id parent-node)
            parent-node-res (get (keyword parent-node-id) may-used-node-res)
            node-result-dict (:node-result-dict node-execution-info)]
      ;传入父节点的执行结果作为此次节点的执行依赖传入
      (timbre/info "当前节点" curr-node-id "依赖的父节点" parent-node-id "执行结果" parent-node-res)
      (reset! node-result-dict (assoc @node-execution-info (keyword parent-node-id) parent-node-res))))
  node-execution-info)


;A----->B---------->F
;|                  |
; ----->C---->D------
;|            |
; ----->E------
(defn run-task-in-local
  "docstring"
  [task-execution-info]
  (timbre/info "start to get startup nodes for task")
  (let [{^task/->TaskInfo task-info :task-info}   @task-execution-info
        {^task/->TaskNodeGraph node-graph :node-graph} task-info
        startup-nodes            (task/get-startup-nodes node-graph)]
    (if (<= (count startup-nodes) 0) (timbre/warn "未匹配到启动节点")
        ;从深度遍历执行
        (loop [depth             0
               may-used-node-res (atom {})]
          (timbre/info "开始迭代执行depth=" depth "节点列表")
          ;future-list 采集所有的future,用于结果处理
          (let [node-future-map (atom {})
                iter-nodes      (get (:task-node-tree node-graph) depth)]
            (when (> (count iter-nodes) 0)
              #_{:clj-kondo/ignore [:unused-value]}
              (for [tmp-node    iter-nodes
                    tmp-node-id (:node-id tmp-node)
                    ;获取注册的方法
                    node-func   (register/get-node-func tmp-node-id)]
                (let [node-future      (-> (executions/new-node-execution-info tmp-node-id task-execution-info)
                                           (fill-node-param tmp-node-id (:task-config task-info))
                                           (fill-node-result-cxt tmp-node-id node-graph may-used-node-res)
                                            ;转换成future,添加结果依赖
                                           #(future (node-func %)))]
                  ;future 保存
                  (reset! node-future-map (assoc @node-future-map (keyword tmp-node-id) node-future))))
              (timbre/info "当前depth=" depth "所有节点转换成future完成")
              ;通过future 获取结果集
              (doseq [[k v] @node-future-map]
                (let [v-res (deref v 60000 nil)]
                  (timbre/info "父节点node-id" k "执行完成")
                    ;记录执行结果
                  (reset! may-used-node-res (assoc @may-used-node-res k v-res))))))
          (recur (inc depth) may-used-node-res)))))