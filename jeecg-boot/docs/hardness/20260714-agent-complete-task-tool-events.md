# 20260714 Agent Task/Tool 完整事件落库 Hardness

## 元信息
- 任务 ID：`20260714-agent-complete-task-tool-events`
- 任务名称：Agent Task/Tool 完整事件落库
- 分级：H2
- 负责人：AI
- 时间窗口：2026-07-14
- 关联：`ts_chat_message_events`

## 目标与非目标
- 目标：每次 SubAgent Task 只保存 1 条完整事件。
- 目标：每次 Tool 调用只保存 1 条完整事件。
- 目标：LLM 与 Agent 控制事件数据库写入次数为 0。
- 非目标：不修改数据库结构，不修改正式聊天消息与 SSE 对外事件名称。

## 输入约束
- 已知上下文：`AgentRuntimeService`、`NodeRunner`、`AgentEventPublisher`、`TsChatMessageEventService`。
- 强约束：保持现有 SSE 顺序；JSON 固定为 `input/output/error/metrics`。
- 禁止事项：禁止把 LLM delta、Prompt、聊天上下文或 SSE 展示载荷写入事件表。

## 任务分解
### T1 事件聚合
- 输入：现有 start/error/end 发布链路。
- 执行动作：start/error 只缓存状态，end 组装完整事件并落库。
- 输出：SubAgent/Tool 每次执行各 1 条记录。
- 验收标准：成功与失败路径均只调用一次 `saveEvent`。
- 证据类型：单元测试、代码 diff。

### T2 固定 JSON
- 输入：Task 输入/结果和 Tool 参数/结果。
- 执行动作：写入 `input/output/error/metrics`，上下文字段继续使用独立数据库列。
- 输出：可稳定解析的完整 JSON。
- 验收标准：JSON 不包含 LLM、Prompt、SSE 和重复上下文字段。
- 证据类型：单元测试捕获实体。

### T3 回归验证
- 输入：AIRAG Maven 模块。
- 执行动作：运行定向测试和模块编译。
- 输出：测试与编译结果。
- 验收标准：相关测试全部通过，模块编译成功。
- 证据类型：Maven 输出。

## 验证矩阵
| 验证项 | 方法 | 期望 | 结果证据 |
|---|---|---|---|
| SubAgent 成功 | 单元测试 | `subagent.end` 只保存 1 条完整记录 | 通过 |
| Tool 失败 | 单元测试 | `tool.end` 只保存 1 条失败记录 | 通过 |
| LLM 事件 | 单元测试 | 数据库写入次数为 0 | 通过 |
| 内部 task Tool | 单元测试 | 不重复保存 Tool 记录 | 通过 |
| 固定 JSON | 单元测试 | 仅含四个固定字段并保留显式空值 | 通过 |
| 模块回归 | Maven compile | 编译成功 | `BUILD SUCCESS` |
| 模块全量测试 | Maven test | 全部成功 | 被既有无关测试编译错误阻塞 |

## 上下文与防漏策略
- 上下文预算：事件链路、实现、测试、文档四阶段。
- 分段策略：每阶段完成后更新执行计划。
- 压缩策略：保留目标、已改文件、测试结果和未完成项。
- 恢复策略：重新读取本文件、`git diff` 和最近测试输出。

## 风险与回退
- 风险：进程在 end 前退出时不会生成完整记录。
- 触发条件：运行日志存在 start，但没有对应 end。
- 回退步骤：恢复分段事件落库实现并重新编译。

## 完成定义
- [x] 代码改动完成
- [x] 验证矩阵执行
- [x] 证据归档
- [x] 未完成项列出

## 未完成项
- AIRAG 全量测试未完成：既有 `AiragPromptTemplateServiceTest.java:77` 构造参数类型不匹配，测试编译阶段失败；该文件与本次事件落库改造无关。
