# 20260823 推荐行为埋点第一阶段 Hardness

## 元信息
- 任务 ID：20260823-ts-behavior-tracking-v1
- 任务名称：推荐行为埋点第一阶段
- 分级：H3
- 负责人：Codex
- 时间窗口：2026-08-23
- 关联：`PLANS.md#20260823-ts-behavior-tracking-v1`

## 目标与非目标
- 目标：登录用户行为通过 Kafka 异步采集，单批最多 100 条。
- 目标：MySQL 明细和 Redis 特征使用独立消费者组。
- 目标：消息失败具有有限重试与死信路径。
- 非目标：不实现匿名埋点、ClickHouse 和推荐排序。

## 输入约束
- 已知上下文：Spring Boot 3.5.5、MyBatis-Plus、MySQL、Redis。
- 强约束：Controller -> Service -> Kafka；用户 ID 只信任登录态；配置使用环境变量。
- 禁止事项：请求线程同步写行为明细；提交真实 Kafka 地址或凭证；覆盖其他未提交改动。

## 任务分解
### T1 数据模型
- 输入：统一事件字段。
- 执行动作：新增迁移、Entity、Mapper 和 DTO。
- 输出：`ts_user_behavior_event` 及幂等写入能力。
- 验收标准：`event_id` 唯一，常用训练查询字段有索引。
- 证据类型：SQL/XML 检查与测试。

### T2 采集与投递
- 输入：登录态和 Kafka 配置。
- 执行动作：实现单条/批量接口和异步 Producer。
- 输出：事件按用户 ID 投递统一 Topic。
- 验收标准：批量上限、时间窗口和 JSON 大小校验生效。
- 证据类型：单元测试与编译。

### T3 消费与特征
- 输入：统一 Kafka 事件。
- 执行动作：两个消费者组分别写 MySQL 和 Redis。
- 输出：离线训练明细与实时用户特征。
- 验收标准：明细重复事件不重复入库，Redis 去重键和 TTL 生效。
- 证据类型：单元测试与代码检查。

### T4 文档与验证
- 输入：最终接口和配置。
- 执行动作：更新 API、配置、ADR、changelog，执行验证。
- 输出：可联调文档和证据。
- 验收标准：影响模块编译通过，XML 可解析，无编码和空白错误。
- 证据类型：Maven、XML、`git diff --check`。

## 验证矩阵
| 验证项 | 方法 | 阈值/期望 | 结果证据 |
|---|---|---|---|
| 参数边界 | 定向单元测试 | 非法批量、时间、JSON 被拒绝 | 测试输出 |
| Producer | Mock KafkaTemplate | Topic、分区键和消息正确 | 测试输出 |
| 明细幂等 | Mapper 设计检查 | `event_id` 唯一及 `INSERT IGNORE` | SQL/XML |
| Redis 特征 | Mock StringRedisTemplate | 去重、累计和 TTL 正确 | 测试输出 |
| 回归 | Maven compile | BUILD SUCCESS | 命令输出 |

## 上下文与防漏策略
- 上下文预算：模型 20%，生产与消费 45%，文档测试 35%。
- 分段策略：计划、生产链路、消费链路、验证四阶段推进。
- 压缩策略：保留接口契约、配置键、表结构、失败策略和未完成项。
- 恢复策略：从 `PLANS.md`、本文件和 `git diff` 恢复。

## 风险与回退
- 风险：Broker 故障造成投递失败；记录异步失败日志并由 Kafka 监控告警。
- 风险：消费者重平衡造成重复消费；MySQL 唯一键、Redis 去重键处理。
- 风险：明细表增长；后续归档或迁移 ClickHouse。
- 回退：关闭功能开关、回退代码和配置、删除新增表。
- 回退验证：应用在开关关闭时正常启动，既有接口编译与测试通过。

## 完成定义
- [x] 代码改动完成
- [x] 验证矩阵执行
- [x] 证据归档
- [x] 未完成项列出

## 未完成项
- 未执行真实 Kafka、MySQL、Redis 联调和接口冒烟。

## 验证证据
- 完整依赖链 Java 编译：`BUILD SUCCESS`。
- system-biz 测试源码编译：`BUILD SUCCESS`。
- 定向测试：6 条成功、0 失败。
- Mapper XML：解析成功。
- 配置：Kafka 与行为环境变量键已完成静态检查。
- 编码：原有 BOM 与换行符保持，新文件使用 UTF-8 无 BOM。
