# 20260714 Agent 子流程节点恢复 Hardness

## 元信息
- 任务 ID：`20260714-agent-node-resume-state`
- 任务名称：Agent 子流程节点与状态跨消息恢复
- 分级：H3
- 负责人：AI
- 时间窗口：2026-07-14
- 关联：用户当前需求、`PLANS.md`

## 目标与非目标
- 目标：会话持久化 1 个活动节点、1 个活动阶段和 1 份白名单流程状态。
- 目标：角色与故事子 Agent 在下一轮从保存阶段继续，定向测试通过率为 100%。
- 目标：子 Agent 完成后在同一顶层 Run 显式 Handoff 回主 Agent。
- 非目标：不持久化整个 `AgentContext`。
- 非目标：不改变 SSE 事件结构和消息接口路径。
- 非目标：不建设通用图工作流持久化框架。

## 输入约束
- 已知上下文：`active_agent_code` 已负责跨消息保留当前 Agent；`source_node_name` 仅用于消息审计。
- 强约束：恢复节点与来源节点必须分离；流程状态必须按 Agent 白名单保存；数据库迁移先于应用发布。
- 强约束：保持源文件编码、BOM 和换行符。
- 禁止事项：禁止把任意 Attribute 全量序列化；禁止在 Handoff 后继承上一 Agent 的恢复状态；禁止改变现有 Controller 路由。

## 任务分解
### T1 数据模型与状态支持
- 输入：`ts_agent_chat_session`、角色/故事工具写入字段。
- 执行动作：新增迁移、Entity 字段和流程状态白名单工具。
- 输出：`active_node_name`、`active_stage`、`agent_flow_state_json`。
- 验收标准：角色与故事字段互不串用；无效 JSON 不阻塞请求。
- 证据类型：SQL、单元测试、编译输出。

### T2 会话恢复与回写
- 输入：`TsAgentChatReplyServiceImpl` 的 Context 创建和 Run 收尾逻辑。
- 执行动作：运行前恢复状态；等待用户时保存；进入主 Agent 或完成时清空。
- 输出：跨请求可恢复的 `AgentContext`。
- 验收标准：直接从子 Agent 启动时保留状态；Handoff 切换 Agent 时清除旧状态。
- 证据类型：定向测试、代码 diff。

### T3 子 Agent 节点路由
- 输入：角色/故事现有阶段和节点链。
- 执行动作：按阶段路由，拆分角色图片/声音续跑入口，完成后构造完成 Handoff。
- 输出：角色支持 dialog/confirmation/image/voice，故事支持 dialog/confirmation/background。
- 验收标准：恢复到后续节点时不重复执行前置节点；完成结果交还主 Agent。
- 证据类型：Mockito 单元测试、运行步骤断言。

### T4 文档与回归
- 输入：最终代码和迁移。
- 执行动作：更新 ADR、架构、API 会话字段说明、changelog 与计划证据。
- 输出：部署和回滚路径可追溯。
- 验收标准：文档明确迁移顺序、字段语义和已知测试限制。
- 证据类型：文档 diff、编译与测试输出。

## 验证矩阵
| 验证项 | 方法 | 阈值/期望 | 结果证据 |
|---|---|---|---|
| 状态白名单 | JUnit | 角色/故事互不串用，坏 JSON 可降级 | 测试输出 |
| 节点恢复路由 | Mockito JUnit | 后续阶段不调用前置节点 | 测试输出 |
| Handoff 清理 | JUnit | 切换 Agent 后旧恢复状态为空 | 测试输出 |
| AIRAG 编译 | Maven compile | BUILD SUCCESS | 命令输出 |
| system-biz 编译 | Maven compile | BUILD SUCCESS | 命令输出 |
| 数据库迁移 | SQL 静态检查 | V29 可执行且字段可空 | SQL 文件 |

## 上下文窗口与大任务防漏策略
- 上下文预算：分析 15%，状态模型 25%，路由 30%，测试 20%，文档 10%。
- 分段策略：状态模型和子 Agent 路由分别完成后立即执行最小测试。
- 压缩策略：保留字段语义、白名单、已改文件、失败测试和下一步。
- 恢复策略：重新读取本 Hardness、`PLANS.md`、`git status --short` 和最新测试输出。

## 风险与回退
- 风险：保存的阶段与业务字段不一致。
- 触发条件：恢复节点缺少所需核心数据。
- 缓解：按当前上下文重新推导默认阶段，回退到 dialog/confirmation。
- 风险：完成 Handoff 引起重复委托。
- 触发条件：主 Agent 收到完成报告后再次调用同名 task。
- 缓解：完成报告写入 `completed=true`、结果摘要和禁止重复委托说明。
- 回退步骤：回退 V29 对应代码；新增列可保留为空，或在停机窗口删除。
- 回退验证：旧逻辑仍可仅依赖 `active_agent_code` 启动并完成编译。

## 完成定义（DoD）
- [x] 代码改动完成
- [x] 验证矩阵执行
- [x] 证据归档
- [x] 未完成项列出

## 未完成项
- AIRAG 标准 Maven `test-compile` 仍被仓库既有 `AiragPromptTemplateServiceTest.java:77` 类型错误阻塞；本任务新增和相关运行时测试已隔离编译执行。

## 证据索引
- 数据库迁移：`db/V3.9.1_29__add_agent_flow_resume_state.sql`。
- AIRAG 编译：`mvn -pl jeecg-boot-module/jeecg-boot-module-airag -am -DskipTests compile`，`BUILD SUCCESS`。
- system-biz 编译：`mvn -pl jeecg-module-system/jeecg-system-biz -am -DskipTests compile`，`BUILD SUCCESS`。
- AIRAG 定向测试：13 条成功、0 条失败。
- system-biz 流程状态测试：1 条成功、0 条失败。
- 已知阻塞：`AiragPromptTemplateServiceTest.java:77` 的既有构造参数类型错误。
