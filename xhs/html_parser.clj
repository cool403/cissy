(ns xhs.html-parser
  (:require [taoensso.timbre :as timbre])
  (:import [com.vladsch.flexmark.html2md.converter FlexmarkHtmlConverter]))

;; Define a multi-method for parsing HTML content
(defmulti parse-html-content (fn [page-content] (:source page-content)))

;; FlexmarkHtmlConverter singleton
(defonce html-to-markdown-converter (-> (FlexmarkHtmlConverter/builder nil) (.build)))

;; html to markdown
(defn html-to-markdown [html]
  (timbre/info "html-to-markdown")
  (time (str (.convert html-to-markdown-converter html))))

;; parse xhs-index
(defmethod parse-html-content :xhs-index
  [{:keys [page content]}]
  (timbre/info "parse-html-content :xhs-index"))
