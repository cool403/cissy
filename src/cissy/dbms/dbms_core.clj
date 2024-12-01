(ns cissy.dbms.dbms-core
  (:require [cissy.registry :as register]
            [cissy.dbms.dialect :as dialect]
            [pod.babashka.mysql :as mysql]
            [pod.babashka.oracle :as oracle]
            [pod.babashka.postgresql :as pg]
            [pod.babashka.go-sqlite3 :as sqlite]
            [taoensso.timbre :as timbre]))

(defn- fill-page-params
  "填充分页参数"
  [task-execution-info node-param-dict]
  (let [{task-execution-dict :task-execution-dict} task-execution-info
        ;默认1
        execution-round (get @task-execution-dict :execution-round 1)
        ;默认 1000
        page-size (get @node-param-dict :page_size 1000)
        page-offset (* page-size (- execution-round 1))]
    (reset! node-param-dict (assoc @node-param-dict :page_offset page-offset))))

;从数据库读取数据
(defn read-rows
  "从数据库加载读取数据"
  [task-node-execution-info]
  (let [{task-execution-info :task-execution-info
         node-param-dict :node-param-dict
         node-execution-dict :node-execution-dict} task-node-execution-info
        {from-db :from_db} node-param-dict
        ;获取关联数据源配置
        from-db-ins (register/get-datasource-ins from-db)
        ;获取db类型
        db-type (if (map? from-db-ins) (:dbtype from-db-ins) "sqlite")]
    ;塞入db类型,sqlite的时候，塞入的不是json，而是字符串
    (reset! node-param-dict (assoc @node-param-dict :dbtype db-type))
    ;塞入分页相关的参数
    (fill-page-params task-execution-info node-param-dict)
    ;获取加载数据的sql
    (when-let [read-sql (dialect/read-data-sql node-param-dict)]
      (timbre/info "加载数据脚本:[" read-sql "]")
      ;执行db读数据
      (case (keyword db-type)
        (:oracle) (oracle/execute! from-db-ins [read-sql])
        (:mysql) (mysql/execute! from-db-ins [read-sql])
        (:sqlite) (sqlite/query from-db-ins [read-sql])
        (:postgresql) (pg/execute! from-db-ins [read-sql])))))


(defn write-rows
  "按行写数据库到db"
  [task-node-execution-info]
  ())