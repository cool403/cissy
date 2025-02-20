(ns xhs.db
  (:require
   [next.jdbc :as jdbc]
   [next.jdbc.result-set :as rs]))


; get todo pages
(defn get-todo-pages [db-spec]
  (jdbc/execute! db-spec ["select * from xhs_pages where craw_status='TODO' order by lastmodifiy_time  desc"] {:result-set-fn rs/as-maps :builder-fn rs/as-unqualified-maps}))

; get todo posts
(defn get-todo-posts [db-spec]
  (jdbc/execute! db-spec ["select * from xhs_posts xp  where is_visited = 'N' or (last_visit_time < (STRFTIME('%s', 'now') * 1000 - 5000 * 3600*24 )) order by lastmodify_time  desc"]
                 {:result-set-fn rs/as-maps :builder-fn rs/as-unqualified-maps}))