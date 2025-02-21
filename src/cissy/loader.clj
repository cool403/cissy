(ns cissy.loader
  ;;   (:import (cissy.task TaskInfo))
  (:require [cheshire.core :as json]
            [cissy.task :as task]
            [taoensso.timbre :as timbre]
            [cissy.sched :as sched]
            [cissy.registry :as registry]
            [cissy.const :as const]
            [clojure.string :as str]
            [cissy.scripts-loader :as sl]
            [cissy.checker :as checker]
            [cissy.helpers :as helpers]
            [clojure.edn :as edn])
  (:import (java.util ArrayList HashMap)))

;; (def demo-json "{\n    \"task_name\":\"demo\",\n    \"nodes\":\"demo->\",\n    \"demo\":{\n        \n    }\n}")

; Parse the nodes configuration in the json
(defn- parse-node-rel-str [s]
  (let [parts (str/split s #";")
        res (atom [])]                                      ; Split by semicolon
    (doseq [part parts]
      (let [[k v] (str/split part #"->")]                   ; Split by arrow
        (timbre/info (str k "->" v))
        (if (empty? v)                                      ; If there is no value after the arrow, treat it as nil
          (reset! res (conj @res (vector k nil)))
          (doseq [v1 (str/split v #",")]
            (reset! res (conj @res (vector k v1)))))))
    @res))                                                  ; If there is a value, split by comma
(comment
  (parse-node-rel-str "a->b;a->c,d;e->"))

(defn- comp-new-task-fn [task tasks-map]
  ; Combine new task
  (merge-with (fn [v1 v2] (helpers/my-merge-fn v1 v2)) task (dissoc tasks-map :tasks :task_group_name :entry_script :datasource)))

(defn- get-tasks-map-fn [config-json]
  ; Parse task group
  (let [config-map (edn/read-string config-json)
        {datasource      :datasource
         task-group-name :task_group_name
         tasks           :tasks
         entry-scripts   :entry_script} config-map
        task-map-vec (atom [])]
    (when (not-empty entry-scripts)
      (timbre/info "Detected script tasks")
      (doseq [entry-script entry-scripts]
        (sl/load-main-entry entry-script)))
    ; FIX 202502 The following writing method will cause the number of iterations to be a Cartesian product
    ;; (doseq [task tasks idx (range 0 (count tasks))]
    (doseq [[idx task] (map-indexed vector tasks)]
      (reset! task-map-vec (conj @task-map-vec (-> task
                                                   (comp-new-task-fn config-map)
                                                   (assoc :datasource datasource)
                                                   ; Use a simple name plus task number to replace the subtask name for now, optimize later
                                                   (assoc :task_name (str task-group-name "_" idx))
                                                   (assoc :task-idx idx)))))
    @task-map-vec))

(defn assemble-task-fn [task-map]
  ; Build task-info
  (let [{task-name  :task_name
         nodes      :nodes
         datasource :datasource} task-map
        node-graph (task/create-task-node-graph)
        task-info (atom (task/->TaskInfo nil task-name nil (sched/->ChanBasedSched) node-graph task-map))]
    ; Parse node configuration nodes(a->b;) into node pairs
    (doseq [[from-node-id to-node-id] (parse-node-rel-str nodes)]
      (cond
        (nil? to-node-id) (task/add-node-pair node-graph (task/->TaskNodeInfo from-node-id nil) nil)
        :else (task/add-node-pair node-graph (task/->TaskNodeInfo from-node-id nil) (task/->TaskNodeInfo to-node-id nil))))
    ; Validate node graph structure
    (timbre/info (str "Start validating node graph structure, current subtask configuration:" task-map))
    ;; (prn node-graph)
    (checker/validate-node-graph node-graph)
    (timbre/info "Node graph structure validation passed")
    ; Build tree after adding
    ;; (prn node-grpah)
    (task/build-node-tree node-graph)
    (timbre/info "Node relationship parsing completed")
    ; Register datasource
    (timbre/info "Start initializing datasource configuration")
    (doseq [[db-sign db-config-map] datasource]
      (timbre/info "Start initializing datasource for db-sign" db-sign)
      ; Directly call register datasource, register and instantiate by register
      (registry/register-datasource db-sign db-config-map)
      (timbre/info "Datasource configuration initialization completed")
      (when (contains? (set (map #(:node-id %) (:all-node-id-set node-graph))) const/drn)
        (timbre/info "Since the current task configuration contains drn node, automatically switch to ExecutionAlwaysSched strategy")
        (reset! task-info (assoc @task-info :sched-info (sched/->ChanBasedSched)))))
    task-info))

; Parse task from edn
(defn get-task-from-edn [^String task-config-edn]
  ; edn->keyword map, keyword map can be accessed by (:name)
  (timbre/info "Start parsing task group configuration" task-config-edn)
  (map assemble-task-fn (get-tasks-map-fn task-config-edn)))

; Test
;; (def demo-edn (slurp "/home/mawdx/Desktop/task_config.edn"))
;; (get-task-from-edn demo-edn)