# ADR 0008：Agent Task/Tool 完整事件落库

## 状态
Accepted

## 背景
- 原事件表按 start/error/end 保存运行过程，产生大量碎片记录。
- 业务回放只需要 SubAgent Task 与 Tool 的完整输入和最终结果。

## 决策
- `ts_agent_chat_message_event` 只持久化 `subagent` 和 `tool` 两种类型。
- SubAgent 仅在 `subagent.end` 保存，每次 Task 一条记录。
- Tool 仅在 `tool.end` 保存，每次调用一条记录。
- start/error 继续发送 SSE，但只用于收集输入、错误和耗时，不直接落库。
- `agent.*`、`llm.*` 与 `llm.delta` 不落库。
- 内部 `task` Handoff Tool 不单独保存，实际任务由 SubAgent 记录表达。

## 数据结构
- 公共上下文字段继续写入独立数据库列。
- `json` 固定包含：
  - `input`
  - `output`
  - `error`
  - `metrics`
- Tool 通过 `parent_event_id` 关联所属 SubAgent Task。

## 影响
- SSE 对外事件名称和顺序保持兼容。
- 历史分段事件保留，不做数据迁移。
- 新记录无法展示 LLM 过程轨迹，但能够直接回放完整 Task/Tool 执行。

## 回滚
- 恢复 `AgentEventPublisher` 的分段持久化调用。
- `ts_agent_chat_message_event` 表结构无需回滚。
