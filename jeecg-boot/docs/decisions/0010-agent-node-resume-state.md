# ADR 0010：子 Agent 节点与流程状态跨消息恢复

## 状态
Accepted

## 日期
2026-07-14

## 背景
- `active_agent_code` 只能决定下一轮由哪个 Agent 处理，不能表达子 Agent 内部应从哪个节点继续。
- 每条用户消息都会创建新的 `AgentContext`，角色和故事子 Agent 原先固定把阶段重置为 `dialog`。
- 消息字段 `source_node_name` 用于审计本轮回答来源，不适合作为下一轮路由状态。

## 决策
1. `ts_agent_chat_session` 新增 `active_node_name`、`active_stage`、`agent_flow_state_json`。
2. `active_node_name` 只表示下一轮恢复节点，`source_node_name` 继续只表示本轮消息来源。
3. `agent_flow_state_json` 不保存整个 `AgentContext`，仅保存当前角色或故事子 Agent 的业务白名单字段。
4. `WAITING_USER` 和可重试 `FAILED` 保存恢复位置；最终运行 Agent 为主 Agent或任务已结束时清空恢复状态。
5. 同一顶层 Run 发生 Handoff 时，先清除上一 Agent 的恢复状态，再合并目标 Agent 的交接上下文。
6. 角色子 Agent 支持从 `dialog/confirmation/image/voice` 继续，故事子 Agent 支持从 `dialog/confirmation/background` 继续。
7. 子 Agent 完成职责内任务后生成 `completed=true` 的交还报告，并显式 Handoff 回主 Agent统一回复。

## 数据库
- 迁移脚本：`db/V3.9.1_29__add_agent_flow_resume_state.sql`。
- 新字段均允许为空，历史会话在没有恢复状态时继续按原逻辑从当前 Agent 的默认阶段启动。
- 部署顺序必须为 V26、V27、V28、V29，再发布应用。

## 方案权衡
- 不复用 `state_json`，因为其业务语义过宽，无法明确区分当前 Agent、恢复节点和流程快照。
- 不序列化全部 Attribute，避免保存日志对象、线程对象、工具实例或其他不可恢复数据。
- 不把节点路由完全交给 LLM，后续图片、声音和背景节点应由后端阶段状态确定，减少重复生成。

## 影响
- 子 Agent 跨消息不只停留在 Agent 层，还能恢复到具体流程阶段。
- 服务器重启后只要 Session 数据保留，下一轮仍可恢复核心数据与后续节点。
- 完成任务后会在同一 Run 回到主 Agent，最终助手消息由主 Agent统一输出。

## 回滚
1. 回退 Session 状态读写和子 Agent 阶段路由代码。
2. 保留新增可空字段不影响旧版本运行；如需删除，在停机窗口执行三条 `DROP COLUMN`。
3. 回滚后系统仍依赖 `active_agent_code` 实现 Agent 级持续停留，但不再保证节点级恢复。
