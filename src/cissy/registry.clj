(ns cissy.registry
  (:require
   [clojure.core :as core]
   [taoensso.timbre :as timbre]
   [cissy.const :as const])
  (:import java.lang.IllegalArgumentException))

;注册器
(core/def task-node-register (atom {}))
;注册atom listener
;; (add-watch )


(comment
  (defn a [x] (inc x))
  (reset! task-node-register (assoc @task-node-register :12 a))
  ((:12 @task-node-register) 12))


;注册task-node
(defn regist-node-fun [node-id func]
  ;如果已经注册过,不再注册
  (when-not (contains? @task-node-register (keyword node-id))
    (timbre/info "start to register node-id ", node-id)
    (compare-and-set! task-node-register @task-node-register
                      (assoc @task-node-register (keyword node-id) func))))

;获取关联函数
(defn get-node-func [node-id]
  ;不包含注册函数,报错
  (if-not (contains? @task-node-register (keyword node-id))
    (throw (IllegalArgumentException. (str "没有发现node-id=" node-id "注册节点，请先注册节点")))
    (get @task-node-register (keyword node-id))))



(comment 
  (defn test-node []
    (prn "test node"))
  (regist-node-fun "test" test-node)
  ((get-node-func "test")))


;db register
(core/def datasource-ins-register (atom {}))

;oracle, mysql, pg 都是这个格式;sqlite 特殊些
;约定sqlite文件还是host里，只是到时候特殊处理, dbtype枚举值: mysql,postgresql
;oracle
(comment (def db {:dbtype   "postgresql"
         :host     "your-db-host-name"
         :dbname   "your-db"
         :user     "develop"
         :password "develop"
         :port     5432}))
(defn register-datasource 
  "注册一个数据源"
  [^String db-sign datasource-config]
  (when-not (contains? @datasource-ins-register (keyword db-sign))
    ;不包含，首先实例化
    (if-let [_ (contains? const/SUPPORTED_DB_TYPE (:dbtype datasource-config))] 
      (cond 
        (= (:dbtype datasource-config) "sqlite") (reset! datasource-ins-register (assoc @datasource-ins-register (keyword db-sign) (:host datasource-config)))
        :else
        (reset! datasource-ins-register (assoc @datasource-ins-register (keyword db-sign) datasource-config)))
      (do
        (timbre/error "数据源配置必须有dbtype属性,且有效值类型只有oracle,mysql,sqlite,postgresql")
        (throw (IllegalArgumentException. "数据源配置必须有dbtype属性,且有效值类型只有oracle,mysql,sqlite,postgresql"))))))


(defn get-datasource-ins [^String db-sign]
  ;获取注册数据源
  (if-not (contains? @datasource-ins-register (keyword db-sign)) (throw (IllegalArgumentException. (str "未发现db-sign" db-sign "注册数据源")))
          (get @datasource-ins-register (keyword db-sign)))
  )

(comment (get-datasource-ins "21"))
;; (def db1 {:dbtype "postgresql", :host "localhost", :user "hello", :password "123456", :port 5432})
;; (register-datasource "from-db" db1)
;; (def db5 {:dbtype "sqlite", :host "/home/mawdx/mywork/jissy/jissy-tests/jissy.db"})
;; (register-datasource "db5" db5)
;; (def db6 {:dbtype "sqlit2e", :host "/home/mawdx/mywork/jissy/jissy-tests/jissy.db"})
;; (register-datasource "db6" db6)

(mysql-sql/insert-multi! aa :users ["id" "username"] [{:users/id 22222222,
  :users/username "njones"}
 {:users/id 22222223,
  :users/username "fzuniga"}])
