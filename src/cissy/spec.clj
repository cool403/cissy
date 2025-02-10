(ns cissy.spec
  (:require
    [clojure.edn :as edn]
    [clojure.spec.alpha :as s]))

;; 定义数据库配置spec
(s/def ::host string?)
(s/def ::dbname string?)
(s/def ::password string?)
(s/def ::port int?)
(s/def ::user string?)
(s/def ::dbtype #{"sqlite" "mysql" "oracle" "postgresql"})

(s/def ::db-config
  (s/keys :req-un [::host ::dbname ::password ::port ::user ::dbtype]))

;; 定义datasource spec
(s/def ::datasource
  (s/map-of keyword? ::db-config))

;; 定义节点配置spec
(s/def ::threads (s/nilable int?))
(s/def ::from_db keyword?)
(s/def ::page_size int?)
(s/def ::to_db keyword?)
(s/def ::sql_template string?)
(s/def ::to_table string?)
(s/def ::order_by string?)
(s/def ::incr_key string?)
(s/def ::from_table string?)
(s/def ::incr_key_value string?)

(s/def ::drn
  (s/keys :opt-un [::threads ::sql_template ::order_by ::from_table ::incr_key ::incr_key_value ::page_size
                   ::from_db]))


(s/def ::dwn
  (s/keys :opt-un [::to_db ::to_table]))

(def ::target_file string?)
(s/def ::csvw
  (s/keys :req-un [::target_file]))

;; 定义任务组配置spec
(s/def ::task_group_name string?)
(s/def ::nodes string?)
(s/def ::entry_script (s/nilable (s/coll-of string?)))
;(s/def ::node-config (s/keys :opt-un [::threads]))
(s/def ::node-config (s/and (s/map-of keyword? (s/keys :opt-un [::threads]))
                            #(cond
                               (contains? % :drn) (s/valid? ::drn (get % :drn))
                               (contains? % :drn) (s/valid? ::dwn (get % :dwn))
                               (contains? % :csvw) (s/valid? ::csvw (get % :csvw))
                               :else true)))

;(s/def ::uni-node-config (s/nilable ::nodes-config))

;(s/def ::tg
;  (s/keys :req-un [::task_group_name ::nodes ::entry_script ::tasks]
;          :opt-un [::uni-node-config]))

;; 定义任务配置spec
;(s/def ::task
;  (s/and (s/map-of keyword? ::node-config)
;         #(cond
;            (contains? % :drn) (s/valid? ::drn (get % :drn))
;            (contains? % :drn) (s/valid? ::dwn (get % :dwn))
;            :else true)))

(s/def ::tasks
  (s/coll-of ::node-config))

(s/def ::sched_type #{"once" "always"})
;; 定义整个数据结构spec
(s/def ::task-json
  (s/keys :req-un [::datasource ::tasks ::nodes ::task_group_name]
          :opt-un [::entry_script ::node-config ::sched_type]))

(defn valid-config-json
  "docstring"
  [config-json]
  ;(let [config-map (json/parse-string config-json #(keyword %))]
  (let [config-map (edn/read-string config-json)]
    (if-not (s/valid? ::task-json config-map)
      (do
        (println (s/explain ::task-json config-map))
        :fail)
      :ok)))
