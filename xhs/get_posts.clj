(ns xhs.get-posts
  (:require
   [cissy.registry :refer [defnode]]
   [taoensso.timbre :as timbre]
   [xhs.initialization :refer [init]]
   [xhs.url-manager :refer [get-urls add-urls]]
   [xhs.http :as http]
   [xhs.parser :as parser]
   [xhs.llm :as llm]))

;; craw page
(defn- craw-page [page-dict]
  (timbre/info (str "start to craw page=" (get-in page-dict [:page :page_url])))
  (let [{:keys [page cookie_file]} page-dict
        {:keys [page_url id]} page
        content (http/http-get {:page_url page_url :cookie_file cookie_file})]
    (parser/parse-html-content {:content-type :home-page :page page :content content :parse-type :llm})))

; init get-posts url
; read people's profile page from database
(defnode get-posts [node-exec-info]
  (timbre/info "start get-posts")
  (let [{:keys [task-execution-info node-result-dict node-execution-dict]} @node-exec-info
        {:keys [task-info task-execution-dict]} @task-execution-info
        {:keys [task-idx task-name task-config node-graph]} @task-info
        {:keys [get-posts]} task-config
        {:keys [thread-idx]} @node-execution-dict
        {:keys [seed_url queue_path cookie_file]} get-posts]
    ;; (timbre/info (str "thread-idx=" thread-idx ",seed_url=" seed_url ",db_file=" db_file))
    ;init db
    (init {:run-mode "local" :seed_url seed_url :queue_path queue_path})
    ;; load page urls
    (let [todo-pages (get-urls {:run-mode "local" :url-type "page"})]
      (timbre/info (str "node-id=get-posts,thread-idx=" thread-idx ", get " (count todo-pages) " pages to crawl"))
      (if (> (count todo-pages) 0)
        (doseq [page todo-pages]
          (try
            (craw-page {:page page :cookie_file cookie_file})
            (catch Exception e
              (timbre/error (str "Thread=" thread-idx ", error when crawling page" page ", " (.getMessage e) e)))))
        (timbre/info (str "Thread=" thread-idx ", no more todo pages"))))))