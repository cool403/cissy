(ns cissy-kafka-read
  (:require
   [cissy.registry :refer [defnode] :as register]
   [cissy.task :as task]
   [taoensso.timbre :as timbre]
   [cissy.helpers :as helpers]
   [cheshire.core :as json])
  (:import [org.apache.kafka.clients.consumer KafkaConsumer ConsumerRecords]))

;global kafka instance map;key is the kafka instance name, value is the kafka instance
(def kafka-consumer-map (atom {}))
(def init-kafka-consumer-lock (atom false))

(def topic-sub-lock (atom false))
(def topic-vec (atom []))


(defn- init-kafka-consumer [kafka-config-map]
  (let [kafka-properties (java.util.Properties.)]
    (doseq [[k v] (-> kafka-config-map
                      (helpers/my-merge-fn {"key.deserializer" "org.apache.kafka.common.serialization.StringDeserializer"
                                            "value.deserializer" "org.apache.kafka.common.serialization.StringDeserializer"}))]
      (.put kafka-properties (name k) v))
    (new KafkaConsumer kafka-properties)))

; ensure that only one thread can get kafka successfully
; The KafkaConsumer class in Apache Kafka is designed to be used by a single thread
(defn- get-kafka-consumer [ref-kafka thread-idx kafka-config-map]
  (let [kafka-sign (keyword (str ref-kafka "_" thread-idx))]
    (if (nil? (get @kafka-consumer-map kafka-sign))
      (try
        (loop []
          (if (compare-and-set! init-kafka-consumer-lock false true)
            (if (nil? (get @kafka-consumer-map kafka-sign))
              (let [kafka-instance (init-kafka-consumer kafka-config-map)]
                ;; (prn (type kafka-instance))
                (timbre/info (str "Thread=" thread-idx "successfully init kafka"))
                (swap! kafka-consumer-map assoc kafka-sign kafka-instance)
                kafka-instance)
              (do
                (timbre/info (str "Thread=" thread-idx ",kafka instance already exists"))
                ;return the kafka instance
                (get @kafka-consumer-map kafka-sign)))
            (do
              (timbre/warn (str "Thread=" thread-idx ",failed to get the lock for init kafka"))
              (Thread/sleep 10)
              (recur))))
        (finally
          (reset! init-kafka-consumer-lock false)))
      (get @kafka-consumer-map kafka-sign))))

;aovid concurrent sub the topic
(comment (defn- sub-topic [thread-idx kafka-instance topic]
           (when-not (contains? @topic-vec topic)
             (try
               (loop []
                 (if (compare-and-set! topic-sub-lock false true)
                   (when-not (contains? @topic-vec topic)
                     (.subscribe kafka-instance (java.util.Arrays/asList (into-array String [topic])))
                     (timbre/info (str "Thread=" thread-idx ", subsribe the topic=" topic " success."))
                     (swap! topic-vec conj topic))
                   (do
                     (timbre/warn (str "Thread=" thread-idx ", failed to get the lock for register the consumer topic"))
                     (Thread/sleep 10)
                     (recur))))
               (finally
                 (reset! topic-sub-lock false))))))

(defn- val2json [val]
  (json/parse-string (str val) true))


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
    ;; (prn (type kafka-consumer))
    (.subscribe kafka-consumer (java.util.Arrays/asList (into-array String [topic])))
    ;poll data from kafka, if there is no data, the thread will be blocked,you can set the timeout
    (let [^ConsumerRecords records (.poll kafka-consumer (java.time.Duration/ofMillis Long/MAX_VALUE))]
      (timbre/info (str "task-name=" task-name ",thread-idx=" thread-idx ",poll records count=" (.count records)))
      ;transform the data to json format
      (vec (map #(val2json (.value %)) (into [] (iterator-seq (.iterator records))))))))
;; (doseq [record records]
;;   (let [topic (.topic record)
;;         value (.value record)
;;         partition (.partition record)
;;         offset (.offset record)
;;         headers (.headers record)
;;         timestamp (.timestamp record)] (timbre/info (str "Thread=" thread-idx ", topic=" topic ", value=" value ", partition=" partition ", offset=" offset ", headers=" headers ", timestamp=" timestamp))))
    
