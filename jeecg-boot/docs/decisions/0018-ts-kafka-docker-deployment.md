# ADR 0018：推荐行为 Kafka 纳入 Docker Compose

## 状态

已采用，默认关闭行为链路。

## 背景

后台已经具备 Spring Kafka 客户端、行为 Producer、双消费者和 Topic 创建配置，但现有两套 Docker Compose 只有 MySQL、Redis 和 PGVector，没有 Kafka Broker。若直接在容器内使用默认 `localhost:9092`，后台会连接自身容器而不是 Kafka 服务。

## 决策

1. 在根目录 `docker-compose.yml` 和 `docker-deploy/monolith/docker-compose.yml` 中统一增加服务名为 `jeecg-boot-kafka` 的 Kafka Broker。
2. 使用单节点 KRaft 模式，不额外部署 ZooKeeper。
3. Kafka 与后台、MySQL、Redis 使用各自 Compose 中已有的共享网络。
4. 后台通过 `jeecg-boot-kafka:9092` 连接 Kafka；Docker 环境通过 `.env` 或 Compose `environment` 注入，不使用容器内 `localhost`。
5. Kafka 数据持久化到 `kafka/data`，单节点开发/测试使用 `replicas=1`。
6. `TS_BEHAVIOR_KAFKA_ENABLED` 保持默认 `false`，Kafka 基础服务接入不等于开启行为埋点。

## 取舍

- 单节点 KRaft 配置简单，适合当前开发和联调；不提供生产高可用能力。
- 不默认映射 Kafka 宿主机端口，减少外部暴露面；后台通过 Docker 内部网络访问。
- 两套 Compose 同步配置会有维护成本，但可以避免不同启动入口的行为不一致。

## 影响

- 首次启动对应 Compose 时会拉取 Kafka 镜像并创建 `kafka/data` 数据目录。
- 开启行为 Kafka 前仍需执行 `V3.9.1_44__create_ts_user_behavior_event.sql`，并验证 MySQL、Redis 消费链路。
- 生产环境需要替换为多 Broker、持久化存储、认证和网络隔离配置。

## 回退

删除两套 Compose 中的 `jeecg-boot-kafka` 服务、后台依赖和 Kafka 环境变量，移除 `kafka/data` 卷挂载；保持 `TS_BEHAVIOR_KAFKA_ENABLED=false`，不影响现有 MySQL、Redis 和业务接口。
