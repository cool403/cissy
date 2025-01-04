(ns cissy.core
  (:require
    ;; [cissy.executions :refer [TaskExecutionInfo]]
   [cissy.task :as task]
   [cissy.executions :as executions]
   [cissy.registry :as register]
   [clojure.string :as str]
   [taoensso.timbre :as timbre]
   [cissy.const :as const]
   [clojure.core.async :refer [>! <! go chan]]))

;; (comment
;;   (defprotocol Human
;;     (age []))
;;   (deftype Jissy [name age]
;;     Human
;;     (age [this] age))
;;   (def j (->Jissy "张三" 23))
;;   (age j))

;填充执行参数
(defn- fill-node-param [node-execution-info curr-node-id task-config]
  (let [node-rel-config ((keyword curr-node-id) task-config)
        db-keys (filter #(str/ends-with? % const/DB_SUFFIX_KEY) (keys node-rel-config))]
    ;把所有node-rel-config当做param传入到node-param-dict中
    (doseq [db-key db-keys]
      (let [db-ref-key (get node-rel-config db-key)
            db-ins (register/get-datasource-ins (keyword db-ref-key))]
        (timbre/info "node=" curr-node-id "依赖数据源配置:" db-ref-key "添加" db-ins)
        (-> (:node-param-dict @node-execution-info)
            (#(reset! % (assoc (deref %) (keyword db-ref-key) db-ins))))))
    (reset! (:node-param-dict @node-execution-info) (merge (deref (:node-param-dict @node-execution-info)) node-rel-config)))
  node-execution-info)

;填充执行结果集
(defn- fill-node-result-cxt [node-execution-info curr-node-id node-graph may-used-node-res]
  ;获取父节点列表
  #_{:clj-kondo/ignore [:missing-else-branch]}
  (when-let [parent-node-list (task/get-parent-nodes node-graph curr-node-id)]
    (doseq [parent-node parent-node-list]
      ;传入父节点的执行结果作为此次节点的执行依赖传入
      (let [parent-node-id (:node-id parent-node)
            parent-node-res (get @may-used-node-res (keyword parent-node-id))
            node-result-dict (:node-result-dict @node-execution-info)]
        (timbre/info "当前节点" curr-node-id "依赖的父节点" parent-node-id "返回" (if (counted? parent-node-res)
                                                                        (str (count parent-node-res) "条纪录")
                                                                        parent-node-res))
        (reset! node-result-dict (assoc @node-execution-info (keyword parent-node-id) parent-node-res)))))
  node-execution-info)

(defn- fill-thread-info [node-execution-info thread-idx round]
  ;; 填充线程信息
  ;; thread-idx: 线程索引
  ;; round: 执行轮次
  (let [node-execution-dict (:node-execution-dict @node-execution-info)]
    (reset! node-execution-dict (assoc @node-execution-dict :thread-idx thread-idx :execution-round round))))


;A----->B---------->F
;|                  |
; ----->C---->D------
;|            |
; ----->E------
;; (defn run-task-in-local
;;   "docstring"
;;   [task-execution-info]
;;   (timbre/info "开始获取任务启动节点列表")
;;   ;; (prn (:node-graph (deref (:task-info @task-execution-info))))
;;   (let [{^task/->TaskInfo task-info :task-info}   @task-execution-info
;;         {^task/->TaskNodeGraph node-graph :node-graph} @task-info
;;         startup-nodes            (task/get-startup-nodes node-graph)]
;;     (if (<= (count startup-nodes) 0) (timbre/warn "未匹配到启动节点")
;;         ;从深度遍历执行
;;         (loop [depth             0
;;                may-used-node-res (atom {})]
;;           (timbre/info "开始迭代执行depth=" depth "节点列表")
;;           ;future-list 采集所有的future,用于结果处理
;;           (let [node-future-map (atom {})
;;                 iter-nodes      (get (:task-node-tree node-graph) depth)]
;;             ;; (prn (count iter-nodes))
;;             ;; (prn iter-nodes)
;;             (when (= (count iter-nodes) 0)
;;               (timbre/info (str "depth=" depth "没有需要执行的节点了")))
;;             (when (> (count iter-nodes) 0)
;;               #_{:clj-kondo/ignore [:unused-value]}
;;               (doseq [tmp-node    iter-nodes]
;;                 ;; (prn node-func)
;;                 (let [tmp-node-id (:node-id tmp-node)
;;                       node-func   (register/get-node-func (str tmp-node-id)) ;获取注册的方法
;;                       tmp-node-execution-info (-> (executions/new-node-execution-info tmp-node-id task-execution-info)
;;                                                   (fill-node-param tmp-node-id (:task-config @task-info))
;;                                                   (fill-node-result-cxt tmp-node-id node-graph may-used-node-res))
;;                                                    ;方法执行转换成future
;;                       node-future             (future (node-func tmp-node-execution-info))]
;;                   ;future 保存
;;                   (reset! node-future-map (assoc @node-future-map (keyword tmp-node-id) node-future))))
;;               (timbre/info "当前depth=" depth "所有节点转换成future完成")
;;               ;通过future 获取结果集
;;               ;; (prn @node-future-map)
;;               (doseq [[k v] @node-future-map]
;;                 (let [v-res (deref v 60000 nil)]
;;                   (timbre/info "父节点node-id" k "执行完成")
;;                   ;记录执行结果
;;                   (reset! may-used-node-res (assoc @may-used-node-res k v-res))))
;;               (recur (inc depth) may-used-node-res)))))))

(defn process-node-chan-based
  "处理基于Channel的节点执行
   node-id: 节点ID
   task-execution-info: 任务执行信息
   node-channels: 节点channel映射
   node-graph: 节点图"
  [node-id task-execution-info node-channels node-graph]
  (let [{task-info :task-info} @task-execution-info
        node-func (register/get-node-func node-id)
        node-chan (get @node-channels node-id)
        child-nodes (get (:child-node-map node-graph) node-id)
        child-chans (map #(get @node-channels (:node-id %)) child-nodes)
        thread-count (or (get-in @task-info [:task-config (keyword node-id) :threads]) 1)]

    ;; 创建指定数量的工作线程
    (dotimes [thread-idx thread-count]
      ;; (timbre/info (str "为节点" node-id "创建第" thread-idx "个线程"))
      (go
        (loop [round 1]
          (timbre/info (str "为节点" node-id "创建第" thread-idx "个线程，执行轮次" round))
          (let [node-execution-info (-> (executions/new-node-execution-info node-id task-execution-info)
                                                         ;; 填充节点参数
                                            (fill-node-param node-id (:task-config @task-info))
                                                           ;填充执行线程信息
                                            (fill-thread-info thread-idx round))]
            (if node-chan 
              (when-let [parent-result (<! node-chan)]
                (timbre/info (str "节点" node-id "获取到父节点结果"))
                (fill-node-result-cxt node-execution-info node-id node-graph parent-result)
                (let [result (node-func node-execution-info)]
                  (doseq [ch child-nodes]
                    (>! ch result))))
              (do 
                (timbre/info (str "开始启动root节点" node-id))
                (let [result (node-func node-execution-info)]
                  (doseq [ch child-chans]
                    (>! ch result))))))
          (recur (inc round)))))))
