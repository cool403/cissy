# 中文 | [英文](./README_EN.md)

# Cissy

Cissy 是一个基于 Clojure的数据同步工具，支持多种数据库之间的数据迁移和同步。

![License](https://img.shields.io/badge/license-Apache%202.0-blue.svg)

主要功能流程图
```mermaid
flowchart TD
    A[开始] --> B[解析EDN配置文件]
    B --> C[构建节点图TaskNodeGraph]
    
    subgraph 节点图验证
    C --> D[验证节点关系]
    D --> E[检查根节点唯一性]
    E --> F[检查节点父子关系]
    end
    
    F --> R{调度策略}
    
    subgraph 持续执行策略
    R -->|always| S[Channel调度器]
    S --> G1[根节点执行]
    G1 --> |数据流向| H1[子节点1]
    G1 --> |数据流向| H2[子节点2]
    H1 --> |Channel传输| I1[孙节点1]
    H2 --> |Channel传输| I2[孙节点2]
    
    H1 -.-> |状态监控| M1[节点监控器]
    H2 -.-> |状态监控| M1
    I1 -.-> |状态监控| M1
    I2 -.-> |状态监控| M1
    
    M1 --> |继续执行| G1
    end
    
    subgraph 单次执行策略
    R -->|once| T[一次性调度器]
    T --> G2[根节点执行]
    G2 --> |数据流向| H3[子节点1]
    G2 --> |数据流向| H4[子节点2]
    H3 --> |Channel传输| I3[孙节点1]
    H4 --> |Channel传输| I4[孙节点2]
    
    H3 -.-> |状态监控| M2[节点监控器]
    H4 -.-> |状态监控| M2
    I3 -.-> |状态监控| M2
    I4 -.-> |状态监控| M2
    end
    
    M1 --> |检查节点状态| N1{所有节点完成?}
    M2 --> |检查节点状态| N2{所有节点完成?}
    
    N1 -->|否| G1
    N1 -->|是| O1[重新开始执行]
    O1 --> G1
    
    N2 -->|否| G2
    N2 -->|是| O2[标记任务完成]
    O2 --> U[结束]
```
以节点执行为维度，功能流程图
```mermaid
flowchart TD
    A[开始] --> B[解析配置文件]
    B --> C[初始化任务]
    
    C --> D{任务类型}
    D -->|数据库同步| E[数据库节点]
    D -->|Kafka处理| F[Kafka节点]
    D -->|CSV导出| G[CSV节点]
    D -->|自定义ZIP任务| H[ZIP格式任务]
    
    E --> E1[DRN读取节点]
    E --> E2[DWN写入节点]
    
    F --> F1[KRN读取节点]
    F --> F2[KWN写入节点]
    
    G --> G1[数据读取]
    G1 --> G2[CSV写入]
    
    H --> H1[加载ZIP包]
    H1 --> H2[执行自定义节点]
    
    E1 --> I[Channel处理]
    E2 --> I
    F1 --> I
    F2 --> I
    G2 --> I
    H2 --> I
    
    I --> J[任务调度]
    J --> K[多线程执行]
    K --> L[监控执行状态]
    
    L --> M{是否完成}
    M -->|否| K
    M -->|是| N[结束]
```

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
java -jar cissy.jar start -c {edn格式配置的绝对路径}
```
[`edn`格式](https://github.com/edn-format/edn)
> `edn`相比`json` 更简单、支持更多数据类型、在`clojure`生态中更好用、可扩展性

clojure环境下可以使用如下命令启动
```sh
clojure -m cissy.app start -c {edn格式配置的绝对路径}
```

### 样例
可以通过如下命令生成一个示例配置文件
```sh
java -jar cissy.jar demo
```
如何想开发一个自定义的外挂`zip`格式任务节点，你可以使用
```
java -jar cissy.jar demo -z
```
生成一个`zip_task_demo.zip` 样例，默认保存在桌面目录下，然后基于这个样例开发，改压缩包中也提供`zip`格式任务一些配置示例


### 已内置节点列表
| 节点名称 | 节点主要功能 | 节点主要配置参数 |
| ---- | ---- | ---- |
| `drn` | 从数据库读取加载数据 |  [`drn`配置项](#drnConfigItem)  |
| `dwn` | 往数据库写入数据 | [`dwn`配置项](#dwnConfigItem)  |
| `csvw` | 往`csv`文件写入数据 | [`csvw`配置项](#csvwConfigItem) |
| `console` | 控制台打印数据,一般调试或者观察数据用 | 无 |
| `krn` | 从`kafka`中`poll`数据 | [`krn`配置项](#krnConfigItem) |

<a name="drnConfigItem"></a>
#### `drn`配置项
```edn
{
    :from_table   "users"
    :from_db      "xx"
    :sql_template "select * from users1 order by id"
    :threads      1
    :page_size    10000
}
```

<a name="dwnConfigItem"></a>
#### `dwn`配置项
```edn
{
    :to_table "users"
    :from_db  "xx"
    :threads  2
}
```

<a name="csvwConfigItem"></a>
#### `csvw`配置项
```edn
{
    :target_file "{绝对路径/xx.csv}"
    :threads     1
}
```

<a name="krnConfigItem"></a>
#### `krn`配置项
```edn
{
    :topic "test-topic"
    :from_db "main"
    :threads 5
}
```

> 自定义节点时，节点名称不能和内置冲突，可能导致一些问题

### 单元测试
`clojure`的单元测试必须以`_test`结尾, 执行命令
```sh
clojure -M:test
```


