(ns xhs.get-posts
  (:require 
   [cissy.registry :refer [defnode]]
   [taoensso.timbre :as timbre]
   [xhs.initialization :refer [init-db]]
   [xhs.db :refer [get-todo-pages]]
   [xhs.http :as http]))

(defn- extract-all-posts [page content])

(defn- craw-page [page-dict]
  (let [{:keys [page cookie_file]} page-dict
        {:keys [page_url :id]} page
        content (http/http-get {:page_url page_url :cookie_file cookie_file})]
    (extract-all-posts page content)))

; init get-posts url
; read people's profile page from database
(defnode get-posts [node-exec-info]
  (timbre/info "start get-posts")
  (let [{:keys [task-execution-info node-result-dict node-execution-dict]} @node-exec-info
        {:keys [task-info task-execution-dict]} @task-execution-info
        {:keys [task-idx task-name task-config node-graph]} @task-info
        {:keys [get-posts]} task-config
        {:keys [thread-idx]} @node-execution-dict
        {:keys [seed_url db_file cookie_file]} get-posts
        db-spec {:dbtype "sqlite" :dbname db_file}]
    (timbre/info (str "thread-idx=" thread-idx ",seed_url=" seed_url ",db_file=" db_file))
    ;init db
    (init-db db-spec seed_url)
    ;; load page urls
    (let [todo-pages (get-todo-pages db-spec thread-idx)]
      (if (> (count todo-pages) 0)
        (doseq [page todo-pages]
          (try
            (craw-page {:page page :cookie_file cookie_file})
            (catch Exception e
              (timbre/error (str "Thread=" thread-idx ", error when crawling page" page ", " (.getMessage e) e)))))
        (timbre/info (str "Thread=" thread-idx ", no more todo pages"))))))