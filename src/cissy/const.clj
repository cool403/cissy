(ns cissy.const)

; Database configuration prefix
(defonce db-suffix "_db")

; drn node name
(defonce drn "drn")                                             ; Database read node
(defonce dwn "dwn")                                             ; Database write node

(defonce csvw "csvw")                                           ; CSV write node

; Supported database types
(defonce db-types #{"mysql" "postgresql", "oracle", "sqlite" "kafka"})