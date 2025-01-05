# Cissy

Cissy 是一个基于 Clojure/Babashka 的数据同步工具，支持多种数据库之间的数据迁移和同步。

## 功能特性

- 支持多种数据库：SQLite、MySQL、PostgreSQL
- 支持分页查询和批量写入
- 基于配置文件的任务定义
- 支持数据同步进度跟踪
- 支持 JVM Clojure 和 Babashka 两种运行环境

## 依赖要求

### JVM 环境
- Clojure 1.11.1+
- Java 11+

### Babashka 环境
- Babashka 1.3.0+
- 必要的数据库 pods:
  - org.babashka/postgresql
  - org.babashka/go-sqlite3
  - org.babashka/mysql

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

### 配置文件
可以通过如下命令生成一个示例配置文件
```sh
java -jar cissy.jar demo
```

### 开发日志
- [x] 2025-01-05 修复解决多线程执行下，select 数据错序导致插入冲突而导致失败，配置上order by 后，可以解决这个问题
- [ ] 支持断点继续执行
- [ ] 监控channel的执行情况


