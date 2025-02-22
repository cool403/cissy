(ns xhs.db
  (:require
   [honey.sql :as sql]
   [honey.sql.helpers :as helpers]
   [next.jdbc :as jdbc]
   [next.jdbc.result-set :as rs]
   [taoensso.timbre :as timbre]))


(def get-todo-pages-lock (atom false))
(def get-todo-posts-lock (atom false))

(defonce get-todo-pages-sql "select * from xhs_pages where craw_status = 'TODO' or (craw_status = 'DONE' and last_visit_time < (STRFTIME('%s', 'now') * 1000 - 5000 * 3600*24 )) 
                             order by lastmodifiy_time  desc 
                             limit 10")
(defonce get-todo-posts-sql "select * from xhs_posts xp  where craw_status = 'TODO' or (craw_status = 'DONE' and last_visit_time < (STRFTIME('%s', 'now') * 1000 - 5000 * 3600*24 )) 
                             order by lastmodify_time  desc 
                             limit 10")

; get todo pages
; avoid concurrent access
(defn get-todo-pages [db-spec thread-idx]
  (try
    (loop []
      (if (compare-and-set! get-todo-pages-lock false true)
        (do
          (timbre/info (str "Thread=" thread-idx ", successfully get todo pages"))
          (let [res-vec (jdbc/execute! db-spec [get-todo-pages-sql] {:result-set-fn rs/as-maps :builder-fn rs/as-unqualified-maps})]
            ;; update page status to DING
            (jdbc/execute! db-spec (sql/format (-> (helpers/update :xhs_pages)
                                                   (helpers/set {:craw_status "DING" :lastmodifiy_time (System/currentTimeMillis)})
                                                   (helpers/where [:in :id (map :id res-vec)]))))
            res-vec))
        (do
          (timbre/warn (str "Thread=" thread-idx ",failed to get the lock for init kafka producer"))
          (Thread/sleep 10)
          (recur))))
    (finally
      (reset! get-todo-pages-lock false))))

; get todo posts
; avoid concurrent access
(defn get-todo-posts [db-spec thread-idx]
  (try
    (loop []
      (if (compare-and-set! get-todo-posts-lock false true)
        (do
          (timbre/info (str "Thread=" thread-idx ", successfully get todo posts"))
          (let [res-vec (jdbc/execute! db-spec [get-todo-posts-sql] {:result-set-fn rs/as-maps :builder-fn rs/as-unqualified-maps})]
            ;; update post status to DING
            (jdbc/execute! db-spec (sql/format (-> (helpers/update :xhs_posts)
                                                   (helpers/set {:craw_status "DING" :lastmodify_time (System/currentTimeMillis)})
                                                   (helpers/where [:in :id (map :id res-vec)]))))
            res-vec))
        (do
          (timbre/warn (str "Thread=" thread-idx ",failed to get the lock for init kafka producer"))
          (Thread/sleep 10)
          (recur))))
    (finally
      (reset! get-todo-posts-lock false))))