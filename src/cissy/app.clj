(ns cissy.app
  (:require [clojure.tools.cli :as cli]
            [clojure.string :as str]
            [cissy.commands :as commands]))

(def cli-options
  [["-c" "--config config" "任务配置描述,需要是json格式"
    ;; :default 80
    ;; :parse-fn #(Integer/parseInt %)
    ;; :validate [#(< 0 % 0x10000) "Must be a number between 0 and 65536"]]
    ]
   ["-h" "--help"]])


(defn usage [options-summary]
  (->> ["cissy是一个支持数据节点同步工具;支持数据同步"
        ""
        "Usage: cissy [-h] {start,demo} ..."
        ""
        "Options:"
        options-summary
        ""
        "Actions:"
        "  start  启动任务"
        "  demo   获取任务json dmeo"
        ""
        "-h help 获取更多支持"
        ]
       (str/join \newline)))

(defn error-msg [errors]
  (str "The following errors occurred while parsing your command:\n\n"
       (str/join \newline errors)))

(defn validate-args 
  [args]
  (let [{:keys [options arguments errors summary]} (cli/parse-opts args cli-options)]
    (cond
      (:help options) ; help => exit OK with usage summary
      {:exit-message (usage summary) :ok? true}
      errors ; errors => exit with description of errors
      {:exit-message (error-msg errors)}
      ;; custom validation on arguments
      (and (= 1 (count arguments))
           (#{"start" "demo"} (first arguments)))
      {:action (first arguments) :options options}
      :else ; failed custom validation => exit with usage summary
      {:exit-message (usage summary)})))

(defn exit [status msg]
  (println msg)
  (System/exit status))

(defn -main [& args]
  (let [{:keys [action options exit-message ok?]} (validate-args args)]
    (if exit-message
      (exit (if ok? 0 1) exit-message)
      (case action
        "start"  (commands/start options)
        "demo" (commands/demo options)))))