(ns cissy.log
  (:require [taoensso.timbre :as timbre]
            [taoensso.timbre.appenders.core :as appenders]
            [clojure.string :as str])
  (:import (java.time.format DateTimeFormatter)
           (java.time LocalDateTime ZoneId)
           (java.lang Thread)))

(defn- truncate-str
  "Truncate string to specified length, replace excess with ..."
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
    (format thread-name)))

(defn- format-msg
  "Format message, handle special objects"
  [msg]
  (cond
    (delay? msg) (format-msg @msg)
    (map? msg) (-> msg
                   (dissoc :password)
                   pr-str)
    :else (str msg)))

(defn- format-location
  "Format file location information"
  [?file ?line]
  (let [file-name (if ?file
                    (last (str/split ?file #"/"))
                    "unknown")]
    (format "%-20s:%-4d" file-name (or ?line 0))))

(def ^:private log-format
  "Custom log format"
  (fn [{:keys [level thread msg_ ?file ?line] :as data}]
    (let [thread-name (truncate-str (get-thread-name) 17)
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
  "Initialize log configuration"
  []
  (timbre/merge-config!
    {:output-fn log-format
     :appenders {:println (appenders/println-appender {:stream :auto})}}))