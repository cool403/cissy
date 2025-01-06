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
            node-result-dict (:node-result-dict @node-execution-info)]
        (timbre/info "当前节点" curr-node-id "依赖的父节点" parent-node-id "返回" (if (counted? may-used-node-res)
                                                                        (str (count may-used-node-res) "条纪录")
                                                                        may-used-node-res))
        (reset! node-result-dict (assoc @node-execution-info (keyword parent-node-id) may-used-node-res)))))
  node-execution-info)

(defn- fill-thread-info [node-execution-info thread-idx round]
  ;; 填充线程信息
  ;; thread-idx: 线程索引
  ;; round: 执行轮次
  (let [node-execution-dict (:node-execution-dict @node-execution-info)]
    (reset! node-execution-dict (assoc @node-execution-dict :thread-idx thread-idx :execution-round round)))
  node-execution-info)

(defn process-node-chan-based
  "处理基于Channel的节点执行"
  [node-id task-execution-info node-channels node-graph node-monitor-channel]
  (let [{task-info :task-info} @task-execution-info
        node-func (register/get-node-func node-id)
        node-chan (get @node-channels node-id)
        child-nodes (get (:child-node-map node-graph) node-id)
        child-chans (map #(get @node-channels (:node-id %)) child-nodes)
        thread-count (or (get-in @task-info [:task-config (keyword node-id) :threads]) 1)
        curr-offset (atom 0)]

    ;; 创建指定数量的工作线程
    (dotimes [thread-idx thread-count]
      (let [thread-node-execution (-> (executions/new-node-execution-info node-id task-execution-info)
                                      (fill-node-param node-id (:task-config @task-info)))]
        (go
          ;设置节点状态为ding
          (reset! thread-node-execution (assoc @thread-node-execution :curr-node-status "ding"))
          (loop [round 1]
            (when-not (= (:curr-task-status @task-execution-info) "done")
              (timbre/info (str "为节点" node-id "创建第" thread-idx "个线程，执行轮次" round))
                      ;; 更新执行信息
              (let [curr-node-execution (-> thread-node-execution
                                            (fill-thread-info thread-idx round))
                    curr-node-status (:curr-node-status @curr-node-execution)
                            ;; 如果存在calc-page-offset函数，计算新的offset
                    node-param-dict (:node-param-dict @curr-node-execution)]
                (>! node-monitor-channel {:node-id node-id :node-status curr-node-status
                                          :thread-idx thread-idx})
                        ;; 如果有offset计算函数，更新page_offset
                (when (contains? @node-param-dict :page_size)
                  (let [page-size (get @node-param-dict :page_size 1000)]
                    (swap! curr-offset + page-size)
                    (reset! node-param-dict
                            (assoc @node-param-dict :page_offset @curr-offset))
                    (timbre/info "当前thread-index=" thread-idx "的取到的offset=" (get @node-param-dict :page_offset))))
                (if node-chan
                  ;; 非root节点等待输入
                  (when-let [parent-result (<! node-chan)]
                    (when-not (= curr-node-status "done")
                      (timbre/info (str "节点" node-id "获取到父节点结果"))
                      (let [result (-> curr-node-execution
                                       (fill-node-result-cxt node-id node-graph parent-result)
                                       node-func)]
                        (doseq [ch child-chans]
                          (>! ch result)))
                      (recur (inc round))))
                          ;; root节点执行
                  (do
                    (timbre/info (str "开始启动root节点" node-id))
                    (when-not (= curr-node-status "done")
                      (let [result (node-func curr-node-execution)]
                        (doseq [ch child-chans]
                          (>! ch result)))
                      (recur (inc round)))))))))))))
