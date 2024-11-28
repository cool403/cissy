(ns cissy.app
  (:require [cissy.task :refer [TaskNodeInfo]]
            [babashka.cli :as cli]))

(def cli-options {:port {:default 80 :coerce :long}
                  :help {:coerce :boolean}})

(defn -main [& _args]
  (println (cli/parse-opts _args {:spec cli-options})))