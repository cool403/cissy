(ns cissy.loader
  ;;   (:import (cissy.task TaskInfo))
  (:require [cheshire.core :as json]
            [cissy.task :as task]
            [taoensso.timbre :as timbre]
            [clojure.string :as str])
  (:import (java.util ArrayList HashMap)))

(def demo-json "{\n    \"task_name\":\"demo\",\n    \"nodes\":\"demo->\",\n    \"demo\":{\n        \n    }\n}")

(defn- parse-node-rel-str [s]
  (let [parts (str/split s #";")
        res (atom [])] ; 按照分号切分
    (doseq [part parts]
      (let [[k v] (str/split part #"->")] ; 按照箭头切分
        (timbre/info k v)
        (if (empty? v) ; 如果没有箭头后的值，视为nil
          (reset! res (conj @res (vector k nil)))
          (doseq [v1 (str/split v #",")]
            (reset! res (conj @res (vector k v1)))))))
    @res)) ; 如果有值，按照逗号切分

(parse-node-rel-str "a->b;a->c,d;e->")

;从 json 中解析任务
(defn get-task-from-json [^String task-json]
  "从 json 中解析任务。
  ;ret TaskInfo 返回一个任务类型"
  ;json->keyword map,keyword map才能用(:name 方式访问)
  (timbre/info "开始解析任务配置"  task-json)
  (let [task-map (json/parse-string task-json #(keyword %))
        {task-name :task_name nodes :nodes datasource :datasource} task-map
        node-grpah (task/->TaskNodeGraph (HashMap.) (HashMap.) (ArrayList.) (HashMap.))
        task-info (atom (task/->TaskInfo nil task-name nil ))]
    ())
  (json/parse-string demo-json #(keyword %)))