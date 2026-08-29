# Postiz 部署说明

Postiz 使用独立 Compose，通过外部 Docker 网络 `share-net` 复用：

- `jeecg-boot-pgvector:5432` 中的独立 `postiz` 数据库；
- `jeecg-boot-redis:6379` 的 Redis DB 1。

Temporal 使用独立 PostgreSQL，不启用 Elasticsearch。Temporal UI 放在
`tools` profile 中，默认不启动。

## Jenkins 前置配置

新增以下 String Credentials：

- `jeecg-postgres-admin-password`
- `postiz-database-password`
- `postiz-jwt-secret`
- `postiz-temporal-database-password`

密码必须使用 URL 安全字符，避免破坏 PostgreSQL/Redis URL。

Jenkins 参数：

- `DEPLOY_POSTIZ`：是否部署 Postiz，默认关闭；
- `POSTIZ_MAIN_URL`：完整 HTTPS 地址，末尾不能带 `/`；
- `POSTIZ_IMAGE`：Postiz 镜像，生产环境建议固定版本。

## 数据边界

- Jeecg 主业务数据库保持 MySQL，不受影响；
- pgvector 默认数据库保持 `vector_db`；
- Postiz 使用独立数据库 `postiz` 和独立账号；
- Jeecg Redis 使用 DB 0，Postiz 使用 DB 1。
- 共享 PostgreSQL 使用固定 named volume `jeecg-postgres-data`，不会随 Jenkins
  部署包目录重建而删除。

## 回退

先停止 `docker-compose.yml` 中的 Postiz 服务，再停止单体 Compose 的
`jeecg-boot-pgvector` profile。禁止使用 `down -v`，避免删除 Postiz 和
Temporal 数据。
