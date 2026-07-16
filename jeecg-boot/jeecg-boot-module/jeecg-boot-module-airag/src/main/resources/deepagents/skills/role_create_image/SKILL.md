---
code: role_create_image
name: 角色形象生成 skill
description: 用于在角色核心设定确认后，生成可用于出图的角色形象描述与视觉提示。
domain: role
version: "1.0.0"
allowed_tools: role_generate_role_image
metadata:
  flow: create-role-image
---

# 角色形象生成 skill

## 当前任务

以下内容是本次角色形象生成任务，应作为生成目标：

{{task_description}}

## 任务

根据当前任务和上下文中已有的角色设定，调用工具生成角色形象。重点围绕外貌、气质、服装、姿态和画面感；没有已有角色核心时，直接以当前任务作为生成依据。

## 允许工具

只允许：
- `role_generate_role_image`

## 输出要求

- 中文自然
- 简短清晰
- 清楚说明角色形象生成结果
- 不要写成大段说明
