---
code: demo-basic
name: demo-basic
description: 用于演示 skill 按需加载机制的基础示例。
domain: demo
version: "1.0.0"
allowed_tools: readSkill
metadata: {}
---

# demo-basic

## 适用场景

用于演示 Skill 的索引、路由、按需读取和上下文注入流程。

## 执行说明

在需要了解 Skill 详情时，再通过 `readSkill(skillCode)` 读取完整正文。
不要一次性加载无关 Skill。

## 输出建议

先根据当前任务选择最相关的 Skill，再决定是否继续执行。

## 注意事项

- 该示例不包含具体业务逻辑
- 该示例仅用于测试 Skill 加载机制
- 不要把它当作真实业务能力使用
