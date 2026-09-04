# 配置说明与环境策略

## 1. 配置文件分层

### 1.1 系统主配置（`jeecg-system-start`）
- 目录：`jeecg-module-system/jeecg-system-start/src/main/resources`
- 核心文件：
  - `application.yml`（统一入口，定义 `spring.application.name` 与 profile 占位符）
  - `application-dev.yml`
  - `application-test.yml`
  - `application-prod.yml`
  - `application-docker.yml`
  - 数据库方言配置：`application-dm8.yml`、`application-oracle.yml`、`application-postgresql.yml`、`application-sqlserver.yml`、`application-kingbase8.yml`
  - 属性覆盖文件：`application-dev.properties`、`application-prod.properties`

### 1.2 AIRAG 模块配置
- 目录：`jeecg-boot-module/jeecg-boot-module-airag/src/main/resources`
- 文件：
  - `application.yml`
  - `application.properties`

## 2. Profile 激活规则
- `application.yml` 中 `spring.profiles.active` 使用 `@profile.name@`。
- 根 `pom.xml` 开启资源过滤（`<filtering>true</filtering>`），由 Maven Profile 注入：
  - `dev`（默认）
  - `test`
  - `docker`
  - `prod`
- `SpringCloud` Profile 会附加云模块 `jeecg-server-cloud`。

## 3. 关键配置项（摘要）
- 服务基础：
  - `server.port`
  - `server.servlet.context-path`（默认 `/jeecg-boot`）
  - `spring.application.name`（`jeecg-system`）
- 数据与缓存：
  - `spring.datasource.*`
  - `spring.data.redis.*`
- 运行与运维：
  - `management.endpoints.web.exposure.include`
  - `management.health.*`
- AIRAG / MiniMax：
  - `MINIMAX_API_KEY`
  - `MINIMAX_BASE_URL`
  - `MINIMAX_CHAT_MODEL`
  - `AIRAG_MINIMAX_*`

## 4. 安全规范（强制）
- 禁止在仓库中提交真实密钥、令牌、密码、对象存储凭证。
- 示例配置仅允许占位符：`${ENV_VAR}` 或脱敏值（如 `***`）。
- 对外发布前必须做一次密钥扫描与轮换检查。

## 5. 变更流程
当新增或修改配置时：
1. 在对应 `application*` 文件中改动配置。
2. 在本文件记录配置用途、默认值、环境差异。
3. 在 `docs/changelog.md` 增加一条记录。
4. 若影响架构策略，新增 ADR（`docs/decisions/*.md`）。

## 6. 2026-04-01 MiniMax 配置迁移
- MiniMax 相关配置统一放置到 `jeecg-system-start`：
  - `application-dev.yml` / `application-prod.yml`（结构化键）
  - `application-dev.properties` / `application-prod.properties`（环境值）
- 关键键包括：
  - `spring.ai.minimax.*`
  - `jeecg.airag.minimax.*`
  - `MINIMAX_*`
  - `AIRAG_MINIMAX_*`
- `jeecg-boot-module-airag/src/main/resources` 中的 MiniMax 配置已移除。
- `prompts` 资源目录已迁移到 `jeecg-system-biz/src/main/resources/prompts`，由系统模块统一提供 classpath 资源。

## 7. 2026-08-29 业务行为 Kafka 配置
- `KAFKA_BOOTSTRAP_SERVERS`：Broker 地址；`application-dev.properties` 默认 `localhost:9092`，`application-prod.properties` 和单体 Docker Compose 默认 `jeecg-boot-kafka:9092`，外部环境变量可覆盖。
- `TS_BEHAVIOR_KAFKA_ENABLED`：行为生产和消费开关，默认 `false`。
- `TS_BEHAVIOR_KAFKA_TOPIC`：主 Topic，默认 `ts.user-behavior.v1`。
- `TS_BEHAVIOR_KAFKA_DLQ_TOPIC`：死信 Topic，默认 `ts.user-behavior.dlq.v1`。
- `TS_BEHAVIOR_KAFKA_DETAIL_GROUP`：ClickHouse 明细消费者组。
- `TS_BEHAVIOR_KAFKA_PARTITIONS` / `TS_BEHAVIOR_KAFKA_REPLICAS`：Topic 分区与副本数，默认 `6/1`。
- `TS_BEHAVIOR_MAX_BATCH_SIZE`：单批最大事件数，默认 `100`。
- `TS_BEHAVIOR_MAX_PROPERTIES_BYTES`：单条扩展 JSON 最大字节数，默认 `8192`。

Producer 开启幂等、`acks=all` 和 LZ4 压缩；消费者失败重试两次后进入死信 Topic。
正式启用前必须创建 ClickHouse `ts_user_behavior_event` 表，初始化脚本为
`docker-deploy/monolith/clickhouse/init/01_user_behavior_event.sql`。
事件 v3 会额外写入内容版本、标签 ID 和标签分数快照。已有 ClickHouse 数据卷
不会重复执行容器初始化脚本，需要手动执行
`docker-deploy/monolith/clickhouse/init/02_user_behavior_tag_snapshot.sql`；
全新数据卷由 `01_user_behavior_event.sql` 直接创建完整字段。

