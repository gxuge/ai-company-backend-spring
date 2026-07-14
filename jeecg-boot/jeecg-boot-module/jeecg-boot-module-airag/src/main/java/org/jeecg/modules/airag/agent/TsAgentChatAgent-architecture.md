# TsAgentChatAgent 架构说明

## 1. 总体链路

```text
用户输入
  ↓
TsAgentChatReplyServiceImpl
  ├─ 读取会话 / 消息 / 记忆 / active Agent 与流程恢复状态
  ├─ 组装 AgentContext
  └─ 调用 AgentRunLoopService
        ├─ AgentRegistry 解析当前 Agent
        ├─ AgentContextPreparer 注入当前 Agent 配置
        └─ AgentRuntimeService 执行一个 Agent step
              ↓ HANDOFF
           在同一 Run 内切换 active Agent 并继续
        ↓
Tool / Skill
  ├─ tool 负责真正执行
  └─ skill 负责执行说明与约束
        ↓
TsAgentChatReplyServiceImpl
  ├─ 保存 lastAgentCode、恢复节点、阶段和白名单流程状态
  ├─ 按实际回复 Agent 保存 assistant 消息
  ├─ 记录子 Agent 历史
  └─ 返回前端
```

## 2. 核心职责

- `AgentRunLoopService`：维护一次顶层 Run 的 active Agent，消费 Handoff 并限制最大 step 数。
- `AgentRegistry`：统一查找主 Agent 和子 Agent，历史 active Agent 失效时回退主 Agent。
- `AgentContextPreparer`：在同一个上下文中切换主/子 Agent 配置、历史、Skill 与 prompt 变量。
- 主 Agent：做入口编排和任务委托，`task` 只返回切换到目标子 Agent 的 Handoff。
- SubAgent：只保留 `name / description / skills / tools / permissions / responseFormat` 这类流程级信息。
- `handoff_to_main`：子 Agent 完成、取消或超出职责时，显式把控制权交还主 Agent。
- Skill：描述怎么做，按需注入子 Agent 上下文。
- Tool：真正执行角色、故事、形象、声音等业务动作。

## 3. 数据传递

- `AgentContext` 在同一个顶层 Run 内复用，Handoff 只更新用户输入和交接上下文。
- 主 Agent 委托子 Agent 时，传递任务描述；上下文准备器按目标 Agent 注入定义、Skill、Tool 权限和专属历史。
- 子 Agent 交还主 Agent 时，交接报告写入 `handoffReport`，供主 Agent 重新判断和派活。
- 子 Agent 内部历史按 Agent 编码隔离，用户可见消息继续统一进入消息表。

## 4. 跨消息续接

- `ts_agent_chat_session.active_agent_code` 相当于应用层保存的 `last_agent`。
- `active_node_name` 保存下一轮恢复节点，`active_stage` 保存角色/故事流程阶段。
- `agent_flow_state_json` 仅保存当前子 Agent 声明的核心、确认和产物字段，不序列化整个上下文。
- 每次 Run 结束后保存最后实际运行的 Agent。
- 下一条用户消息直接从该 Agent 和保存阶段启动，不再固定先经过主 Agent或子 Agent 的 `dialog`。
- 只有显式 Handoff 才切换控制者；`WAITING_USER` 会继续停留在当前子 Agent。
- 角色流程可恢复到对话/确认/图片/声音，故事流程可恢复到对话/确认/背景。
- 子 Agent 完成后携带 `completed=true` 的报告显式 Handoff 回主 Agent，并清空恢复状态。
- active Agent 不存在或已下线时，自动回退 `main`。

## 5. 控制规则

- 单次 Run 最多执行 8 个 Agent step，防止循环 Handoff。
- `AgentRunOutcome.lastAgentCode` 始终表示最后真正执行过的 Agent。
- `Session` 保存消息与历史，`active_agent_code` 保存下一轮控制者，两者职责分离。
- `source_node_name` 是消息审计来源，`active_node_name` 是下一轮路由入口，两者职责分离。
- 同一会话并发请求采用最后完成写入策略，客户端应避免同时提交多条消息。
