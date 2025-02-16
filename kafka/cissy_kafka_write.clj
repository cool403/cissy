(ns cissy-kafka-write
  (:require
   [cissy.registry :refer [defnode] :as register]
   [cissy.task :as task]
   [taoensso.timbre :as timbre]
   [cissy.helpers :as helpers]
   [cheshire.core :as json])
  (:import [org.apache.kafka.clients.producer KafkaProducer ProducerRecord]))


;global kafka instance map;key is the kafka instance name, value is the kafka instance
(def kafka-producer-map (atom {}))
(def init-kafka-producer-lock (atom false))

(def topic-sub-lock (atom false))
(def topic-vec (atom []))


(defn- init-kafka-Producer [kafka-config-map]
  (let [kafka-properties (java.util.Properties.)]
    (doseq [[k v] (-> kafka-config-map
                      (helpers/my-merge-fn {"key.serializer" "org.apache.kafka.common.serialization.StringSerializer"
                                            "value.serializer" "org.apache.kafka.common.serialization.StringSerializer"
                                            "batch.size" 32678
                                            "linger.ms" 100}))]
      (.put kafka-properties (name k) v))
    (new KafkaProducer kafka-properties)))

; ensure that only one thread can get kafka successfully
; the kafka producer is thread safe
(defn- get-kafka-producer [ref-kafka thread-idx kafka-config-map]
  (let [kafka-sign (keyword (str ref-kafka))]
    (if (nil? (get @kafka-producer-map kafka-sign))
      (try
        (loop []
          (if (compare-and-set! init-kafka-producer-lock false true)
            (if (nil? (get @kafka-producer-map kafka-sign))
              (let [kafka-instance (init-kafka-Producer kafka-config-map)]
                ;; (prn (type kafka-instance))
                (timbre/info (str "Thread=" thread-idx ", successfully init kafka producer"))
                (swap! kafka-producer-map assoc kafka-sign kafka-instance)
                kafka-instance)
              (do
                (timbre/info (str "Thread=" thread-idx ",kafka instance already exists"))
                ;return the kafka instance
                (get @kafka-producer-map kafka-sign)))
            (do
              (timbre/warn (str "Thread=" thread-idx ",failed to get the lock for init kafka producer"))
              (Thread/sleep 10)
              (recur))))
        (finally
          (reset! init-kafka-producer-lock false)))
      (get @kafka-producer-map kafka-sign))))




; The parent node is either unique or empty
(defn- parent-node-id [node-graph node-id]
  (:node-id (first (task/get-parent-nodes node-graph node-id))))


;to_db ref to kafka configuration
;kafka configuration is in the datasource configuration
(defnode kwn [node-exec-info]
  (let [{:keys [task-execution-info node-result-dict node-execution-dict]} @node-exec-info
        {:keys [task-info task-execution-dict]} @task-execution-info
        {:keys [task-idx task-name task-config node-graph]} @task-info
        {:keys [kwn]} task-config
        {:keys [thread-idx]} @node-execution-dict
        {:keys [to_db topic]} kwn
        kafka-producer (get-kafka-producer to_db thread-idx (register/get-datasource-ins to_db))
        node-result-lst (get @node-result-dict (keyword (parent-node-id node-graph "kwn")))]
    (if (nil? node-result-lst)
      (timbre/warn (str "task_name=" task-name ", thread-idx=" thread-idx ", get parent result is nil, will don't send to kafka"))
      (cond 
        (seq? node-result-lst) 
        (doseq [item node-result-lst]
          (let [value-str (json/generate-string item)]
            (.send kafka-producer (new ProducerRecord topic nil value-str))))
        :else 
        (.send kafka-producer (new ProducerRecord topic nil (json/generate-string node-result-lst)))))))