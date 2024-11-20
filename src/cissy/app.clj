(ns cissy.app
  (:require [cissy.task :refer [TaskNodeInfo]]))

(task/->TaskNodeInfo "" "")
(defn -main [& _args]
  (println "hello bb"))