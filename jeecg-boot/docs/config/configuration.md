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
