(ns cissy.dbms.dialect
  (:require [honey.sql :as sql]))


(defmulti read-data-sql :dbtype)
(defmethod read-data-sql :oracle
  [sql-params-map])
(defmethod read-data-sql :postgresql
  [sql-params-map])
(defmethod read-data-sql :default
  [sql-params-map]
  ())


(defmulti write-data-sql :dbtype)

(defmethod write-data-sql :default
  [sql-params-map])

