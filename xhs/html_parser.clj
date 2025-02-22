(ns xhs.html-parser
  (:require [taoensso.timbre :as timbre]))

;; Define a multi-method for parsing HTML content
(defmulti parse-html-content (fn [page-content] (:source page-content)))

;; parse xhs-index
(defmethod parse-html-content :xhs-index
    [{:keys [page content]}]
  (timbre/info "parse-html-content :xhs-index"))
