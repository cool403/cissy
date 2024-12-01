(ns cissy.dbms.dialect
  (:require [honey.sql :as sql]
            [honey.sql.helpers :as helpers])
  (:import (java.lang IllegalArgumentException)))





(defmulti read-data-sql (fn [sql-params-map]
                          (let [db-type (:dbtype sql-params-map)]
                            (cond
                              (= db-type "oracle") :oracle
                              (= db-type "postgresql") :pg
                              :else :common))))
(defmethod read-data-sql :oracle
  [sql-params-map]
  (-> (helpers/select :*)
      (helpers/from (keyword (:from_table sql-params-map)))
      (when-let [incr-key (:incr_key sql-params-map)]
        (helpers/where (>= (keyword (incr-key)) (:incr_key_value sql-params-map))))
      (when-let [order-by (:order_by sql-params-map)]
        (helpers/order-by (keyword order-by)))
      (helpers/offset (:page_offset sql-params-map))
      (helpers/fetch (:page_size sql-params-map))
      (sql/format {:dialect :oracle})))
(defmethod read-data-sql :pg
  [sql-params-map]
  (-> (helpers/select :*)
      (helpers/from (keyword (:from_table sql-params-map)))
      (when-let [incr-key (:incr_key sql-params-map)]
        (helpers/where (>= (keyword (incr-key)) (:incr_key_value sql-params-map))))
      (when-let [order-by (:order_by sql-params-map)]
        (helpers/order-by (keyword order-by)))
      (helpers/offset (:page_offset sql-params-map))
      (helpers/fetch (:page_size sql-params-map))
      sql/format))
(defmethod read-data-sql :common
  [sql-params-map]
  (-> (helpers/select :*)
      (helpers/from (keyword (:from_table sql-params-map)))
      (cond->
       (:incr_key sql-params-map) (helpers/where (>= #_{:clj-kondo/ignore [:type-mismatch]}
                                                  (keyword (:incr_key sql-params-map)) (:incr_key_value sql-params-map))))
      (cond->
       (:order_by sql-params-map) (helpers/order-by (keyword (:order_by sql-params-map))))
      (helpers/offset (:page_offset sql-params-map))
      (helpers/fetch (:page_size sql-params-map))
      (sql/format {:dialect :mysql})))

(def aa {:from_db "db1"
         :dbtype "mysql"
         :from_table "es_task_info"
         :incr_key "lastmodify_time"
         :incr_key_value "2024-10-25"
         :page_size  2,
         :sql_template ""})
(read-data-sql aa)


(defmulti write-data-sql :dbtype)

(defmethod write-data-sql :default
  [sql-params-map])

