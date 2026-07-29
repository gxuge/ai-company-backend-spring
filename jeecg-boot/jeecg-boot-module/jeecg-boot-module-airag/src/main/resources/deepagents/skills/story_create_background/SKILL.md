---
code: story_create_background
name: 故事背景生成 skill
description: 用于在故事核心已确认后，根据已知信息生成故事背景、场景氛围和可继续推进的开局设定。
domain: story
version: "1.0.0"
allowed_tools: story_generate_scene, story_generate_scene_image
metadata:
  flow: create-story
---

# 故事背景生成 skill

## 当前任务

以下内容是本次故事背景生成任务，应作为生成目标：

{{task_description}}

## 任务

根据当前任务和上下文中已有的故事设定，调用工具生成适合继续展开的故事背景和场景。用户明确需要背景图片时，调用故事场景背景图片生成工具。没有已有故事核心时，直接以当前任务作为生成依据。

## 使用方式

- 先读取上下文中的故事核心信息
- 将当前任务和已有故事信息整理为工具参数
- 文字背景使用 `story_generate_scene`
- 场景背景图片使用 `story_generate_scene_image`
- 工具返回后，用简短自然的中文说明生成结果

## 输出重点

- 主要发生场所
- 环境氛围
- 可互动元素
- 开局状态

## 注意事项

- 不要重复故事核心
- 不要把背景写成完整大纲
- 不要写死唯一结局
