# ADR 0005：Agent SSE 事件传输与落库说明

## 状态
Superseded by ADR 0008

> 本文保留为历史设计记录。当前落库规则以 `0008-agent-task-tool-complete-event-storage.md` 为准。

## 背景
- Agent 执行过程中会同时产生 SSE 事件、Redis 临时缓冲和数据库事件记录。
- 当前需要统一事件语义，减少前端解析复杂度，并保证后端可回放、可排障。

## 事件范围
保留以下事件：
- `agent.start`
- `agent.end`
- `llm.start`
- `llm.delta`
- `llm.error`
- `llm.end`
- `tool.start`
- `tool.error`
- `tool.end`

## 传输规则
- `agent.start` / `agent.end`：只通过 SSE 下发，不入库。
- `llm.start`：入库 + SSE。
- `llm.delta`：只写进程内 buffer + SSE，不入库。
- `llm.error`：入库 + SSE。
- `llm.end`：入库 + SSE，内容来自进程内 buffer 合并结果。
- `tool.start`：入库 + SSE。
- `tool.error`：入库 + SSE。
- `tool.end`：入库 + SSE，且始终作为 Tool 节点最后一个事件。

## 存储规则
- Agent 业务事件自 2026-07-14 起写入 `ts_agent_chat_message_event`；旧 `ts_chat_message_events` 仅保留历史数据。
- `type` 只使用 `llm` / `tool`。
- `name` 存节点名，不存事件名。
- `content` 存当前事件的主要文本内容。
- `json` 存扩展信息，例如事件名、节点类型、节点名、promptCode、toolName、错误信息等。

## 处理链路
1. `NodeRunner` 包裹节点执行，统一发送 start / error / end。
2. `AgentEventPublisher` 负责：
   - 组织事件 payload
   - 写入数据库
   - 推送 SSE
   - 在进程内缓存 `llm.delta`
3. `llm.end` 时从进程内缓冲合并完整文本，再写入事件表。
4. 前端只消费精简后的 SSE 内容，避免直接处理大段嵌套 JSON。

## 设计目标
- 让前端看到的事件尽量简洁。
- 让后端保留完整事件轨迹，便于排查与回放。
- 保持 `llm.delta` 轻量，避免重复包装。
