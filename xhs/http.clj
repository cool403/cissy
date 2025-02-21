(ns xhs.http
  (:require
   [taoensso.timbre :as timbre]
   [clojure.java.io :as io]
   [clojure.string :as str])
  (:import
   [org.apache.hc.core5.http.message BasicHttpRequest]
   [org.apache.hc.client5.http.classic.methods HttpGet]
   [org.apache.hc.client5.http.impl.classic HttpClients]
   [org.apache.hc.core5.http.io.entity EntityUtils]))

;; global cookie info map, one key is cookie key,
;; another key is last visit time
(def exist-cookie (atom {}))

;; parse cookie line
(defn- parse-cookie-line [^String line]
  (let [fields (str/split line #"\t")]
    {:domain (nth fields 0)
     :flag (nth fields 1)
     :path (nth fields 2)
     :secure (nth fields 3)
     :expiration (nth fields 4)
     :name (nth fields 5)
     :value (nth fields 6)}))

(def five-hour-in-seconds (* 5 60 60))

;; load cookie from file
(defn- load-cookie [^String cookie-file-path]
  (timbre/info "Load cookie from file: " cookie-file-path)
  (let [rdr (io/reader cookie-file-path)]
    (into {} (map #(partial parse-cookie-line (str/join "\t" %)) (line-seq rdr)))))

;; get cookies
(defn- get-cookies [^String cookie-file-path]
  (timbre/info "Load cookie from file: " cookie-file-path)
  ;; check if the cookie exist and  the time is valid
  (let [{:keys [cookie last_load_time]} @exist-cookie]
    (if (or (nil? cookie) (> (- (System/currentTimeMillis) last_load_time) five-hour-in-seconds))
      (do
        (timbre/info "Reload cookie from file: " cookie-file-path)
        (swap! exist-cookie assoc :cookie (load-cookie cookie-file-path) :last_load_time (System/currentTimeMillis)))
      cookie)))

;; wrap a http request
;; set the default headers
;; set the default cookies
(defn wrapper-request [^BasicHttpRequest request cookie-file]
  (timbre/info "Wrapper request: " request)
  (let [request (doto request
                  (.setHeader "User-Agent" "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/133.0.0.0 Safari/537.36 Edg/133.0.0.0")
                  (.setHeader "Accept" "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7")
                  (.setHeader "Accept-Language" "zh-CN,zh;q=0.9,en;q=0.8,en-GB;q=0.7,en-US;q=0.6")
                  (.setHeader "Accept-Encoding" "gzip, deflate, br, zstd")
                  (.setHeader "Connection" "keep-alive")
                  (.setHeader "Cache-Control" "max-age=0")
                  (.setHeader "Upgrade-Insecure-Requests" "1"))]
    (when cookie-file
      (let [cookie (get-cookies cookie-file)
            cookie-header (str/join "; " (map #(str (:name %) "=" (:value %)) cookie))]
        (.setHeader request "Cookie" cookie-header)))
    request))

;; create a default http client
(defonce http-client (HttpClients/createDefault))

;; get the response content as string
(defn http-get [get-dict]
  (let [request (-> (new HttpGet (:page_url get-dict))
                    (wrapper-request (:cookie_file get-dict)))
        response (.execute http-client request)
        status (.getStatusCode response)]
    (if (= status 200)
      (EntityUtils/toString (.getEntity response))
      (throw (Exception. (str "HTTP status: " status ",content: " (.getContent response)))))))


