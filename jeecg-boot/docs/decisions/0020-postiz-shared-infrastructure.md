# ADR 0020：Postiz 复用 Jeecg PostgreSQL 与 Redis

## 状态

已采用。

## 背景

Postiz 默认 Compose 会额外启动 PostgreSQL、Redis、Temporal PostgreSQL 和
Elasticsearch。当前 Jeecg Jenkins 单体部署已经提供带密码的 Redis，
根 Compose 中的 pgvector/PostgreSQL 尚未持久化且未进入 Jenkins 部署包。

## 决策

1. 在 Jeecg 单体 Compose 中增加 `postiz` profile 下的
   `jeecg-boot-pgvector`，使用固定 Docker named volume 持久化数据。
2. PostgreSQL 实例内为 Postiz 创建独立数据库和账号，不复用 Jeecg
   业务库或向量库数据库。
3. Postiz 复用 `jeecg-boot-redis`，固定使用 Redis DB 1；Jeecg 保持 DB 0。
4. Postiz 使用独立 Compose，通过外部 `share-net` 连接共享服务。
5. Temporal 保留独立 PostgreSQL；Temporal、Postiz 不部署 Elasticsearch。
6. Jenkins 使用 `DEPLOY_POSTIZ` 参数控制独立部署阶段，凭据从 Jenkins
   Credentials 注入，默认关闭。

## 取舍

- 复用 PostgreSQL/Redis 可减少容器数量，但会共享宿主机资源和故障域。
- 独立数据库、账号和 Redis DB 可降低键名与表结构冲突，但 Redis DB
  不是安全隔离边界。
- Temporal PostgreSQL 保持独立，避免工作流历史与业务数据互相影响。
- 不修改 Postiz 上游仓库，降低后续同步官方更新时的冲突。

## 影响

- 启用 Postiz 后新增 Postiz、pgvector/PostgreSQL、Temporal 和
  Temporal PostgreSQL 容器。
- PostgreSQL 使用 `jeecg-postgres-data`，Postiz 上传和 Temporal 数据也
  使用 Docker named volume，不随 Jenkins 部署包目录重建而删除。
- Jenkins 必须配置四个 String Credentials，并提供 Postiz 对外地址。

## 回退

关闭 `DEPLOY_POSTIZ`，执行 Postiz Compose `down`，再停止
`jeecg-boot-pgvector`。回退时禁止使用 `down -v` 或删除
`jeecg-postgres-data`，除非已备份并明确放弃数据。
