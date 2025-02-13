(ns cissy.app
  (:gen-class)
  (:require [clojure.tools.cli :as cli]
            [clojure.string :as str]
            [cissy.commands :as commands]
            [taoensso.timbre :as timbre]
            [cissy.log :as log]))

(def cli-options
  [["-c" "--config CONFIG" "任务配置描述文件路径(json格式)"]
   ["-h" "--help" "显示帮助信息"]
   ["-z" "--zip" "生成一个zip任务demo"]])

(defn usage [options-summary]
  (->> ["cissy - 数据同步工具"
        ""
        "用法: cissy [选项] 命令"
        ""
        "选项:"
        options-summary
        ""
        "命令:"
        "  start    启动任务"
        "  demo     获取配置文件示例"
        ""
        "示例:"
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
      {:exit-message "Error: start 命令需要指定配置文件(-c)"})))

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