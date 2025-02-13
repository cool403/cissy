(ns cissy.registry
  (:require
    ;;  [clojure.core :as core]
    [cissy.const :as const]
    [taoensso.timbre :as timbre])
  (:import (cissy.executions NodeExecutionInfo)
           java.lang.IllegalArgumentException))

; Registry
(def task-node-register (atom {}))
; Register atom listener
;; (add-watch )

(def param-nei? (fn [param]
                  (when-not (instance? NodeExecutionInfo @param)
                    (throw (IllegalArgumentException. "Parameter must be of type NodeExecutionInfo, see cissy.executions namespace for definition")))))

(comment
  (defn a [x] (inc x))
  (reset! task-node-register (assoc @task-node-register :12 a))
  ((:12 @task-node-register) 12))

; Unified default parameter name is the method name keyword
(defmacro defnode [name params & body]
  (when-not (= 1 (count params) 1)
    (throw (IllegalArgumentException. (str "Node method " name " can only have one parameter"))))
  (let [[param1] params
        type-checks `(param-nei? ~param1)]
    `(do
       ;; Define method
       (defn ~name ~params
         ;~@(when docstring [docstring])
         ; Check
         ~type-checks
         ~@body)
       ;; Register method
       (reset! task-node-register (assoc @task-node-register ~(keyword `~name) ~name))
       ;; Return method symbol
       ~name
       )))

; Register task-node
;(defn regist-node-fun [node-id func]
;  ; If already registered, do not register again
;  (when-not (contains? @task-node-register (keyword node-id))
;    (timbre/info (str "Start registering node=" node-id " and execution function=" func))
;    (compare-and-set! task-node-register @task-node-register
;                      (assoc @task-node-register (keyword node-id) func))))

; Get associated function
(defn get-node-func [node-id]
  ; If the registered function is not included, report an error
  ;; (prn node-id)
  (if-not (contains? @task-node-register (keyword node-id))
    (throw (IllegalArgumentException. (str "Node-id=" node-id " registered node not found, please register the node first")))
    (get @task-node-register (keyword node-id))))

;(comment
;  (defn test-node []
;    (prn "test node"))
;  (regist-node-fun "test" test-node)
;  ((get-node-func "test")))

; db register
(def datasource-ins-register (atom {}))

; oracle, mysql, pg are in this format; sqlite is special
; Conventionally, the sqlite file is still in the host, just special handling at that time, dbtype enumeration values: mysql, postgresql
; oracle
(comment (def db {:dbtype   "postgresql"
                  :host     "your-db-host-name"
                  :dbname   "your-db"
                  :user     "develop"
                  :password "develop"
                  :port     5432}))
(defn register-datasource
  "Register a datasource"
  [^String db-sign datasource-config]
  (when-not (contains? @datasource-ins-register (keyword db-sign))
    ; If not included, instantiate first
    (if-let [_ (contains? const/db-types (:dbtype datasource-config))]
      (cond
        (= (:dbtype datasource-config) "sqlite") (reset! datasource-ins-register (assoc @datasource-ins-register (keyword db-sign) (:host datasource-config)))
        :else
        (reset! datasource-ins-register (assoc @datasource-ins-register (keyword db-sign) datasource-config)))
      (do
        (timbre/error "Datasource configuration must have dbtype attribute, and valid values are only oracle, mysql, sqlite, postgresql")
        (throw (IllegalArgumentException. "Datasource configuration must have dbtype attribute, and valid values are only oracle, mysql, sqlite, postgresql"))))))

(defn get-datasource-ins [db-sign]
  ; Get registered datasource
  (if-not (contains? @datasource-ins-register (keyword db-sign)) (throw (IllegalArgumentException. (str "Registered datasource for db-sign " db-sign " not found")))
                                                                 (get @datasource-ins-register (keyword db-sign))))

(comment (get-datasource-ins "21"))
;; (def db1 {:dbtype "postgresql", :host "localhost", :user "hello", :password "123456", :port 5432})
;; (register-datasource "from-db" db1)
;; (def db5 {:dbtype "sqlite", :host "/home/mawdx/mywork/jissy/jissy-tests/jissy.db"})
;; (register-datasource "db5" db5)
;; (def db6 {:dbtype "sqlit2e", :host "/home/mawdx/mywork/jissy/jissy-tests/jissy.db"})
;; (register-datasource "db6" db6)

;; (comment
;;   (mysql-sql/insert-multi! aa :users ["id" "username"] [{:users/id 22222222,
;;                                                          :users/username "njones"}
;;                                                         {:users/id 22222223,
;;                                                          :users/username "fzuniga"}])
;;   )

