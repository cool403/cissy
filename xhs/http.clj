(ns xhs.http
  (:require
   [taoensso.timbre :as timbre]) 
  (:import
   [org.apache.hc.core5.http.message BasicHttpRequest]
   [org.apache.hc.client5.http.classic.methods HttpGet]
   [org.apache.hc.client5.http.impl.classic HttpClients]
   [org.apache.hc.core5.http.io.entity EntityUtils]))

;; wrap a http request
;; set the default headers
;; set the default cookies
(defn wrapper-request [^BasicHttpRequest request]
  (timbre/info "Wrapper request: " request)
  (doto request
    (.setHeader "User-Agent" "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/133.0.0.0 Safari/537.36 Edg/133.0.0.0")
    (.setHeader "Accept" "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7")
    (.setHeader "Accept-Language" "zh-CN,zh;q=0.9,en;q=0.8,en-GB;q=0.7,en-US;q=0.6")
    (.setHeader "Accept-Encoding" "gzip, deflate, br, zstd")
    (.setHeader "Connection" "keep-alive")
    (.setHeader "Cache-Control" "max-age=0")
    (.setHeader "Upgrade-Insecure-Requests" "1")))

;; create a default http client
(defonce http-client (HttpClients/createDefault))

;; get the response content as string
(defn http-get [^String url]
  (let [request (-> (new HttpGet url)
                   (wrapper-request))
        response (.execute http-client request)
        status (.getStatusCode response)]
    (if (= status 200)
      (EntityUtils/toString (.getEntity response))
      (throw (Exception. (str "HTTP status: " status ",content: " (.getContent response)))))))