### 7.1 Docker 部署
- 两套 Compose 均提供 `jeecg-boot-kafka` 单节点 KRaft Broker，并与后台服务加入同一 Docker 网络。
- Kafka 数据持久化到 Compose 目录下的 `kafka/data`，内部监听 `9092`，供 Docker 网络内的后台服务通过 `jeecg-boot-kafka:9092` 访问。
- Kafka 外部调试监听 `9094` 映射到宿主机，端口可通过 `KAFKA_EXTERNAL_PORT` 调整；`KAFKA_EXTERNAL_HOST` 必须配置为外部调试客户端可访问的服务器 IP 或域名。
- Kafka Controller 监听 `9093` 仅用于容器内部，不对宿主机暴露。
- Docker 环境默认仍为 `TS_BEHAVIOR_KAFKA_ENABLED=false`；开启前需先确认 Broker、Topic 和 ClickHouse 明细消费者链路。
- 当前 `TS_BEHAVIOR_KAFKA_PARTITIONS=6`、`TS_BEHAVIOR_KAFKA_REPLICAS=1` 仅适用于单节点开发/测试部署，生产集群需要按 Broker 数量调整。

## 8. 2026-08-23 AI 文本审核模型
- Agent 审核默认使用当前 Agent 应用绑定的 AIRAG 文本模型。
- 公共 Prompt Chat 审核使用当前 Prompt Chat 已解析出的 AIRAG 文本模型。
- MiniMax 普通聊天与图片 Prompt 审核复用：
  - `jeecg.airag.prompt-chat.model-id`
  - 或 `jeecg.airag.prompt-chat.app-id` 对应应用绑定的文本模型。
- 两项均未配置或模型不可用时，审核按失败关闭处理：输入不进入主模型，输出不直接返回用户。
- 审核日志不记录完整原文，只记录阶段、类别、分数、动作、服务、时间、内容长度和 SHA-256 摘要前缀。

## 9. 2026-08-26 ClickHouse 分析数据源
- MySQL `master` 继续作为默认事务数据源；ClickHouse 仅用于行为、广告、AI 日志等分析查询。
- ClickHouse 与 MySQL 一样配置在 `application-dev.yml`、`application-prod.yml`、`application-docker.yml` 的 `spring.datasource.dynamic.datasource` 下，数据源名称固定为 `clickhouse`。
- 开发和生产连接值分别放在 `application-dev.properties`、`application-prod.properties`，Docker 连接值通过环境变量注入。
- 后续分析 Service 或 Mapper 必须显式使用 `@DS("clickhouse")`，禁止将 MySQL 事务写入 ClickHouse。

### 9.1 加载方式
- 本地开发：启用现有 `dev` Profile，自动加载 `application-dev.properties` 中的 ClickHouse 连接值。
- 生产环境：启用现有 `prod` Profile，自动加载 `application-prod.properties` 中的 ClickHouse 连接值。
- Docker 环境：启用现有 `docker` Profile，自动读取 Compose 注入的 ClickHouse 环境变量。
- `application-prod.properties` 已默认使用 `jeecg-boot-clickhouse:8123`，即使部署 `.env` 未提供 `CLICKHOUSE_URL`，生产容器仍不会回退到 `localhost`。
- 不再需要额外的 `clickhouse` Profile 或 `SPRING_PROFILES_INCLUDE`。

### 9.2 环境变量
- `CLICKHOUSE_URL`：JDBC 地址；本地示例为 `jdbc:clickhouse:http://localhost:8123/jeecg_analytics`。
- `CLICKHOUSE_USERNAME` / `CLICKHOUSE_PASSWORD`：连接用户名和密码，生产环境必须覆盖示例值。
- `CLICKHOUSE_DB`：Docker 初始化数据库名，默认 `jeecg_analytics`。
- `CLICKHOUSE_HTTP_PORT` / `CLICKHOUSE_NATIVE_PORT`：Docker 映射端口，默认 `8123/9000`。
- `CLICKHOUSE_POOL_MAX_ACTIVE` / `CLICKHOUSE_POOL_MAX_WAIT`：连接池最大连接数和最大等待毫秒数，默认 `20/60000`。

