(ns cissy.loader
  ;;   (:import (cissy.task TaskInfo))
  (:require [cheshire.core :as json]))

(def demo-json "{\n    \"task_name\":\"demo\",\n    \"nodes\":\"demo->\",\n    \"demo\":{\n        \n    }\n}")
;从 json 中解析任务
(defn get-task-from-json [^String task-json]
  "从 json 中解析任务。
  ;ret TaskInfo 返回一个任务类型"
  ;json->keyword map,keyword map才能用(:name 方式访问)
  (let [task-map (json/parse-string task-json #(keyword %))
        {task-name :task_name nodes :nodes datasource :datasource} task-map]
    )
  (json/parse-string demo-json #(keyword %)))