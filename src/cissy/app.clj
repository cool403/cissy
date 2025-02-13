(ns cissy.app
  (:gen-class)
  (:require [clojure.tools.cli :as cli]
            [clojure.string :as str]
            [cissy.commands :as commands]
            [taoensso.timbre :as timbre]
            [cissy.log :as log]))

(def cli-options
  [["-c" "--config CONFIG" "Task configuration description file path (json format)"]
   ["-h" "--help" "Show help information"]
   ["-z" "--zip" "Generate a zip task demo"]])

(defn usage [options-summary]
  (->> ["cissy - Data synchronization tool"
        ""
        "Usage: cissy [options] command"
        ""
        "Options:"
        options-summary
        ""
        "Commands:"
        "  start    Start task"
        "  demo     Get configuration file example"
        ""
        "Examples:"
        "  cissy -c task.json start"
        "  cissy demo > task.json"]
       (str/join \newline)))

(defn error-msg [errors]
  (str "The following errors occurred while parsing your command:\n\n"
       (str/join \newline errors)))

(defn validate-args
  [args]
  (let [{:keys [options arguments errors summary]} (cli/parse-opts args cli-options)]
    (cond
      (:help options)                                       ; help => exit OK with usage summary
      {:exit-message (usage summary) :ok? true}
      errors                                                ; errors => exit with description of errors
      {:exit-message (error-msg errors)}
      ;; custom validation on arguments
      (and (= 1 (count arguments))
           (#{"start" "demo"} (first arguments)))
      {:action (first arguments) :options options}
      :else                                                 ; failed custom validation => exit with usage summary
      {:exit-message "Error: start command requires a configuration file (-c)"})))

(defn exit [status msg]
  (println msg)
  (System/exit status))

(defn -main [& args]
  (log/init-logging!)
  (let [{:keys [action options exit-message ok?]} (validate-args args)]
    (if exit-message
      (exit (if ok? 0 1) exit-message)
      (case action
        "start" (commands/start options)
        "demo" (commands/-demo options)))))