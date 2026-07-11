---
code: story_create_background
name: 故事背景生成 skill
description: 用于在故事核心已确认后，根据已知信息生成故事背景、场景氛围和可继续推进的开局设定。
domain: story
version: "1.0.0"
allowed_tools: story_generate_scene
metadata:
  flow: create-story
---

# 故事背景生成 skill

## 任务

在故事核心已经确定后，基于已确认信息生成更适合继续展开的背景和场景。

## 使用方式

- 先读取上下文中的故事核心信息
- 如果背景信息已足够，直接整理成清晰的场景设定
- 如果还缺少关键背景信息，只补最重要的一句，不要一次问很多

## 输出重点

- 主要发生场所
- 环境氛围
- 可互动元素
- 开局状态

## 注意事项

- 不要重复故事核心
- 不要把背景写成完整大纲
- 不要写死唯一结局
