(ns cissy.const)

;db配置前缀
(def DB_SUFFIX_KEY "_db")

;drn节点名称
(def DRN_NODE_NAME "drn") ;数据库读取节点
(def DWN_NODE_NAME "dwn") ;数据库写节点


;支持的数据库类型
(def SUPPORTED_DB_TYPE #{"mysql" "postgresql","oracle","sqlite"})