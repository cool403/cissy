(ns xhs.parser
  (:require [taoensso.timbre :as timbre]
            [xhs.llm :as llm])
  (:import [com.vladsch.flexmark.html2md.converter FlexmarkHtmlConverter]))

;; Define a multi-method for parsing HTML content
(defmulti parse-html-content (fn [page-content] [(:content-type page-content) (:parse-type page-content)]))

;; FlexmarkHtmlConverter singleton
(defonce html-to-markdown-converter (-> (FlexmarkHtmlConverter/builder nil) (.build)))

;; html to markdown
(defn html-to-markdown [html]
  (timbre/info "html-to-markdown")
  (time (str (.convert html-to-markdown-converter html))))

;; parse xhs-index
(defmethod parse-html-content [:home-page :llm]
  [{:keys [page content]} :as page-content]
  (timbre/info "parse-html-content :home-page with llm")
  (let [markdown-content (html-to-markdown content)]
    (try
      (let [posts-list (llm/completion-request markdown-content)]
        (timbre/info "llm parse home-page result=" posts-list))
      (catch Exception e
        (timbre/error "Error when parsing home-page with llm" e)))))



(defmethod parse-html-content [:home-page :regex]
  [{:keys [page content]} :as page-content]
  (timbre/info "parse-html-content :home-page with regex"))
