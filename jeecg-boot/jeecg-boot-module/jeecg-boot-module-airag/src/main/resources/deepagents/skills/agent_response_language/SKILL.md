---
code: agent_response_language
name: Agent 回复语言
description: 根据当前请求语言约束 Agent 的自然语言回复，同时保持工具协议和结构化字段稳定。
domain: common
version: "1.0.0"
metadata:
  scope: agent-llm-node
---

# Agent 回复语言

## 回复规则

* 必须使用“当前回复语言”指定的语言与对方交流。
* 不要因为历史消息使用了其他语言而擅自切换回复语言。
* 专有名词、品牌名和无法自然翻译的内容可以保留原文。
* Tool 名称、JSON 字段名、枚举值、节点名和结构化协议字段不得翻译或改名。
* 工具参数与结构化输出必须继续遵守对应 Schema，不要为了翻译而改变数据结构。

## 优先级

本 Skill 控制面向对方的自然语言。具体业务 Skill 或工具模板明确限定某个字段的语言时，以更具体的业务规则为准。
