# ADR 0007：Active Agent Handoff 顶层运行循环

## 状态
Accepted

## 日期
2026-07-14

## 背景
- 旧链路每条用户消息都固定从主 Agent 启动。
- 主 Agent 的 `task` 工具会在工具服务内部嵌套执行子 Agent。
- 子 Agent 返回 `WAITING_USER` 后，下一条用户消息无法直接回到该子 Agent。
- 子 Agent 已具备 `handoff_to_main` 控制工具，但缺少统一的顶层切换循环。

## 决策
1. 保留 `AgentRuntimeService` 作为单个 Agent step 的执行器。
2. 新增 `AgentRunLoopService`，在同一个 `AgentContext` 和顶层 Run 内连续执行 Agent step。
3. `AgentResult` 的 `HANDOFF` 状态携带：
   - `handoffTargetAgentCode`
   - `handoffInput`
   - `handoffContext`
4. 主 Agent 的 `task` 工具只登记委托并返回 Handoff，不再创建嵌套 Run。
5. 子 Agent 通过 `handoff_to_main` 显式交还主 Agent；没有 Handoff 时，`SUCCESS`、`FAILED`、`WAITING_USER` 都保留最后实际运行的 Agent。
6. `ts_agent_chat_session.active_agent_code` 保存跨用户消息的当前控制者，下一轮以该字段作为起始 Agent。
7. `Session` 消息与子 Agent 历史继续保存“聊了什么”，`active_agent_code` 单独保存“下一轮谁处理”。
8. 单次 Run 最多执行 8 个 Agent step，超过限制后失败并保留最后实际执行的 Agent。

## 运行流程
```text
用户消息
  -> 读取 active_agent_code
  -> AgentRunLoopService
     -> AgentRuntimeService.execute(activeAgent)
     -> HANDOFF 时切换目标 Agent 并继续
     -> 非 HANDOFF 时结束
  -> 保存 lastAgentCode 到会话
  -> 保存实际回复 Agent 到消息 sender_type / agent_code
```

## 数据库
- 新增 `active_agent_code varchar(64) NOT NULL DEFAULT 'main'`。
- 新增 `active_agent_updated_at datetime NULL`。
- 历史会话自动从 `main` 启动。
- 迁移脚本：`db/V3.9.1_26__add_agent_active_handoff_state.sql`。

## 影响
- 子 Agent 追问后，下一条用户消息直接进入同一个子 Agent。
- 子 Agent 完成、取消或超出职责时，可在当前 Run 内交还主 Agent。
- 助手消息能够标记实际回复的 Agent。
- 已下线或不存在的 active Agent 会自动回退主 Agent。
- 同一会话并发请求仍采用最后完成写入策略，调用方应避免并发发送。

## 备选方案
- 继续嵌套执行子 Agent：无法自然表达跨消息控制权，也会形成父子 Run。
- 仅将 active Agent 写入 `state_json`：字段语义弱，缺少清晰约束和直接查询能力。
- 每轮始终从主 Agent 启动并让模型判断：增加模型调用，且不能保证稳定回到原子 Agent。

## 回滚
1. 回复编排恢复固定调用主 Agent。
2. `task` 工具恢复原子 Agent 执行方式。
3. 停止读写 `active_agent_code`；数据库字段可保留以兼容已部署结构。
