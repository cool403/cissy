(ns cissy.log
  (:require [taoensso.timbre :as timbre]
            [taoensso.timbre.appenders.core :as appenders]
            [clojure.string :as str])
  (:import (java.time.format DateTimeFormatter)
           (java.time LocalDateTime ZoneId)
           (java.lang Thread)))

(defn- truncate-str 
  "截断字符串到指定长度，超出部分用...替代"
  [s max-len]
  (if (<= (count s) max-len)
    (str s (apply str (repeat (- max-len (count s)) " ")))
    (str (subs s 0 (- max-len 3)) "...")))

(def ^:private datetime-formatter 
  (DateTimeFormatter/ofPattern "yyyy-MM-dd HH:mm:ss.SSS"))

(defn- format-timestamp []
  (.format datetime-formatter (LocalDateTime/now (ZoneId/systemDefault))))

(defn- get-thread-name []
  (let [thread (Thread/currentThread)
        thread-id (.getId thread)
        thread-name (.getName thread)]
    (cond
      (= thread-name "main") "main"
      (.startsWith thread-name "async") (format "async-%d" thread-id)
      :else (format "t-%d" thread-id))))

(defn- format-msg 
  "格式化消息，处理特殊对象"
  [msg]
  (cond
    (delay? msg) (format-msg @msg)
    (map? msg) (-> msg 
                   (dissoc :password)
                   pr-str)
    :else (str msg)))

(defn- format-location
  "格式化文件位置信息"
  [?file ?line]
  (let [file-name (if ?file 
                   (last (str/split ?file #"/"))
                   "unknown")]
    (format "%-20s:%-4d" file-name (or ?line 0))))

(def ^:private log-format
  "自定义日志格式"
  (fn [{:keys [level thread msg_ ?file ?line] :as data}]
    (let [thread-name (truncate-str (get-thread-name) 10)
          level-str (truncate-str (-> level name str/upper-case) 5)
          location (format-location ?file ?line)
          formatted-msg (format-msg msg_)]
      (format "%s [%-10s] %-5s %-25s %s"
              (format-timestamp)
              thread-name
              level-str
              (str location ":")
              formatted-msg))))

(defn init-logging!
  "初始化日志配置"
  []
  (timbre/merge-config!
   {:output-fn log-format
    :appenders {:println (appenders/println-appender {:stream :auto})}}))