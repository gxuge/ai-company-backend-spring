# 20260823 Agent 全局安全 Skill Hardness

## 元信息
- 任务 ID：20260823-agent-global-safety-skill
- 任务名称：Agent LLM 节点全局安全 Skill
- 分级：H2
- 负责人：Codex
- 开始时间：2026-08-23
- 关联：`PLANS.md#20260823-agent-global-safety-skill`

## 目标与非目标
- 目标：所有 Agent `LlmNode` 强制加载统一安全 Skill。
- 目标：安全规则在最终 System Prompt 中保持最高优先级。
- 目标：安全 Skill 不可用时失败关闭。
- 非目标：不实现线上内容审核服务，不覆盖非 Agent 文本生成接口。

## 输入约束
- 已知上下文：现有固定 Skill 由 `NodeRunner` 预加载，正文通过 `AgentContext` 交给 `LlmNode`。
- 强约束：保持现有角色、故事、语言、候选项 Skill 行为不变。
- 禁止事项：不得把安全 Skill 放入按用户输入匹配的普通 Skill 路由。

## 任务分解

### T1 安全 Skill 资源
- 输入：现有 `deepagents/skills/*/SKILL.md` 格式。
- 执行动作：新增 `ai_safety_guard/SKILL.md`。
- 输出：可由现有 `SkillRegistry` 读取的安全规则。
- 验收标准：Skill 元信息和正文可正常解析。
- 证据类型：资源文件、测试结果。

### T2 强制加载
- 输入：`NodeRunner.prepareNodeSkillContext`。
- 执行动作：执行每个 LLM 节点前单独读取安全 Skill。
- 输出：独立的安全 Prompt 上下文字段。
- 验收标准：缺失或空正文时抛出异常，不执行节点。
- 证据类型：单元测试。

### T3 最高优先级注入
- 输入：`LlmNode.buildMessages`。
- 执行动作：在全部业务 Prompt 拼装完成后，将安全 Prompt 前置。
- 输出：以安全规则开头的第一条 `SystemMessage`。
- 验收标准：DeepAgents、节点 Prompt 和普通 Skill 均位于安全规则之后。
- 证据类型：单元测试。

## 验证矩阵
| 验证项 | 方法 | 期望 | 结果证据 |
|---|---|---|---|
| Skill 强制加载 | NodeRunner 定向测试 | 每个 LLM 节点加载固定 code | 通过 |
| 失败关闭 | 缺失 Skill 测试 | 节点不执行并抛出异常 | 通过 |
| Prompt 优先级 | LlmNode 定向测试 | System Prompt 以安全正文开头 | 通过 |
| 编译 | Maven compile | AIRAG 模块 BUILD SUCCESS | 通过 |
| 编码与差异 | BOM/EOL、`git diff --check` | 无转码和空白错误 | 通过 |

## 上下文与防漏策略
- 上下文预算：先完成加载机制，再完成 Prompt 顺序和测试。
- 分段策略：每个任务单元完成后执行定向检查。
- 压缩策略：保留目标、改动文件、未完成测试和风险。
- 恢复策略：重新读取本 Hardness、`NodeRunner`、`LlmNode` 和当前 diff。

## 风险与回退
- 风险：外部 Skill 目录替代 classpath 索引时缺少安全 Skill。
- 触发条件：`SkillRegistry#getSkillBody("ai_safety_guard")` 失败。
- 回退步骤：回退本任务代码和资源；恢复原有节点 Skill 加载行为。

## 完成定义
- [x] 安全 Skill 资源已新增。
- [x] 强制加载和最高优先级注入已完成。
- [x] 定向测试与编译已通过。
- [x] 文档和证据已更新。

## 未完成项
- 未执行真实模型调用冒烟。