### 9.3 部署边界
- Docker 使用固定 ClickHouse `26.3.22.7` LTS 镜像，并持久化 `data` 与 `log` 目录。
- JDBC 驱动使用官方 `com.clickhouse:clickhouse-jdbc:0.9.8:all`，驱动类为 `com.clickhouse.jdbc.ClickHouseDriver`。
- ClickHouse 不参与当前 Flyway、Quartz、MySQL 本地事务和主数据源健康判断。
- 各环境都会注册 ClickHouse 数据源；服务不可达时，首次执行 `@DS("clickhouse")` 查询会失败。
- 业务行为明细表由 `docker-deploy/monolith/clickhouse/init/01_user_behavior_event.sql` 初始化；
  已有行为表通过 `docker-deploy/monolith/clickhouse/init/02_user_behavior_tag_snapshot.sql`
  增加 `content_version/tag_ids/tag_scores`；
  其他分析表、冷热分层和集群高可用仍需按具体场景另行设计。

## 10. 2026-08-29 Postiz 共享基础设施

### 10.1 部署边界
- Jeecg 默认 Jenkins 发布不启动 Postiz；只有参数 `DEPLOY_POSTIZ=true` 时执行独立部署阶段。
- `jeecg-boot-pgvector` 位于单体 Compose 的 `postiz` profile，数据持久化到固定
  Docker named volume `jeecg-postgres-data`。
- Postiz 使用独立数据库 `postiz` 和独立账号，不复用 Jeecg MySQL 主库或 `vector_db`。
- Postiz 复用 `jeecg-boot-redis` 的 DB 1；Jeecg 保持使用 DB 0。
- Temporal 使用独立 PostgreSQL，未部署 Elasticsearch；Temporal UI 位于可选 `tools` profile。

### 10.2 Jenkins 参数与凭据
- 参数 `DEPLOY_POSTIZ`：是否部署 Postiz，默认 `false`。
- 参数 `POSTIZ_MAIN_URL`：Postiz 对外 HTTPS 地址，末尾禁止 `/`。
- 参数 `POSTIZ_IMAGE`：Postiz 镜像地址，生产环境应固定版本。
- String Credential `jeecg-postgres-admin-password`：共享 PostgreSQL 管理密码。
- String Credential `postiz-database-password`：Postiz 独立数据库密码。
- String Credential `postiz-jwt-secret`：Postiz JWT 签名密钥。
- String Credential `postiz-temporal-database-password`：Temporal PostgreSQL 密码。

所有密码必须使用 URL 安全字符，并通过 Jenkins Credentials 注入；禁止写入
`.env`、Compose、Jenkinsfile 或构建归档。

### 10.3 运行配置
- `POSTIZ_DB_NAME` / `POSTIZ_DB_USER`：默认均为 `postiz`。
- `POSTIZ_REDIS_DB`：默认 `1`。
- `POSTIZ_PORT`：默认宿主机端口 `4007`。
- `POSTIZ_DISABLE_REGISTRATION`：默认 `false`，完成首个账号注册后应改为 `true`。
- `TEMPORAL_UI_PORT`：启用 `tools` profile 时默认 `8088`。

## 11. 2026-08-30 推荐训练数据 ETL
- `RECOMMEND_ETL_ENABLED`：ETL 编排总开关，默认 `true`。
- `RECOMMEND_ETL_DISPATCH_MODE`：执行分发模式，支持 `local/kafka`；开发默认 local，生产和 Docker 默认 kafka。
- `RECOMMEND_ETL_PYTHON_EXECUTABLE`：Python 可执行文件，开发默认 `python`，容器默认 `python3`。
- `RECOMMEND_ETL_SCRIPT_ROOT`：允许执行的脚本根目录，任务脚本不得越出该目录。
- `RECOMMEND_ETL_OUTPUT_ROOT` / `RECOMMEND_ETL_LOG_ROOT`：数据集输出和完整运行日志根目录。
- `RECOMMEND_ETL_DEFAULT_TIMEOUT_SECONDS` / `RECOMMEND_ETL_MAX_TIMEOUT_SECONDS`：默认和最大进程超时，默认 `3600/86400` 秒。
- `RECOMMEND_ETL_MAX_LOG_CHARS`：单条执行记录保留的日志尾部字符数，默认 `1000000`；完整日志写入日志目录。
- `RECOMMEND_ETL_WORKER_CORE_SIZE` / `WORKER_MAX_SIZE` / `WORKER_QUEUE_CAPACITY`：local 模式有界线程池参数，默认 `2/4/100`。
- `RECOMMEND_ETL_KAFKA_TOPIC` / `KAFKA_GROUP`：Kafka 模式 Topic 与消费组。
- `RECOMMEND_ETL_KAFKA_PARTITIONS` / `KAFKA_REPLICAS`：Topic 分区和副本，默认 `3/1`。
- `RECOMMEND_ETL_RECOVERY_DELAY_MS`：遗留 WAITING/RUNNING 记录扫描间隔，默认 60 秒。

Docker 后台镜像安装 Python3，并挂载：
- `./etl/scripts:/opt/etl/scripts:ro`
- `./etl/output:/opt/etl/output`
- `./etl/logs:/opt/etl/logs`

ClickHouse、OSS 凭证不得写入任务配置或命令参数。Python 进程继承容器环境变量读取这些敏感配置。
