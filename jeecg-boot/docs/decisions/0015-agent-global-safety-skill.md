# ADR 0015：Agent LLM 节点强制注入全局安全 Skill

## 状态
已采纳

## 背景
Agent 的角色、故事、DeepAgents、节点和用户提示词会在 `LlmNode` 中组合。普通 Skill 可能按节点或用户输入选择加载，无法保证安全规则始终存在，也无法保证其位于最终 System Prompt 的最高优先级位置。

## 决策
- 新增固定 Skill：`ai_safety_guard`。
- `NodeRunner` 在每个 LLM 节点执行前强制读取该 Skill，不经过 `SkillRouter`。
- 安全 Skill 使用独立上下文字段保存，不与普通节点 Skill 混合追加。
- `LlmNode` 在全部系统业务 Prompt 拼装完成后，将安全 Skill 正文统一前置。
- 安全 Skill 缺失、读取失败或正文为空时失败关闭，不调用主 LLM。

## 结果
- 所有经过 `NodeRunner` 的 Agent LLM 节点获得一致安全规则。
- 角色、故事、上下文和用户指令不能通过 Prompt 拼装顺序覆盖安全规则。
- 外部 Skill 目录部署必须包含 `ai_safety_guard`，否则 Agent LLM 节点会明确失败。

## 非目标
- 本决策不替代输入和输出内容审核服务。
- 本决策暂不覆盖直接调用 `IPromptChatService` 或 MiniMax 的非 Agent 生成流程。
