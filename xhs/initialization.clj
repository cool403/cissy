(ns xhs.initialization
  (:require
   [honey.sql :as sql]
   [honey.sql.helpers :refer [columns insert-into values]]
   [next.jdbc :as jdbc]
   [taoensso.timbre :as timbre]
   [xhs.create-table-sql :refer [xhs-comments-table-sql xhs-pages-table-sql
                                 xhs-posts-table-sql]]
   [xhs.url-manager :refer [add-urls init-url-queue]]))

; the check sql
(defonce check-table-sql "select 1 from sqlite_master where name='xhs_pages' and type='table'")

; Check if the table exists
(defn- check-table-exists? [db-spec]
  (seq (jdbc/execute! db-spec [check-table-sql])))

; Create tables if not exists
(defn- create-tables [db-spec seed-url]
  (if (not (check-table-exists? db-spec))
    (do
      (timbre/info "Create xhs_pages, xhs_posts, xhs_comments tables")
      (jdbc/execute! db-spec [xhs-pages-table-sql])
      (jdbc/execute! db-spec [xhs-posts-table-sql])
      (jdbc/execute! db-spec [xhs-comments-table-sql])
      ; Insert seed data
      (jdbc/execute! db-spec (sql/format (-> (insert-into :xhs_pages)
                                             (columns :page_url :craw_status :create_time :lastmodifiy_time)
                                             (values [[seed-url "TODO" (System/currentTimeMillis) (System/currentTimeMillis)]]))))
      (timbre/info "insert into seed url:" seed-url " success!"))
    (timbre/info "xhs_pages, xhs_posts, xhs_comments tables already exists")))

;; make sure only one thread call create-tables
(defonce init-lock (atom false))
(defonce initialized (atom false))

(defmulti init (fn [options] (:run-mode options)))

;; db-spec: {dbtype "sqlite",  dbname "db/xhs.db"}
(defmethod init :db [{:keys [db-spec seed-url]} options]
  (timbre/info "Initialize database")
  (loop []
    (if @initialized
      (timbre/info "Database already initialized")
      (if (compare-and-set! init-lock false true)
        (do
          (create-tables db-spec seed-url)
          (reset! initialized true))
        (do
          (Thread/sleep 15)
          (recur))))))

(defmethod init :default [{:keys [seed-url queue-path]} options]
  (timbre/info "runs in the local memory mode")
  (loop []
    (if @initialized
      (timbre/info "Database already initialized")
      (if (compare-and-set! init-lock false true)
        (do
          (init-url-queue queue-path)
          ;;add -url
          (add-urls {:run-mode (:run-mode options) :url-type "page" :page-urls [seed-url]})
          (reset! initialized true))
        (do
          (Thread/sleep 15)
          (recur))))))