# Cissy

Cissy 是一个基于 Clojure的数据同步工具，支持多种数据库之间的数据迁移和同步。

## 功能特性

- 支持多种数据库：SQLite、MySQL、PostgreSQL、Oracle
- 支持分页查询和批量写入
- 基于配置文件的任务定义
- 支持数据同步进度跟踪
- 支持 JVM Clojure

## 依赖要求

### JVM 环境
- Clojure 1.11.1+
- Java 11+

## 安装

### 使用 deps.edn (JVM 环境)
```sh
clojure -T:build clean
clojure -T:build uber
```

### 运行
```sh
java -jar cissy.jar start -c path/to/config.json
```
clojure环境下可以使用如下命令启动
```sh
clojure -m cissy.app start -c /home/xxx/Desktop/group_tasks.json
```

### 配置文件
可以通过如下命令生成一个示例配置文件
```sh
java -jar cissy.jar demo
```

### 单元测试
`clojure`的单元测试必须以`_test`结尾, 执行命令
```sh
clojure -M:test
```


