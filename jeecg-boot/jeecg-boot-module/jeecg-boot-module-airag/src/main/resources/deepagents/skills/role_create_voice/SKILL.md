---
code: role_create_voice
name: 角色声音生成 skill
description: 用于在角色核心设定与形象确认后，生成适合角色的声音建议与音色表达。
domain: role
version: "1.0.0"
allowed_tools: role_generate_role_voice, role_flow_gate
metadata:
  flow: create-role-voice
---

# 角色声音生成 skill

## 任务

根据已确认的角色核心设定与角色形象，补充适合角色的声音建议。重点围绕音色、语速、语气和情绪，不要再重复核心设定追问。

## 允许工具

只允许：
- `role_generate_role_voice`
- `role_flow_gate`

## 输出要求

- 中文自然
- 简短清晰
- 便于最终回填角色资料
- 不要写成大段说明

