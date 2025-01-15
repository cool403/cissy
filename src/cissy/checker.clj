(ns cissy.checker 
  (:require
   [clojure.string :as str]
   [clojure.spec.alpha :as s]
   [clojure.spec.gen.alpha :as gen]))


(defn validate-node-graph
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

(s/def ::host string?)
(s/def ::dbname string?)
(s/def ::password string?)
(s/def ::port int?)
(s/def ::user string?)
(s/def ::dbtype #{"sqlite" "mysql" "oracle" "postgresql"})
(s/def ::datasource (s/keys :req-un [::host ::dbname ::password ::port ::user ::dbtype]))
(s/def ::threads int?)
(s/def ::from_db string?)
(s/def ::page_size int?)
(s/def ::to_db string?)
(s/def ::from_table string?)
(s/def ::sql_template string?)
(s/def ::to_table string?)
(s/def ::drn (s/keys :req-un [::threads ::from_db ::page_size]))
(s/def ::dwn (s/keys :req-un [::to_db ::threads]))
(s/def ::task (s/keys :req-un [::drn ::dwn]))
(s/def ::task_group (s/keys :req-un [::task]))
(s/def ::config (s/keys :req-un [::datasource ::task_group]))

(defn valid-config-json
  "校验任务json格式"
   [config-json]
  (when-not (s/valid? ::config config-json)
      (s/explain ::config config-json)
    1
    ))