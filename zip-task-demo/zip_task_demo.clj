(ns zip-task-demo
  (:require [cissy.registry :refer [defnode]]))

(defnode rnd [node-exec-info]
  (let [{:keys [task-execution-info]} @node-exec-info
         {:keys [task-info]} @task-execution-info
         {:keys [task-idx task-name task-config]} @task-info
         {:keys [rnd]} task-config]))



(defnode wnd [node-exec-info]
  (let [{:keys [task-execution-info node-result-dict]} @node-exec-info
         {:keys [task-info]} @task-execution-info
         {:keys [task-idx task-name task-config]} @task-info
         {:keys [rnd]} task-config]))
