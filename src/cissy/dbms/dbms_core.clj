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
    ;[honey.sql.helpers :as helpers]
            [cissy.task :as task]
            [honey.sql :as sql]
            [taoensso.timbre :as timbre]
            [next.jdbc :as jdbc]
            [next.jdbc.result-set :as rs]
            [next.jdbc.sql :as jdbc-sql]
            [cissy.helpers :as helpers]))
; Do not use babashka pod to access the database, directly use next.jdbc and honeysql to access the database
;(jdbc/execute! db-spec ["select * from users limit 1"] {:result-set-fn rs/as-maps :builder-fn rs/as-unqualified-maps})
; The above execution returns the same result as the babashka pod
(defn- fill-page-params
  "Fill pagination parameters"
  [task-execution-info node-param-dict]
  (let [{node-execution-dict :node-execution-dict} @task-execution-info
        ; Default 1
        ;; execution-round (get @node-execution-dict :execution-round 1)
        ;; thread-idx (get @node-execution-dict :thread-idx 0)
        ; Default 1000
        page-size (get @node-param-dict :page_size 1000)
        page-offset (get @node-param-dict :page_offset 0)]
    (reset! node-param-dict (assoc @node-param-dict :page_offset (- page-offset page-size)))))

; Read data from the database
(register/defnode drn
  [task-node-execution-info]
  (timbre/info "Start executing drn node")
  (let [{task-execution-info :task-execution-info
         node-param-dict     :node-param-dict
         node-execution-dict :node-execution-dict} @task-node-execution-info
        thread-idx (get @node-execution-dict :thread-idx 0)
        {from-db :from_db} (:drn (:task-config (deref (:task-info @task-execution-info))))
        ; Get associated datasource configuration
        from-db-ins (register/get-datasource-ins from-db)
        ; Get db type
        db-type (if (map? from-db-ins) (:dbtype from-db-ins) "sqlite")]
    ; Insert db type, if sqlite, insert string instead of json
    (reset! node-param-dict (assoc @node-param-dict :dbtype db-type))
    ; Insert pagination related parameters
    (fill-page-params task-node-execution-info node-param-dict)
    ; Get sql for loading data
    (when-let [read-sql (dialect/read-data-sql @node-param-dict)]
      (timbre/info "Execute sql script:[" read-sql "]")
      ; Execute db read data
      (let [result-list (jdbc/execute! from-db-ins [read-sql] {:result-set-fn rs/as-maps :builder-fn rs/as-unqualified-maps})]
        (when (or (nil? result-list) (empty? result-list))
          (timbre/warn (str "Current drn node thread-idx=" thread-idx "did not read data, node status becomes done"))
          ; Resetting the current node is not enough, as multiple threads may share one execution
          (reset! node-execution-dict (assoc @node-execution-dict (keyword (str thread-idx)) "done")))
        result-list))))

;(mysql-sql/insert-multi! aa :users ["id" "username","email"] [[22222222 "njones" "2332"]])
;(vec (map (fn[x] (vec (vals x))) ee)) --> (vec (map #(vec (vals %)) ee))
;(vec (map (fn [x] (name x)) (keys (first ee)))) -->(vec (map #(name %) (keys (first ee))))
;(defn- get-table-columns [sql columns]
;  ;columns need to be converted to keyword, otherwise they will be treated as parameters
;  (apply helpers/columns sql (map keyword columns)))

; Unique or empty parent node
(defn- parent-node-id [node-graph node-id]
  (:node-id (first (task/get-parent-nodes node-graph node-id))))

; Write data to db row by row
(register/defnode dwn
  [node-exec-info]
  (timbre/info "Start executing dwn node")
  (let [{:keys [task-execution-info node-result-dict node-execution-dict]} @node-exec-info
        {:keys [task-info task-execution-dict]} @task-execution-info
        {:keys [task-idx task-name task-config node-graph]} @task-info
        {:keys [dwn]} task-config
        {:keys [thread-idx]} @node-execution-dict
        {:keys [to_db to_table]} dwn
        node-result-lst (get @node-result-dict (keyword (parent-node-id node-graph "dwn"))) 
        ; Get associated datasource configuration
        to-db-ins (register/get-datasource-ins to_db)]
    ; Check if drn node data is empty
    (if (or (nil? node-result-lst) (= (count node-result-lst) 0))
      (do
        (timbre/warn "drn node did not read data, do nothing")
        ; Here we can only reset the current node, not the task status, because the task status is at the task level and cannot be terminated due to the termination of one node
        (helpers/curr-node-done node-exec-info))
      ; Get column information 
      (let [columns (vec (map #(name %) (keys (first node-result-lst))))
            datas (vec (map #(vec (vals %)) node-result-lst))]
        ; Write to different databases based on db type
        (jdbc-sql/insert-multi! to-db-ins to_table columns datas)
        ;; (case (keyword db-type)
        ;;   :oralce (oracle-sql/insert-multi! to-db-ins to-table  columns datas)
        ;;   :mysql (mysql-sql/insert-multi! to-db-ins to-table  columns datas)
        ;;   :postgresql (pg-sql/insert-multi! to-db-ins to-table  columns datas)
        ;;   :sqlite (let [insert-sql (-> (helpers/insert-into to-table)
        ;;                                (get-table-columns columns)
        ;;                                (helpers/values datas)
        ;;                                sql/format)]
        ;;             (sqlite/execute! to-db-ins insert-sql)))
        ; Sync count
        ; Log
        (swap! (:sync-count @task-execution-dict) #(+ % (count datas)))
        (timbre/info (str "Inserted " (deref (:sync-count @task-execution-dict)) " records into " to_table " table"))))))

; Register nodes
;(register/regist-node-fun const/drn read-rows)
;(register/regist-node-fun const/dwn write-rows)

