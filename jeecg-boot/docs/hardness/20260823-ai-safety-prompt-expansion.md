# 20260823 AI 安全 Prompt 扩展 Hardness

## 元信息
- 任务 ID：20260823-ai-safety-prompt-expansion
- 任务名称：AI 安全 Skill 扩展到非 Agent 模型入口
- 分级：H2
- 负责人：Codex
- 开始时间：2026-08-23
- 关联：`PLANS.md#20260823-ai-safety-prompt-expansion`

## 目标与非目标
- 目标：统一读取和拼装 `ai_safety_guard`。
- 目标：覆盖公共 Prompt Chat、普通聊天和图片生成入口。
- 非目标：不实现输入输出审核服务，不审核生成图片本身。

## 输入约束
- 已知上下文：Agent 已通过 `NodeRunner` 强制加载安全 Skill。
- 强约束：安全规则必须位于业务 Prompt 之前，缺失时失败关闭。
- 禁止事项：不得在多个 Service 中复制安全规则正文。

## 任务分解

### T1 公共提供器
- 输入：`SkillRegistry` 和现有 Skill 正文格式。
- 执行动作：新增公共安全 Prompt 提供器并迁移 Agent 加载。
- 输出：文本 System Prompt 与图片 Prompt 的统一拼装方法。
- 验收标准：frontmatter 不进入模型上下文，缺失时抛出异常。
- 证据类型：单元测试。

### T2 文本入口
- 输入：`MiniMaxPromptChatServiceImpl`、`MiniMaxDemoServiceImpl`。
- 执行动作：普通文本和 Tool Call 使用安全 System Prompt。
- 输出：安全规则优先的结构化消息。
- 验收标准：第一条 System Message 以安全规则开头。
- 证据类型：单元测试、编译结果。

### T3 图片入口
- 输入：`MiniMaxDemoServiceImpl.image`。
- 执行动作：调用媒体供应商前包装最终图片 Prompt。
- 输出：带安全边界的供应商 Prompt。
- 验收标准：原始图片描述完整保留且位于安全规则之后。
- 证据类型：单元测试。

## 验证矩阵
| 验证项 | 方法 | 期望 | 结果证据 |
|---|---|---|---|
| Skill 解析 | 提供器单元测试 | 去除 frontmatter | 待执行 |
| 文本优先级 | Prompt Chat 测试 | 安全规则位于 System 首部 | 待执行 |
| 普通聊天 | MiniMax 聊天测试 | 使用结构化 System/User | 待执行 |
| 图片 Prompt | 图片入口测试 | 安全边界前置且保留原始描述 | 待执行 |
| 编译 | Maven compile | 相关模块通过 | 待执行 |
| 编码与差异 | BOM/EOL、`git diff --check` | 无转码和空白错误 | 待执行 |

## 上下文与防漏策略
- 上下文预算：公共提供器、文本入口、图片入口、验证四段执行。
- 分段策略：每个入口改动后立即补测试。
- 压缩策略：保留目标入口、改动文件、测试状态和未完成项。
- 恢复策略：重新读取本 Hardness、ADR 0015/0016 和当前 diff。

## 风险与回退
- 风险：外部 Skill 根目录未包含安全 Skill。
- 触发条件：公共提供器读取 `ai_safety_guard` 失败。
- 回退步骤：回退本任务代码，保留第一阶段 Agent 安全 Skill。

## 完成定义
- [ ] 公共提供器已完成。
- [ ] 三类目标入口已接入。
- [ ] 定向测试和编译已通过。
- [ ] 文档证据已更新。

## 未完成项
- 待实现与验证。
