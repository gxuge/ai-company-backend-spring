# ADR 0016：使用统一提供器扩展 AI 安全 Skill

## 状态
已采纳

## 背景
Agent、TS 角色故事生成、普通伴侣聊天和图片生成使用不同模型调用入口。若每个 Service 分别读取或复制安全规则，容易出现文本不一致、优先级漂移和遗漏。

## 决策
- 在 AIRAG 模块新增 `GlobalSafetySkillPromptProvider`，统一读取 `ai_safety_guard`。
- 提供器负责去除 Skill frontmatter、失败关闭及 System/Image Prompt 拼装。
- `NodeRunner`、`MiniMaxPromptChatServiceImpl`、`MiniMaxDemoServiceImpl` 依赖该提供器。
- 文本模型使用结构化 System/User 消息，安全规则位于 System 首部。
- 图片生成在调用供应商前包装最终 Prompt，原始图片描述位于安全规则之后。

## 结果
- 安全规则只维护一份。
- 角色、故事、JSON 修复、普通聊天和图片提示词获得一致安全边界。
- 后续接入 Moderation Service 时，可继续在公共入口编排，不需要修改安全 Skill 内容。

## 非目标
- 不以 Prompt 规则替代输入和输出审核。
- 不审核供应商返回的图片像素内容。
