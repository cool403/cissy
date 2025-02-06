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

### 开发日志
- [x] 2025-01-05 修复解决多线程执行下，select 数据错序导致插入冲突而导致失败，配置上order by 后，可以解决这个问题
- [x] 监控channel的执行情况;`2025-01-08`,定义了一个节点停止干活原则，当一个节点的所有父节点`done`，当前节点也标记`done`，当一个根节点的所有子节点都是`done`，根节点也标记`done`
- [x] 100万数据测试同步还是出现主键冲突问题;2025-01-11 使用STM而非atom 解决并发导致的数据不一致问题
- ~~支持断点继续执行~~
- [x] 支持多长张表同步
- [x] 修复simple_task_test测试一直无法停止;`202501117`
- [x] `20250119` 新增宏`defnode`,可实现自动注册节点方法,避免手工调用节点注册方法
- [x] `20250122` 配置文件格式替换使用`edn`格式,`json`解析先保留
- [x] `20250202` 解决使用`deps/resolve-deps`因为`deps-map`参数中没有传递`mvn/repo`参数导致每次加载都提示`Caused by: org.eclipse.aether.transfer.ArtifactNotFoundException: Could not find artifact org.apache:apache:pom:21`
- [x] `20250204` 解决`deps`加载成功,但是加载脚本仍然报找不到依赖问题，主要原因是因为加载脚本所使用的`classloader`和加载`deps`使用的不一致
- [x] `20250206` 解决`zip`格式任务单元测试能执行成功，但是使用命令行执行`java -jar cissy.jar`提示`Exception in thread "main" java.lang.IllegalStateException: Can't change/establish root binding of: null with set`问题
- [x] `20250206` 新增全局任务配置项`sched_type`, 如果配置的是`once`,此次任务将只会执行一次，不会一直执行，默认策略是`always`
- [ ] 基于`cissy`封装一个爬虫`demo`


