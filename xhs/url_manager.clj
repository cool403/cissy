(ns xhs.url-manager 
  (:require
   [clojure.java.io :as io]
   [clojure.data.json :as json]) 
  (:import
   [java.nio.file Files Paths]
   [net.openhft.chronicle.queue.impl.single SingleChronicleQueue]))

(defonce queue-types #{:post-url :user-profile-url})

(def url-queue-ref (atom {}))

(defn init-url-queue [^String queue-path]
  (doseq [queue-type queue-types]
    (let [queue-dir (str queue-path "/" queue-type)]
      ; check dir exists, not exists create it
      (when-not (.exists (io/file queue-dir))
        (Files/createDirectory (Paths/get queue-dir))
        (println (str "create dir: " queue-dir)))
      (let [local-file-queue ((SingleChronicleQueue/single queue-dir) .build)]
        (reset! url-queue-ref (assoc @url-queue-ref queue-type local-file-queue))
        (println (str "init " queue-type " queue"))))))

;; url-info: {:run-mode "db" :url-type "page" :page-urls ["url1" "url2"]}
(defmulti add-urls (fn [url-info] (:run-mode url-info)))

(defmethod add-urls :db 
  [url-info])

;url-info is a map ,and add it to the queue
(defmethod add-urls :default
    [{:keys [url-type page-urls] :as url-info}]
  (let [local-file-queue (@url-queue-ref url-type)
        appender (.aquireAppender local-file-queue)]
    (doseq [url page-urls]
      (.writeText appender url))
    (.releaseAppender appender))
  (println (str "add " url-type " urls: " page-urls " success!")))


;url-info: {:run-mode "db" :url-type "page" :page-url ""}
(defmulti get-urls (fn [options] (:run-mode options)))

(defmethod get-urls :db 
  [options])

(defmethod get-urls :default
  [{:keys [url-type] :as options}]
  ;; every time get 10 urls
  (let [local-file-queue (@url-queue-ref url-type)
        tailer (.createTailer local-file-queue)
        todo-urls (atom [])]
    (loop [x 0 ]
      (when (< x 10)
        (let [entry (.readText tailer)]
          (when entry
            (reset! todo-urls (conj @todo-urls entry))
            (recur (inc x) )))))
    ;;skip the read result
    (.toEnd tailer)
    @todo-urls))