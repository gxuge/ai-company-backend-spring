# 20260831 行为事件标签快照 Hardness

## 元信息
- 任务 ID：20260831-behavior-tag-snapshot
- 任务名称：行为事件标签快照
- 分级：H2
- 负责人：Codex
- 开始时间：2026-08-31
- 目标完成时间：2026-08-31
- 关联：`ts_content_tag`、业务行为 Kafka、ClickHouse

## 目标与非目标
- 目标：角色/故事行为自动携带当前内容版本及固定标签快照。
- 目标：事件结构升级到 v3，ClickHouse 保存可直接分析的标签数组。
- 目标：真实取消收藏成功后产生 `unfavorite`。
- 非目标：不实现用户偏好聚合和推荐算法。
- 非目标：不加入 `dislike`、`story_progress`、`story_complete`。

## 输入约束
- 已知上下文：现有行为链路为 Controller/业务成功点到 Kafka，再由消费者写 ClickHouse。
- 强约束：标签只能由服务端读取，客户端不得上传标签；埋点失败不得影响主业务事务。
- 禁止事项：不得恢复旧 MySQL 行为明细表，不得修改语音标签体系。

## 任务分解
### T1 标签快照补全
- 输入：角色/故事 ID、`content_version`、`ts_content_tag`。
- 执行动作：按批次查询资源和标签，将版本、标签 ID、分数写入事件消息。
- 输出：统一标签快照补全组件。
- 验收标准：无资源或无标签时输出空快照；查询失败不阻断业务事件。
- 证据类型：JUnit、代码 diff。

### T2 事件与 ClickHouse 升级
- 输入：v2 Kafka 消息和 ClickHouse 表。
- 执行动作：增加 `content_version/tag_ids/tag_scores`，默认事件版本升级到 3。
- 输出：Java 模型、Mapper XML、初始化和升级 SQL。
- 验收标准：消费者完整映射三个字段，旧事件缺字段时仍可反序列化。
- 证据类型：消费者测试、SQL 静态检查。

### T3 取消收藏事件
- 输入：现有幂等取消收藏事务。
- 执行动作：仅 Mapper 实际更新有效收藏时，在事务提交后发布 `unfavorite`。
- 输出：可信负向事件。
- 验收标准：重复取消不产生重复事件。
- 证据类型：代码检查、定向测试。

## 验证矩阵
| 验证项 | 方法 | 阈值/期望 | 结果证据 |
|---|---|---|---|
| 标签快照 | JUnit | 当前版本标签准确 | 测试输出 |
| 消息映射 | JUnit | 字段无丢失 | 测试输出 |
| 编译回归 | Maven compile | Reactor 成功 | 构建日志 |
| 事件边界 | JUnit/代码检查 | 无新增三类延期事件 | 测试与静态扫描 |

## 上下文与防漏策略
- 上下文预算：按标签补全、消息存储、业务接入、验证四段执行。
- 分段策略：每段完成后立即运行对应定向检查。
- 压缩策略：保留目标、已完成文件、失败验证和剩余步骤。
- 恢复策略：从 `PLANS.md` 和本 Hardness 文档恢复状态。

## 风险与回退
- 风险：批量事件标签查询增加 MySQL 压力；通过资源去重和批量查询控制。
- 风险：ClickHouse 老数据卷不自动执行初始化 SQL；提供独立升级脚本并更新配置文档。
- 回退步骤：回退 Java v3 字段和 Mapper；ClickHouse 新列无需删除，可保持默认空值。

## 完成定义
- [x] 代码改动完成并自检。
- [x] 验证矩阵执行完毕。
- [x] API、配置与变更记录更新。
- [x] 未完成项明确列出。

## 未完成项
- 未连接真实 Kafka 和 ClickHouse 执行端到端冒烟。
- 已有 ClickHouse 数据卷需在部署环境手动执行升级 SQL。

## 验证证据
- `mvn -pl jeecg-module-system/jeecg-system-biz -am -DskipTests test-compile`：
  Reactor 8 模块 `BUILD SUCCESS`。
- 定向测试：5 个测试类共 18 条测试，0 失败、0 错误。
- 升级脚本：
  `docker-deploy/monolith/clickhouse/init/02_user_behavior_tag_snapshot.sql`。
