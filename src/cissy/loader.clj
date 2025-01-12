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
        (timbre/info (str k "->" v))
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

(defn- validate-node-graph
  "验证节点图是否是合法的多叉树结构"
  [node-graph]
  (let [{:keys [parent-node-map all-node-id-set]} node-graph
        root-nodes (filter #(empty? (get parent-node-map (:node-id %))) all-node-id-set)]

    ;; 检查是否只有一个根节点
    ;; (prn root-nodes)
    ;; (prn all-node-id-set)
    (when (not= 1 (count root-nodes))
      (throw (IllegalArgumentException.
              (format "任务图必须有且仅有一个根节点，当前有 %d 个根节点: %s"
                      (count root-nodes)
                      (str/join "," (map :node-id root-nodes))))))

    ;; 检查每个节点是否只有一个父节点
    (doseq [node all-node-id-set]
      (let [parent-nodes (get parent-node-map (:node-id node))]
        (when (> (count parent-nodes) 1)
          (throw (IllegalArgumentException.
                  (format "节点 %s 有多个父节点: %s，每个节点只能有一个父节点"
                          (:node-id node)
                          (str/join "," (map :node-id parent-nodes))))))))

    ;; 检查是否有环
    (let [visited (atom #{})
          path (atom #{})
          detect-cycle (fn detect-cycle [node-id]
                         (when (contains? @path node-id)
                           (throw (IllegalArgumentException.
                                   (format "检测到环形依赖，涉及节点: %s"
                                           (str/join "->" (conj @path node-id))))))
                         (when-not (contains? @visited node-id)
                           (swap! path conj node-id)
                           (swap! visited conj node-id)
                           (let [child-nodes (get (:child-node-map node-graph) node-id)]
                             (when child-nodes
                               (doseq [child child-nodes]
                                 (detect-cycle (:node-id child)))))
                           (swap! path disj node-id)))]

      ;; 从根节点开始检测环
      (detect-cycle (:node-id (first root-nodes)))))
  true)

(defn my-merge-fn [m1 m2]
  ;自定义合并，存在于m1,也存在于m2,保留m1；不存在于m1,存在于m2,添加
  (reduce (fn [acc [k v]]
            (if (contains? acc k)
              acc
              (assoc acc k v)))
          m1
          m2))

(defn- comp-new-task-fn [task tasks-map]
  ;组合新的task
  (merge-with (fn [v1 v2] (my-merge-fn v1 v2)) task (dissoc tasks-map :tasks :task_group_name)))

(defn- get-tasks-map-fn [config-json]
  ;解析任务组
  (let [config-map (json/parse-string config-json #(keyword %))
        {task-group :task_group datasource :datasource} config-map
        task-map-vec (atom [])]
    (doseq [tasks-map task-group]
      (let [{task-group-name :task_group_name tasks :tasks} tasks-map]
        (doseq [task tasks idx (range 0 (count tasks))]
          (reset! task-map-vec (conj @task-map-vec (-> task
                                                       (comp-new-task-fn tasks-map)
                                                       (assoc :datasource datasource)
                                                       ;子任务名称先用一个简单的名称加任务序号替代吧，后续再优化
                                                       (assoc :task_name (str task-group-name "_" idx))))))))
    @task-map-vec))


(defn assemble-task-fn [task-map]
  ;构建task-info
  (let [{task-name  :task_name
         nodes      :nodes
         datasource :datasource} task-map
        node-graph                                                   (task/create-task-node-graph)
        task-info                                                    (atom (task/->TaskInfo nil task-name nil (sched/->ChanBasedSched) node-graph task-map))]
      ;解析节点配置nodes(a->b;)为节点对
    (doseq [[from-node-id to-node-id] (parse-node-rel-str nodes)]
      (cond
        (nil? to-node-id) (task/add-node-pair node-graph (task/->TaskNodeInfo from-node-id nil) nil)
        :else (task/add-node-pair node-graph (task/->TaskNodeInfo from-node-id nil) (task/->TaskNodeInfo to-node-id nil))))
      ;验证节点图结构
    (timbre/info "开始验证节点图结构")
      ;; (prn node-graph)
    (validate-node-graph node-graph)
    (timbre/info "节点图结构验证通过")
      ;添加完成后构建tree
      ;; (prn node-grpah)
    (task/build-node-tree node-graph)
    (timbre/info "解析节点关系完成")
      ;注册数据源
    (timbre/info "开始初始化数据源配置")
    (doseq [[db-sign db-config-map] datasource]
      (timbre/info "开始初始化db-sign的" db-sign "数据源")
        ;直接调用注册数据源，由register完成实例化和注册
      (registry/register-datasource db-sign db-config-map)
      (timbre/info "初始化数据源配置完成")
      (when (contains? (set (map #(:node-id %) (:all-node-id-set node-graph))) const/DRN_NODE_NAME)
        (timbre/info "由于当前任务配置包含drn 节点，自动切换为ExecutionAlwaysSched 策略")
        (reset! task-info (assoc @task-info :sched-info (sched/->ChanBasedSched)))))
    task-info))

;从 json 中解析任务
(defn get-task-from-json [^String task-json]
  ;json->keyword map,keyword map才能用(:name 方式访问)
  (timbre/info "开始解析任务配置" task-json)
  (map assemble-task-fn (get-tasks-map-fn task-json)))

;测试
;; (def demo-json (slurp "/home/mawdx/Desktop/task_config.json"))
;; (get-task-from-json demo-json)