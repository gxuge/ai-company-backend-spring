# ADR 0009：Agent 消息事件独立存储边界

## 状态
Accepted

## 背景
- 通用聊天事件表 `ts_chat_message_events` 与 Agent 正式消息表 `ts_agent_chat_message` 分属不同业务域。
- Agent Runtime 已将持久化事件收敛为 SubAgent Task 与 Tool 的完整执行记录。
- system-biz 需要提供带用户归属过滤的 Agent 事件查询接口，但 AIRAG 不能反向依赖 system-biz。

## 决策
- 新增独立表 `ts_agent_chat_message_event`。
- Agent 事件 Entity、Mapper、Mapper XML 与 Service 放在 AIRAG 模块。
- system-biz 通过已有的 AIRAG 依赖调用事件 Service，并提供独立 Event Controller。
- `message_id` 保存触发当前 Run 的用户消息 ID；同一 Run 内的 Task/Tool 事件均关联该消息。
- 事件分页和详情通过关联 `ts_agent_chat_session` 强制过滤当前登录用户。
- 旧 `ts_chat_message_events` 及历史数据保留，不执行自动复制。
- 事件表增加 `node_name/node_type`，其中 `name` 继续表示 SubAgent 或 Tool，`node_name` 表示实际执行节点。
- 正式助手消息增加 `source_node_name/source_event_id`，用于定位最终产出节点并关联 SubAgent 完整事件。
- 最后一个成功且正文非空的节点作为最终结果节点，避免后续空结果节点覆盖真实来源。
- Agent 最终状态为 `FAILED` 或状态缺失时，事件与正式消息优先记录当前节点，避免误关联到前一个成功节点。
- 每次 Handoff 后准备新活动 Agent 时重置节点来源与最近 SubAgent 事件 ID，节点归属不跨 Agent step 继承。

## 方案权衡
- 若把事件 Service 放在 system-biz，AIRAG 写事件会产生反向依赖或循环依赖，因此不采用。
- 若在助手消息落库后再回填事件 `message_id`，会增加临时关联、批量更新和异常补偿复杂度，因此保留触发消息语义。
- 若迁移旧事件数据，可能产生重复记录并增加锁表时间；当前需求只要求新链路切换，因此不迁移。

## 影响
- 新部署必须先执行数据库迁移，再启动应用。
- 原 Agent Session/Message API 路径保持不变，只调整 Controller 类归属。
- 新增事件分页和详情接口，返回新事件实体字段。
- SSE 事件、正式消息落库和旧表均不受影响。

## 回滚
- 将 `AgentEventPublisher` 恢复注入旧 `TsChatMessageEventService`。
- 恢复合并版 `TsAgentChatSessionController`。
- 新表无业务外键，可保留；若确认无数据保留要求，可执行 `DROP TABLE ts_agent_chat_message_event`。
