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

## 7. 2026-08-23 推荐行为 Kafka 配置
- `KAFKA_BOOTSTRAP_SERVERS`：Broker 地址，默认 `localhost:9092`；Docker Compose 中使用 `jeecg-boot-kafka:9092`。
- `TS_BEHAVIOR_KAFKA_ENABLED`：行为生产和消费开关，默认 `false`。
- `TS_BEHAVIOR_KAFKA_TOPIC`：主 Topic，默认 `ts.user-behavior.v1`。
- `TS_BEHAVIOR_KAFKA_DLQ_TOPIC`：死信 Topic，默认 `ts.user-behavior.dlq.v1`。
- `TS_BEHAVIOR_KAFKA_DETAIL_GROUP`：MySQL 明细消费者组。
- `TS_BEHAVIOR_KAFKA_FEATURE_GROUP`：Redis 特征消费者组。
- `TS_BEHAVIOR_KAFKA_PARTITIONS` / `TS_BEHAVIOR_KAFKA_REPLICAS`：Topic 分区与副本数，默认 `6/1`。
- `TS_BEHAVIOR_MAX_BATCH_SIZE`：单批最大事件数，默认 `100`。
- `TS_BEHAVIOR_MAX_PROPERTIES_BYTES`：单条扩展 JSON 最大字节数，默认 `8192`。
- `TS_BEHAVIOR_FEATURE_TTL_DAYS` / `TS_BEHAVIOR_DEDUP_TTL_DAYS`：特征与消费去重 TTL，默认 `30/7` 天。

Producer 开启幂等、`acks=all` 和 LZ4 压缩；消费者失败重试两次后进入死信 Topic。
正式启用前必须先执行 `V3.9.1_44__create_ts_user_behavior_event.sql`。

### 7.1 Docker 部署
- 两套 Compose 均提供 `jeecg-boot-kafka` 单节点 KRaft Broker，并与后台服务加入同一 Docker 网络。
- Kafka 数据持久化到 Compose 目录下的 `kafka/data`，不默认映射宿主机端口，仅供 Docker 网络内的后台服务访问。
- Docker 环境默认仍为 `TS_BEHAVIOR_KAFKA_ENABLED=false`；开启前需先确认 Broker、Topic、MySQL 明细消费者和 Redis 特征消费者链路。
- 当前 `TS_BEHAVIOR_KAFKA_PARTITIONS=6`、`TS_BEHAVIOR_KAFKA_REPLICAS=1` 仅适用于单节点开发/测试部署，生产集群需要按 Broker 数量调整。

## 8. 2026-08-23 AI 文本审核模型
- Agent 审核默认使用当前 Agent 应用绑定的 AIRAG 文本模型。
- 公共 Prompt Chat 审核使用当前 Prompt Chat 已解析出的 AIRAG 文本模型。
- MiniMax 普通聊天与图片 Prompt 审核复用：
  - `jeecg.airag.prompt-chat.model-id`
  - 或 `jeecg.airag.prompt-chat.app-id` 对应应用绑定的文本模型。
- 两项均未配置或模型不可用时，审核按失败关闭处理：输入不进入主模型，输出不直接返回用户。
- 审核日志不记录完整原文，只记录阶段、类别、分数、动作、服务、时间、内容长度和 SHA-256 摘要前缀。
