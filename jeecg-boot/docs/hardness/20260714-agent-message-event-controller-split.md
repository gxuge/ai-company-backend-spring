# 20260714 Agent 消息事件与 Controller 拆分 Hardness

## 元信息
- 任务 ID：`20260714-agent-message-event-controller-split`
- 任务名称：Agent 消息事件独立存储与 Controller 拆分
- 分级：H3
- 负责人：AI
- 时间窗口：2026-07-14
- 关联：用户当前需求、`PLANS.md`

## 目标与非目标
- 目标：新增 1 张 Agent 消息事件表，并让 Agent Runtime 的 Task/Tool 完整事件全部写入新表。
- 目标：新增事件分页和详情共 2 个接口，所有查询必须按当前登录用户过滤。
- 目标：将会话与消息接口拆为独立 Controller，已有接口路径保持 100% 兼容。
- 非目标：不迁移旧 `ts_chat_message_events` 历史数据。
- 非目标：不改变 SSE 事件格式、顺序和正式消息落库时序。
- 非目标：不实现前端页面。

## 输入约束
- 已知上下文：AIRAG 只依赖 system local API，system-biz 已依赖 AIRAG。
- 强约束：保持 `Controller -> Service -> Mapper`；分页上限为 100；事件查询必须校验会话用户归属。
- 强约束：保留源文件编码、BOM 与换行符。
- 禁止事项：禁止引入 AIRAG 对 system-biz 的依赖；禁止删除或覆盖旧事件历史数据；禁止改变现有消息接口 URL。

## 任务分解
### T1 数据模型与迁移
- 输入：旧事件最终表结构、`ts_agent_chat_message` 与 `ts_agent_chat_session` 结构。
- 执行动作：新增迁移脚本、Entity、Mapper 与 Mapper XML。
- 输出：`ts_agent_chat_message_event` 完整表结构和用户归属查询 SQL。
- 验收标准：表字段覆盖运行上下文与 `input/output/error/metrics` JSON；分页 SQL 包含用户归属与软删除过滤及稳定排序。
- 证据类型：SQL 文件、Mapper XML、编译输出。

### T2 AIRAG 事件链路切换
- 输入：`AgentEventPublisher` 与现有事件 Service。
- 执行动作：新增 Agent 专用事件 Service，替换 Publisher 注入与测试引用，移除旧 Java 持久化入口。
- 输出：SubAgent/Tool 完整事件仅写入新表。
- 验收标准：Publisher 定向测试覆盖 SubAgent、Tool、LLM 不落库和内部 Handoff Tool 不落库；Service 测试验证固定 JSON 字段。
- 证据类型：JUnit 输出、代码 diff。

### T3 Controller 拆分与事件接口
- 输入：现有 `TsAgentChatSessionController`。
- 执行动作：保留 Session CRUD/AI Reply，新建 Message Controller 与 Event Controller。
- 输出：原 Session/Message 路由兼容；新增事件分页与详情路由。
- 验收标准：Controller 不直接访问 Mapper；事件详情越权返回不存在或无权限；分页最大 100。
- 证据类型：编译输出、接口文档、静态检查。

### T4 文档与回归
- 输入：代码和迁移结果。
- 执行动作：更新 API、changelog、ADR 和计划证据。
- 输出：可追溯的契约与回滚说明。
- 验收标准：所有 Controller 变更均出现在 `docs/api/ts-api.md`；数据库回滚步骤明确。
- 证据类型：文档 diff、Git 状态。

## 验证矩阵
| 验证项 | 方法 | 阈值/期望 | 结果证据 |
|---|---|---|---|
| AIRAG 编译 | Maven compile | BUILD SUCCESS | 命令输出 |
| system-biz 编译 | Maven compile | BUILD SUCCESS | 命令输出 |
| 事件写入测试 | 定向 JUnit | 相关测试 100% 通过 | Surefire/控制台输出 |
| Mapper XML | Maven 资源处理与静态检查 | namespace/id 一致，SQL 可解析 | 编译输出、文件检查 |
| 接口兼容 | 路由静态核对 | 原路由无变化，新路由 2 个 | Controller/API 文档 |
| 权限归属 | SQL 静态核对 | 查询包含 `s.user_id = #{userId}` | Mapper XML |

