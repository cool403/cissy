(ns xhs.url-manager)

;url-info: {:run-mode "db" :url-type "page" :page-url ""}
(defmulti add-url (fn [url-info] (:run-mode url-info)))

(defmethod add-url :db 
  [url-info])

(defmethod add-url :default
    [url-info])


;url-info: {:run-mode "db" :url-type "page" :page-url ""}
(defmulti get-url (fn [options] (:run-mode options)))

(defmethod get-url :db 
  [options])

(defmethod get-url :default
  [options])