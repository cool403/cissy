(ns cissy.dbms.dbms-core
  (:require [cissy.registry :as register]
            [cissy.dbms.dialect :as dialect]
    ;; [pod.babashka.mysql :as mysql]
    ;; [pod.babashka.oracle :as oracle]
    ;; [pod.babashka.postgresql :as pg]
    ;; [pod.babashka.mysql.sql :as mysql-sql]
    ;; [pod.babashka.oracle.sql :as oracle-sql]
    ;; [pod.babashka.postgresql.sql :as pg-sql]
    ;; [pod.babashka.go-sqlite3 :as sqlite]
            [cissy.const :as const]
            [honey.sql.helpers :as helpers]
            [honey.sql :as sql]
            [taoensso.timbre :as timbre]
            [next.jdbc :as jdbc]
            [next.jdbc.result-set :as rs]
            [next.jdbc.sql :as jdbc-sql]))
; 不使用babashka pod的方式访问数据库，直接使用next.jdbc和honeysql 配合访问数据库
;(jdbc/execute! db-spec ["select * from users limit 1"] {:result-set-fn rs/as-maps :builder-fn rs/as-unqualified-maps})
; 上述执行返回结果和babashka pod的返回结果一样
(defn- fill-page-params
  "填充分页参数"
  [task-execution-info node-param-dict]
  (let [{node-execution-dict :node-execution-dict} @task-execution-info
        ;默认1
        ;; execution-round (get @node-execution-dict :execution-round 1)
        ;; thread-idx (get @node-execution-dict :thread-idx 0)
        ;默认 1000
        page-size (get @node-param-dict :page_size 1000)
        page-offset (get @node-param-dict :page_offset 0)]
    (reset! node-param-dict (assoc @node-param-dict :page_offset (- page-offset page-size)))))

;从数据库读取数据
(defn read-rows
  "从数据库加载读取数据"
  [task-node-execution-info]
  (timbre/info "开始执行drn节点")
  (let [{task-execution-info :task-execution-info
         node-param-dict     :node-param-dict
         node-execution-dict :node-execution-dict} @task-node-execution-info
        thread-idx (get @node-execution-dict :thread-idx 0)
        {from-db :from_db} (:drn (:task-config (deref (:task-info @task-execution-info))))
        ;获取关联数据源配置
        from-db-ins (register/get-datasource-ins from-db)
        ;获取db类型
        db-type (if (map? from-db-ins) (:dbtype from-db-ins) "sqlite")]
    ;塞入db类型,sqlite的时候，塞入的不是json，而是字符串
    (reset! node-param-dict (assoc @node-param-dict :dbtype db-type))
    ;塞入分页相关的参数
    (fill-page-params task-node-execution-info node-param-dict)
    ;获取加载数据的sql
    (when-let [read-sql (dialect/read-data-sql @node-param-dict)]
      (timbre/info "执行sql脚本:[" read-sql "]")
      ;执行db读数据
      (let [result-list (jdbc/execute! from-db-ins [read-sql] {:result-set-fn rs/as-maps :builder-fn rs/as-unqualified-maps})]
        (when (or (nil? result-list) (empty? result-list))
          (timbre/warn (str "当前drn节点 thread-idx=" thread-idx "未读取到数据,节点状态变成done"))
          ;重置当前节点也不行，可能一个节点有多个线程共享一个execution
          (reset! node-execution-dict (assoc @node-execution-dict (keyword (str thread-idx)) "done")))
        result-list))))

;(mysql-sql/insert-multi! aa :users ["id" "username","email"] [[22222222 "njones" "2332"]])
;(vec (map (fn[x] (vec (vals x))) ee)) --> (vec (map #(vec (vals %)) ee))
;(vec (map (fn [x] (name x)) (keys (first ee)))) -->(vec (map #(name %) (keys (first ee))))
(defn- get-table-columns [sql columns]
  ;columns 需要转成keyword,不然会被当成参数 
  (apply helpers/columns sql (map keyword columns)))

(defn write-rows
  "按行写数据库到db"
  [task-node-execution-info]
  (timbre/info "开始执行dwn节点")
  (let [{task-execution-info :task-execution-info
         node-param-dict     :node-param-dict
         node-result-dict    :node-result-dict} @task-node-execution-info
        {task-execution-dict :task-execution-dict} @task-execution-info
        {to-db :to_db} @node-param-dict
        ;获取关联数据源配置
        to-db-ins (register/get-datasource-ins to-db)
        ;获取db类型
        db-type (if (map? to-db-ins) (:dbtype to-db-ins) "sqlite")
        drn-res ((keyword const/DRN_NODE_NAME) @node-result-dict)
        to-table (:to_table @node-param-dict)]
    ;判断drn节点数据是否为空
    (if (or (nil? drn-res) (= (count drn-res) 0))
      (do
        (timbre/warn "drn节点未读取到数据，什么都不做")
        ;这里只能重置当前节点，不能重置任务状态，因为任务状态是任务级别的，不能因为一个节点的终止而终止任务
        (reset! task-node-execution-info (assoc @task-node-execution-info :curr-node-status "done")))
      ;获取列信息 
      (let [columns (vec (map #(name %) (keys (first drn-res))))
            datas (vec (map #(vec (vals %)) drn-res))]
        ;根据db类型写入不同的数据库
        (jdbc-sql/insert-multi! to-db-ins to-table columns datas)
        ;; (case (keyword db-type)
        ;;   :oralce (oracle-sql/insert-multi! to-db-ins to-table  columns datas)
        ;;   :mysql (mysql-sql/insert-multi! to-db-ins to-table  columns datas)
        ;;   :postgresql (pg-sql/insert-multi! to-db-ins to-table  columns datas)
        ;;   :sqlite (let [insert-sql (-> (helpers/insert-into to-table)
        ;;                                (get-table-columns columns)
        ;;                                (helpers/values datas)
        ;;                                sql/format)]
        ;;             (sqlite/execute! to-db-ins insert-sql)))
        ;同步计数
        ;打印日志
        (swap! (:sync-count @task-execution-dict) #(+ % (count datas)))
        (timbre/info (str "已插入" (deref (:sync-count @task-execution-dict)) "条记录到" to-table "表里"))))))


;注册节点
(register/regist-node-fun const/DRN_NODE_NAME read-rows)
(register/regist-node-fun const/DWN_NODE_NAME write-rows)