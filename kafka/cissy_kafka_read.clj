(ns cissy-kafka-read
  (:require
   [cissy.registry :refer [defnode] :as register]
   [cissy.task :as task]
   [taoensso.timbre :as timbre]
   [cissy.helpers :as helpers])
  (:import [org.apache.kafka.clients.consumer KafkaConsumer]))

;global kafka instance map;key is the kafka instance name, value is the kafka instance
(def kafka-consumer-map (atom {}))
(def init-kafka-consumer-lock (atom false))


(defn- init-kafka-consumer [kafka-config-map]
  (let [kafka-properties (java.util.Properties.)]
    (doseq [[k v] (-> kafka-config-map
                      (helpers/my-merge-fn {
                                            "key.deserializer" "org.apache.kafka.common.serialization.StringDeserializer"
                                            "value.deserializer" "org.apache.kafka.common.serialization.StringDeserializer"}))]
      (.put kafka-properties (name k) v))
    (new KafkaConsumer kafka-properties)))

; ensure that only one thread can get kafka successfully
(defn- get-kafka-consumer [ref-kafka thread-idx kafka-config-map]
  (try
    (loop []
      (if (compare-and-set! init-kafka-consumer-lock false true)
        (if (nil? (get @kafka-consumer-map (keyword ref-kafka)))
          (let [kafka-instance (init-kafka-consumer kafka-config-map)]
            ;; (prn (type kafka-instance))
            (timbre/info (str "Thread=" thread-idx "successfully init kafka"))
            (swap! kafka-consumer-map assoc (keyword ref-kafka) kafka-instance)
            kafka-instance)
          (do
            (timbre/info (str "Thread=" thread-idx ",kafka instance already exists"))
            ;return the kafka instance
            (get @kafka-consumer-map (keyword ref-kafka))))
        (do
          (timbre/warn (str "Thread=" thread-idx "failed to get the lock for init kafka"))
          (Thread/sleep 10)
          (recur))))
    (finally
      (reset! init-kafka-consumer-lock false))))


;kafka startup node, pull data from kafka
;from_db ref to kafka configuration
;kafka configuration is in the datasource configuration
(defnode krn [node-exec-info]
  (let [{:keys [task-execution-info node-result-dict node-execution-dict]} @node-exec-info
        {:keys [task-info task-execution-dict]} @task-execution-info
        {:keys [task-idx task-name task-config node-graph]} @task-info
        {:keys [krn]} task-config
        {:keys [thread-idx]} @node-execution-dict
        {:keys [from_db topic]} krn
        kafka-consumer (get-kafka-consumer from_db thread-idx (register/get-datasource-ins from_db))] 
    (prn (type kafka-consumer))
    (.subscribe kafka-consumer (java.util.Arrays/asList (into-array String [topic])))
    ;poll data from kafka, if there is no data, the thread will be blocked,you can set the timeout
    (let [records (.poll kafka-consumer (java.time.Duration/ofMillis Long/MAX_VALUE))]
      (doseq [record records]
        (let [topic (.topic record)
              value (.value record)
              partition (.partition record)
              offset (.offset record)
              headers (.headers record)
              timestamp (.timestamp record)]
        (timbre/info (str "Thread=" thread-idx ", topic=" topic ", value=" value ", partition=" partition ", offset=" offset ", headers=" headers ", timestamp=" timestamp)))))))
    

