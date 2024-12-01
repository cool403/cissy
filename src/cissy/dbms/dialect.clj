(ns cissy.dbms.dialect
  (:require [honey.sql :as sql]
            [honey.sql.helpers :as helpers]))

(def aa {:incr_key "lastmodify_time" })


(defmulti read-data-sql :dbtype)
(defmethod read-data-sql :oracle
  [sql-params-map]
  (-> (helpers/select :*)
      (helpers/from (:from_table sql-params-map))
      (if-let [incr-key (:incr_key sql-params-map)]
        (helpers/where (>= (incr-key) (:incr_key_value sql-params-map)))
        (helpers/where (= 1 1)))
      (when-let [order-by (:order_by sql-params-map) (helpers/order-by order-by)])
      (helpers/offset (:page_offset sql-params-map))
      (helpers/fetch (:page_size sql-params-map))
      (sql/format {:dialect :oracle})))
(defmethod read-data-sql :postgresql
  [sql-params-map]
  (-> (helpers/select :*)
      (helpers/from (:from_table sql-params-map))
      (if-let [incr-key (:incr_key sql-params-map)]
        (helpers/where (>= (incr-key) (:incr_key_value sql-params-map)))
        (helpers/where (= 1 1)))
      (when-let [order-by (:order_by sql-params-map) (helpers/order-by order-by)])
      (helpers/limit (:page_size sql-params-map))
      (helpers/offset (:page_offset sql-params-map))
      sql/format))
(defmethod read-data-sql :default
  [sql-params-map]
  (-> (helpers/select :*)
      (helpers/from (:from_table sql-params-map))
      (if-let [incr-key (:incr_key sql-params-map)]
        (helpers/where (>= (incr-key) (:incr_key_value sql-params-map)))
        (helpers/where (= 1 1)))
      (when-let [order-by (:order_by sql-params-map) (helpers/order-by order-by)])
      (helpers/limit (:page_size sql-params-map))
      (helpers/offset (:page_offset sql-params-map))
      (sql/format {:dialect :mysql})))


(defmulti write-data-sql :dbtype)

(defmethod write-data-sql :default
  [sql-params-map])

