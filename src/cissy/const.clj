(ns cissy.const)

; Database configuration prefix
(def db-suffix "_db")

; drn node name
(def drn "drn")                                             ; Database read node
(def dwn "dwn")                                             ; Database write node

(def csvw "csvw")                                           ; CSV write node

; Supported database types
(def db-types #{"mysql" "postgresql", "oracle", "sqlite" "kafka"})