## 上下文窗口与大任务防漏策略
- 上下文预算：分析 20%，数据层 25%，Controller 20%，测试 25%，文档 10%。
- 分段策略：数据层、Controller、测试三个阶段分别更新计划状态并执行最小验证。
- 压缩策略：保留目标、已完成文件、未完成项、测试阻塞和回滚路径。
- 恢复策略：重新读取本 Hardness、`PLANS.md`、`git status --short` 和最新测试输出后继续。

## 风险与回退
- 风险：新表未部署导致事件插入失败。
- 触发条件：日志出现 `ts_agent_chat_message_event doesn't exist`。
- 缓解：数据库迁移先于应用发布。
- 风险：事件关联消息语义被误解。
- 触发条件：按助手消息 ID 查询无结果。
- 缓解：API 文档明确 `messageId` 是触发 Run 的用户消息 ID。
- 回退步骤：恢复旧事件 Service 与 Publisher 引用；保留旧 Controller 合并实现；必要时删除新表。
- 回退验证：AIRAG/system-biz 编译成功，旧事件写入测试恢复通过。

## 完成定义（DoD）
- [x] 节点来源扩展代码完成
- [x] 节点来源扩展验证矩阵执行
- [x] 节点来源扩展证据归档
- [x] 未完成项列出

## 未完成项
- AIRAG 全量 Maven `test` 仍被既有 `AiragPromptTemplateServiceTest.java:77` 类型错误阻塞；本任务定向测试已隔离执行并通过。

## 证据索引
- AIRAG 编译：`mvn -pl jeecg-boot-module/jeecg-boot-module-airag -am -DskipTests compile`，`BUILD SUCCESS`。
- system-biz 编译：`mvn -pl jeecg-module-system/jeecg-system-biz -am -DskipTests compile`，`BUILD SUCCESS`。
- AIRAG 定向测试：`AgentEventPublisherTest`、`TsAgentChatMessageEventServiceTest`、`AgentContextNodeSourceTest`、`AgentContextPreparerTest`，11 条成功、0 条失败。
- system-biz 定向测试：`TsAgentChatMessageServiceImplTest`，1 条成功、0 条失败。
- 数据库迁移：`db/V3.9.1_27__create_ts_agent_chat_message_event.sql`。
- 节点来源迁移：`db/V3.9.1_28__add_agent_node_source_fields.sql`。
- API 契约：`docs/api/ts-api.md`。

## 节点来源扩展
### T5 节点来源传播与关联
- 输入：`NodeRunner`、`AgentContext`、SubAgent 完整事件和助手消息落库链路。
- 执行动作：记录最后一个成功且正文非空的结果节点，将 SubAgent 事件 ID 回写上下文，并写入正式助手消息。
- 输出：事件具备 `node_name/node_type`，助手消息具备 `source_node_name/source_event_id`。
- 验收标准：Tool 事件可定位调用节点；SubAgent 事件和助手消息可定位最终输出节点；助手消息可关联 SubAgent 完整事件。
- 证据类型：迁移 SQL、JUnit 输出、跨模块编译输出。

### 扩展风险与回退
- 风险：后续无正文节点覆盖真实回复节点。
- 缓解：仅在节点成功且正文非空时更新结果节点。
- 风险：主 Agent 回复不存在 SubAgent 事件。
- 缓解：主 Agent 消息允许 `source_event_id` 为空，但必须保存 `source_node_name`。
- 风险：同一顶层 Run 内 Handoff 后沿用上一 Agent 的节点来源。
- 缓解：每个 Agent step 的上下文准备阶段重置节点来源和最近 SubAgent 事件 ID。
- 回退：回退 `V3.9.1_28` 对应代码；新增字段均为可空，旧代码可继续运行。
