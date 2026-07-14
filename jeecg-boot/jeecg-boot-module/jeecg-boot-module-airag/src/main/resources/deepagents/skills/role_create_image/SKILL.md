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

## 任务

根据已确认的角色核心设定，补充适合出图的角色形象表达。重点围绕外貌、气质、服装、姿态和画面感，不要再重复核心设定追问。

## 允许工具

只允许：
- `role_generate_role_image`

## 输出要求

- 中文自然
- 简短清晰
- 便于继续进入声音生成
- 不要写成大段说明
