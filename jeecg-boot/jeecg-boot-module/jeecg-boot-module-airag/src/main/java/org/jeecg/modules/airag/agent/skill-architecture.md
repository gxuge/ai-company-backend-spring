# Skill 架构说明

这套 Skill 架构的目标，是把“能力发现、能力编排、能力执行”分开，做到可扩展、可校验、可复用。

## 一、整体分层

1. Skill 层
   - 负责描述能力本身。
   - 每个 Skill 用 `SKILL.md` 定义，包含名称、描述、适用域、输入、输出、允许工具等元信息。
   - Skill 本体不是工具，也不是 Agent，只是一份可加载的能力说明。

2. Router 层
   - 根据用户输入和领域，先从 Skill 索引里挑候选 Skill。
   - 这一层只做“找谁合适”，不做业务执行。

3. Runtime / Executor 层
   - 负责把候选 Skill 和上下文接到具体运行链路里。
   - `skill` 节点走 LLM。
   - `tool` 节点走统一 ToolRegistry。
   - `clarify` 节点只返回一句追问，等待用户补充。

## 二、核心职责

### SkillRegistry
- 扫描并加载 `SKILL.md`。
- 提供 Skill 索引和完整正文读取能力。

### SkillRouter
- 输入用户话术，输出候选 Skill 列表。
- 只做路由，不做执行。

### NodeRunner
- 统一包装节点执行过程。
- 处理 `llm.start / llm.delta / llm.end` 和 `tool.start / tool.error / tool.end`。

### AgentEventPublisher
- 统一负责 SSE 推送、事件落库、进程内文本缓冲。
- `llm.delta` 只推送和缓存，不落库。

## 三、执行链路

用户输入
→ SkillRouter
→ NodeRunner
→ SSE / 事件表 / Redis

## 四、节点类型

当前第一版只保留三种节点：

- `skill`
  - 对应一个可加载的 Skill
  - 用于文本理解、生成、总结、补全

- `tool`
  - 对应一个后端工具
  - 统一封装内部方法、HTTP、MCP、数据库等调用

- `clarify`
  - 用于追问缺失信息
  - 只问一句最关键的问题

## 五、当前落地点

这套实现主要落在：

- `org.jeecg.modules.airag.agent.skill`
- `org.jeecg.modules.airag.agent.runtime`
- `org.jeecg.modules.airag.agent.node`

## 六、设计原则

- Skill 只描述能力，不负责执行。
- Planner 只负责规划，不直接相信输出。
- Validator 负责拦截错误和缺参。
- Executor 负责顺序执行和结果汇总。
- SSE 事件由统一事件发布器处理。
- 后续扩展新的 Skill 或 Tool 时，不需要推翻现有结构。
