# 20260826 ClickHouse 数据源配置 Hardness

## 元信息
- 任务 ID：`20260826-clickhouse-datasource-config`
- 任务名称：后台 ClickHouse 分析数据源配置
- 分级：H2
- 负责人：Codex
- 开始时间：2026-08-26
- 关联需求：参考 MySQL 配置方式接入 ClickHouse

## 目标与非目标
- 目标：增加官方 ClickHouse JDBC 驱动和环境固定动态数据源。
- 目标：开发、生产与 Docker 环境通过环境变量配置连接信息。
- 目标：ClickHouse 与 MySQL 使用一致的环境配置组织方式。
- 非目标：不创建业务表，不迁移 MySQL 数据，不新增查询接口。

## 输入约束
- 已知上下文：项目使用 `dynamic-datasource-spring-boot3-starter`，默认数据源为 `master`。
- 强约束：MySQL 继续作为主数据源；ClickHouse 数据源名称固定为 `clickhouse`。
- 强约束：配置不得新增真实生产凭证。
- 禁止事项：ClickHouse 不参与 Flyway、Quartz 和现有 MySQL 本地事务。

## 任务分解

### T1 JDBC 依赖
- 输入：根 POM 与 `jeecg-boot-base-core` 驱动依赖。
- 执行动作：增加 ClickHouse JDBC 版本属性和运行时依赖。
- 输出：启动模块运行时可加载 `com.clickhouse.jdbc.ClickHouseDriver`。
- 验收标准：Maven 依赖解析和编译成功。
- 证据类型：Maven 命令输出。

### T2 Spring 数据源
- 输入：现有 `spring.datasource.dynamic.datasource.master` 配置。
- 执行动作：在 `dev/prod/docker` 环境中新增与 `master` 同级的第二数据源配置。
- 输出：`@DS("clickhouse")` 可选择 ClickHouse，各环境自动注册。
- 验收标准：YAML 可解析，默认 dev Profile 编译与资源处理不受影响。
- 证据类型：配置解析、资源产物和差异检查。

### T3 Docker 与文档
- 输入：单体 Compose、`.env` 和配置文档。
- 执行动作：增加 ClickHouse LTS 容器、健康检查、持久化和变量说明。
- 输出：Docker 环境具备 ClickHouse 服务及后台连接参数。
- 验收标准：Compose 配置解析成功，文档说明启用方式与边界。
- 证据类型：Compose 命令输出、文档差异。

## 验证矩阵
| 验证项 | 方法 | 阈值/期望 | 结果证据 |
|---|---|---|---|
| YAML 语法 | YAML 解析 | 全部通过 | 命令输出 |
| Compose 语法 | `docker compose config` | 退出码 0 | 命令输出 |
| JDBC 依赖 | Maven 解析 | 驱动可下载 | 命令输出 |
| 模块编译 | Maven compile | BUILD SUCCESS | 命令输出 |
| 默认回归 | dev/prod/docker 环境加载 | MySQL 仍为 `master` | 配置差异 |

## 上下文与防漏策略
- 上下文预算：依赖、Spring 配置、Docker/文档、验证四阶段。
- 分段策略：每完成一个阶段立即检查差异和编码。
- 压缩策略：保留目标文件、启用方式、风险和验证结果。
- 恢复策略：从本文件与 `PLANS.md` 当前任务恢复。

## 风险与回退
- 风险：连接池继承 MySQL 校验 SQL；ClickHouse 数据源必须覆盖为 `SELECT 1`。
- 风险：ClickHouse 服务不可达时，首次使用该数据源会连接失败。
- 回退：删除各环境 ClickHouse 数据源和驱动依赖，移除 Compose ClickHouse 服务与环境变量。

## 完成定义
- [x] JDBC 依赖完成。
- [x] Spring 数据源配置完成。
- [x] Docker 与文档完成。
- [x] 验证矩阵执行并记录。
- [x] 未完成项显式列出。

## 未完成项
- 未启动真实 ClickHouse 容器，未执行连接与建表冒烟。

## 验证结果
- YAML 解析：通过。
- 两套 Compose 配置解析：通过；根 Compose 仅提示既有 `version` 字段已过时。
- Maven 依赖树：`com.clickhouse:clickhouse-jdbc:jar:all:0.9.8:runtime`。
- JDBC 驱动类：`com/clickhouse/jdbc/ClickHouseDriver.class` 存在。
- 模块编译：10 个 Reactor 模块 `BUILD SUCCESS`。
- 静态差异：`git diff --check` 通过。
