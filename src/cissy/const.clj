(ns cissy.const)

;db配置前缀
(def db-suffix "_db")

;drn节点名称
(def drn "drn")                                             ;数据库读取节点
(def dwn "dwn")                                             ;数据库写节点


;支持的数据库类型
(def db-types #{"mysql" "postgresql", "oracle", "sqlite"})