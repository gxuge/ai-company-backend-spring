# 20260829 Postiz 共享基础设施 Hardness

## 元信息
- 任务 ID：20260829-postiz-shared-infrastructure
- 任务名称：Postiz 复用 Jeecg PostgreSQL/Redis 的独立部署
- 分级：H2
- 负责人：Codex
- 时间窗口：2026-08-29
- 关联：`PLANS.md#20260829-postiz-shared-infrastructure`

## 目标与非目标
- 目标：为 Postiz 提供独立数据库、共享 Redis DB 1 和可选 Jenkins 部署。
- 目标：Temporal 使用独立 PostgreSQL 可见性存储，不部署 Elasticsearch。
- 目标：默认 Jeecg Jenkins 发布行为保持不变。
- 非目标：不修改 Jeecg 业务代码、业务数据库和 Postiz 上游源码。

## 输入约束
- 已知上下文：Jenkins 使用 `docker-deploy/monolith/docker-compose.yml` 和 `share-net`。
- 强约束：凭据只从 Jenkins Credentials 注入；Postiz 与 Jeecg 数据必须隔离。
- 禁止事项：不得提交真实密码；不得复用 Jeecg 业务库；不得删除现有 Docker 数据。

## 任务分解
### T1 共享 PostgreSQL
- 输入：现有 pgvector 镜像和 `share-net`。
- 执行动作：增加 `postiz` profile、固定 named volume、健康检查和幂等建库脚本。
- 输出：`jeecg-boot-pgvector` 可为 Postiz 提供独立数据库。
- 验收标准：默认 profile 不启动该服务；显式 profile 可解析全部配置。
- 证据类型：Compose config、脚本静态检查。

### T2 Postiz 精简 Compose
- 输入：Postiz 官方 Compose、共享 PostgreSQL/Redis。
- 执行动作：移除内置 PostgreSQL、Redis、Elasticsearch，保留 Postiz、Temporal 和 Temporal PostgreSQL。
- 输出：`docker-deploy/postiz/docker-compose.yml`。
- 验收标准：Postiz 连接共享服务；Temporal 只使用 PostgreSQL。
- 证据类型：Compose config、环境变量检查。

### T3 Jenkins 与打包
- 输入：现有 bundle 脚本和 Jenkinsfile。
- 执行动作：打包 PostgreSQL/Postiz 资产，新增凭据绑定和可选部署阶段。
- 输出：部署包包含 `postgres` 与 `postiz` 目录。
- 验收标准：`DEPLOY_POSTIZ=false` 保持既有流程；启用时先建库再启动 Postiz。
- 证据类型：脚本语法、Jenkinsfile 静态检查。

### T4 文档与验证
- 输入：最终配置与脚本。
- 执行动作：更新 ADR、配置说明、changelog，执行差异和编码检查。
- 输出：可回溯的部署说明与验证证据。
- 验收标准：文档完整，`git diff --check` 无新增错误。
- 证据类型：命令输出、文件路径。

## 验证矩阵
| 验证项 | 方法 | 阈值/期望 | 结果证据 |
|---|---|---|---|
| 默认兼容 | 主 Compose config | 不要求 Postiz 凭据 | 命令输出 |
| 共享数据库 | profile Compose config | 服务、卷、健康检查完整 | 命令输出 |
| Postiz 依赖 | Postiz Compose config | 无 Elasticsearch/内置 Redis/业务 PostgreSQL | 命令输出 |
| 脚本 | Bash/PowerShell 静态检查 | 无语法错误 | 命令输出 |
| 编码 | BOM/EOL 检查 | 保持原文件编码与换行 | 命令输出 |

## 上下文与防漏策略
- 上下文预算：部署设计 30%，实现 45%，验证与文档 25%。
- 分段策略：共享服务、Postiz Compose、Jenkins、验证四段执行。
- 压缩策略：保留服务名、网络名、凭据 ID、数据库名和回退路径。
- 恢复策略：从 `PLANS.md`、本文件、ADR 和 `git diff` 恢复。

## 风险与回退
- 风险：共享 Redis 资源竞争；监控内存、延迟和淘汰数。
- 风险：初始化脚本只在首次建卷自动执行；Jenkins 每次部署额外幂等执行。
- 风险：数据库密码含 URL 保留字符；Jenkins 凭据必须使用 URL 安全字符。
- 回退：关闭 `DEPLOY_POSTIZ`，停止 Postiz Compose 和 PostgreSQL profile，保留数据。

## 完成定义
- [x] 配置与脚本改动完成
- [x] 验证矩阵执行
- [x] 证据归档
- [x] 未完成项列出

## 未完成项
- 按用户要求未实际启动 Docker、拉取镜像或执行生产 Jenkins 发布。
- 首次生产发布需确认 pgvector 镜像内 PostgreSQL 版本与 Postiz 当前迁移兼容。

## 验证记录
- Compose：Jeecg 默认服务、`postiz` profile、Postiz 默认服务及 `tools` profile 均完成静态解析。
- 依赖：Postiz 配置只保留应用、Temporal、Temporal PostgreSQL；共享 Redis 使用 DB 1。
- 脚本：两个 Bash 脚本语法通过；PowerShell bundle 脚本 AST 解析无错误。
- 差异与编码：`git diff --check` 通过，修改文件保持原 BOM 和换行约定。
