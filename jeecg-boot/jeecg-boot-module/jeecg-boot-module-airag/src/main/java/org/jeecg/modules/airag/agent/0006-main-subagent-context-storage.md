# ADR 0006：MainAgent / SubAgent 对话与上下文存储机制

## 状态
Superseded

> 控制权切换与 active Agent 持久化部分由 ADR 0007 取代；本 ADR 的消息与事件分层原则继续有效。

## 目标
参考 Deep Agents 的上下文隔离思想，将“用户可见对话”和“Agent 内部执行轨迹”分开存储，并保证 MainAgent 与 SubAgent 的协作可追踪、可恢复、可展示。

## 核心原则
1. MainAgent 不保存 SubAgent 的完整内部上下文。
2. SubAgent 的中间推理、tool call、skill 加载过程，不直接进入 MainAgent messages。
3. MainAgent 只接收 SubAgent 的最终结果或摘要。
4. SubAgent 若直接向用户追问或回复，这些消息必须进入统一会话消息流。
5. 用户可见对话和 Agent 内部执行轨迹必须分开存储。

## 一、用户可见消息表：chat_message

用于展示历史对话。

### 建议字段
- `id`
- `session_id`
- `role`：`user` / `assistant`
- `sender_type`：`user` / `main_agent` / `sub_agent`
- `agent_code`：`main` / `character` / `story` / `image` 等
- `content`
- `content_json`
- `visible_to_user`
- `seq`
- `created_at`

### 规则
- 用户消息写入 `chat_message`。
- MainAgent 回复写入 `chat_message`。
- SubAgent 如果直接向用户追问或回复，也写入 `chat_message`。
- 前端展示时按 `session_id + seq` 正序展示。
- 不要因为消息来自 SubAgent 就裁剪掉。

## 二、内部事件表：chat_message_event

用于记录执行过程，不直接展示为聊天内容。

### 建议字段
- `id`
- `message_id`
- `type`：`llm` / `tool` / `skill` / `subagent` / `rag`
- `name`
- `content`
- `status`
- `json`
- `seq`
- `created_at`

### 规则
- `llm.start` / `llm.end` 写事件表。
- `tool.start` / `tool.end` 写事件表。
- `skill.load` 写事件表。
- `subagent.start` / `subagent.end` 写事件表。
- 事件可用于日志、SSE、trace、调试。
- 事件不等于用户可见消息。

## 三、上下文恢复规则

### 展示历史
- 查询 `chat_message`。
- 条件：`session_id + visible_to_user = true`。
- 按 `seq` 正序展示。

### 执行上下文
- 不要把完整历史全部塞给模型。
- 使用：
  - `recent_messages`
  - `session_summary`
  - `state_json`
- `recent_messages` 取最近若干条用户可见消息。
- `session_summary` 保存长期摘要。
- `state_json` 保存结构化状态，例如当前角色、当前 workflow、last_active_agent。

## 四、MainAgent / SubAgent 协作规则

1. MainAgent 调用 SubAgent 时，通过 task 传递任务描述。
2. SubAgent 创建独立上下文执行。
3. SubAgent 内部 messages、tool calls、skill loads 只进入 SubAgentContext 或事件表。
4. SubAgent 完成后，只把最终结果或摘要返回 MainAgent。
5. MainAgent 基于结果继续回复用户。
6. 如果 SubAgent 需要直接问用户，这条问题作为 `chat_message` 持久化，并标记 `sender_type = sub_agent`。

## 五、一句话总结

用户能看到的内容统一进 `chat_message`；
Agent 内部执行过程进 `event / trace`；
MainAgent 只拿 SubAgent 结果，不接收完整子上下文；
后续恢复对话时，用消息流展示，用摘要和结构化状态执行。
