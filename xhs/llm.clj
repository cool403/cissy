(ns xhs.llm
  (:require
  [clojure.java.io :as io]
  [clojure.string :as str] 
   [clojure.data.json :as json])
 (:import
  [org.apache.hc.core5.http.message BasicHttpRequest]
  [org.apache.hc.client5.http.classic.methods HttpPost]
  [org.apache.hc.client5.http.impl.classic HttpClients]
  [org.apache.hc.core5.http.io.entity EntityUtils StringEntity]))

;; deepseek llm config
(defonce openai-config 
  {:api-key  "sk-39253fa2d0074d728883b90e4955aa6f"
   :base-url "https://api.deepseek.com/beta/"})

;; openai-client
(defonce openai-client (HttpClients/createDefault))

;; wrapper rquest
(defn- wrapper-request [^BasicHttpRequest request]
  ;; (timbre/info "Wrapper request: " request)
  (let [request (doto request
                  (.setHeader "Authorization" (str "Bearer " (:api-key openai-config)))
                  (.setHeader "Content-Type" "application/json"))]
    request))


;;completion request
(defn completion-request 
  ([^String prompt] (completion-request prompt {:model "deepseek-chat"}))
  ([^String prompt options]
   (let [request (-> (new HttpPost (str (:base-url openai-config) "/v1/completions")) wrapper-request)
         entity (StringEntity. (json/write-str (assoc options :prompt prompt)))]
     (.setEntity request entity)
     (let [res (.execute openai-client request)
           status (.getCode res)]
       (if (= status 200)
         ;;extract the text 
         (-> (.getEntity res) EntityUtils/toString (json/read-str :key-fn keyword) :choices first :text)
         (throw (Exception. (str "HTTP status: " status ",content: " (.getContent res)))))))))
