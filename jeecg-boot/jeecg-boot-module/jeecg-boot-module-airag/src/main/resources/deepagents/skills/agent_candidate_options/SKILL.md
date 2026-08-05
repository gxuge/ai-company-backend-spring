---
code: agent_candidate_options
name: 候选选项
description: 在需要用户选择或继续输入时，提供简短、自然且友好的可点击候选项。
domain: common
version: "1.0.0"
allowed_tools:
  - agent_request_options
metadata:
  scope: agent-llm-node
---

# 候选选项

当需要用户选择或继续输入时，可以调用 `agent_request_options` 提供候选项。

## 规则

* 提供 2 至 4 个简短、明确、不重复的选项。
* 选项应是用户可以直接发送的内容。
* 表达自然、可爱、舒适，内容不宜过长，可以适当使用符号或表情。
* 不要在正文中重复展示候选项。
* 调用本工具后立即停止输出。
* 调用本工具时，本轮不得同时调用其他工具。
