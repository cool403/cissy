(ns xhs.initialization
  (:require
   [next.jdbc :as jdbc]
   [taoensso.timbre :as timbre]
   [xhs.init-sql :refer [xhs-comments-table-sql xhs-pages-table-sql
                         xhs-posts-table-sql]]))

; the check sql
(defonce check-table-sql "select 1 from sqlite_master where name='xhs_pages' and type='table'")

; Check if the table exists
(defn- check-table-exists? [db-spec]
  (seq (jdbc/execute! db-spec [check-table-sql] {:multi-rs true})))

; Create tables if not exists
(defn- create-tables [db-spec]
  (if (not (check-table-exists? db-spec))
    (do
      (timbre/info "Create xhs_pages, xhs_posts, xhs_comments tables")
      (jdbc/execute! db-spec xhs-pages-table-sql)
      (jdbc/execute! db-spec xhs-posts-table-sql)
      (jdbc/execute! db-spec xhs-comments-table-sql))
    (timbre/info "xhs_pages, xhs_posts, xhs_comments tables already exists")))

;{dbtype "sqlite",  dbname "db/xhs.db"}
(defn init-db [db-spec]
  (timbre/info "Initialize database")
  (defonce init (create-tables db-spec)))