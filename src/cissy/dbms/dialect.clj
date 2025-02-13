(ns cissy.dbms.dialect
  (:require [honey.sql :as sql]
            [honey.sql.helpers :as helpers]
            [clojure.string :as str])
  (:import (java.lang IllegalArgumentException)))

(defn- build-where-cond
  "Build where condition"
  [prefix-sql sql-params-map]
  ;; (prn prefix-sql)
  (if-let [incr-key (:incr_key sql-params-map)]
    ; where lastmodify_time >= '2021-12-12'
    (str/join " " [prefix-sql "where" incr-key ">=" (str "'" (:incr_key_value sql-params-map) "'")])
    (str/join " " [prefix-sql "where 1=1 "])))

(defn- build-orderby-cond
  "Build orderby"
  [prefix-sql sql-params-map]
  ;; (prn prefix-sql)
  (if-let [order-by (:order_by sql-params-map)]
    (str/join " " [prefix-sql "order by" order-by])
    prefix-sql))

(defn- build-page-cond
  "Build pagination"
  [prefix-sql sql-params-map]
  ;; (prn prefix-sql)
  #_{:clj-kondo/ignore [:syntax]}
  (when-let [db-type (:dbtype sql-params-map)]
    (let [page-offset (:page_offset sql-params-map)
          page-size (:page_size sql-params-map)]
      (cond
        (= db-type "oracle") (str/join " " [prefix-sql "offset"
                                            page-offset "rows fetch next" page-size "rows only"])
        (= db-type "postgresql") (str/join " " [prefix-sql page-size "offset" page-offset])
        :else (str/join " " [prefix-sql "limit" page-offset "," page-size])))))

(defn read-data-sql
  "Get sql for reading data"
  [sql-params-map]
  ; If it is a sql template, render and return directly
  (if-not (str/blank? (:sql_template sql-params-map))
    (-> (:sql_template sql-params-map)
        (build-page-cond sql-params-map))
    (-> "select * from "
        (str (:from_table sql-params-map))
        (build-where-cond sql-params-map)
        (build-orderby-cond sql-params-map)
        (build-page-cond sql-params-map))))

(def aa {:from_db        "db1"
         :dbtype         "mysql"
         :from_table     "es_task_info"
         :incr_key       "lastmodify_time"
         :incr_key_value "2024-10-25"
         :page_size      2,
         :sql_template   ""})
(read-data-sql aa)

(defmulti write-data-sql :dbtype)

(defmethod write-data-sql :default
  [sql-params-map])

