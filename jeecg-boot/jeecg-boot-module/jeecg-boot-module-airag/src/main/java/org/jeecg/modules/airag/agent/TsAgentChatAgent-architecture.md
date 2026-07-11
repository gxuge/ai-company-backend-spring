# TsAgentChatAgent 架构说明

## 1. 总体链路

```text
用户输入
  ↓
TsAgentChatReplyServiceImpl
  ├─ 读取会话 / 消息 / 记忆
  ├─ 组装 AgentContext
  └─ 调用 TsAgentChatAgent
        ↓
TsAgentChatAgent
  ├─ 注入 DeepAgents 风格 prompt
  ├─ 交给主 LLM 通过 task 工具委托子 Agent
  └─ 由子 Agent 执行具体任务
        ↓
Tool / Skill
  ├─ tool 负责真正执行
  └─ skill 负责执行说明与约束
        ↓
TsAgentChatReplyServiceImpl
  ├─ 保存 assistant 消息
  ├─ 记录子 Agent 历史
  └─ 返回前端
```

## 2. 核心职责

- 主 Agent：只做入口编排和任务委托，不再做旧式意图分流。
- SubAgent：只保留 `name / description / skills / tools / permissions / responseFormat` 这类流程级信息。
- Skill：描述怎么做，按需注入子 Agent 上下文。
- Tool：真正执行角色、故事、形象、声音等业务动作。

## 3. 数据传递

- 主链路只传会话记忆、最近消息、用户输入和 prompt 变量。
- 主 Agent 委托子 Agent 时，只传任务描述和子 Agent 定义。
- 子 Agent 输出最终结果后，再由服务层落库和回传。

## 4. 已删除的旧概念

- `IntentRouterNode`
- `AgentRouteDecision`
- `AgentRouteAction`
- `AgentIntentMode`
- 旧路由分发方式
- `WelcomeIntro*` 独立开场白分支

## 5. 当前方向

- 维持 DeepAgents 风格。
- 让模型通过描述和 skill 自主委托。
- 让 tool 负责落地执行。
