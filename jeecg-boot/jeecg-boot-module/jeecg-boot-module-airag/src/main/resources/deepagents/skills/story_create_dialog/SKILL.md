---

code: story_create_dialog
name: story_create_dialog
description: 创建故事时的信息收集、生成与确认
domain: story
version: "1.0.0"
allowed_tools:

* story_full_generate_preset
* story_full_generate
* story_confirmation_decision

metadata:
flow: create-story
------------------

# 故事创建

## 工具说明

* `story_full_generate_preset`
  根据少量信息自动生成完整故事设定。用户几乎没有给出核心字段信息，或让智能体自行决定时使用。

* `story_full_generate`
  根据用户提供的信息生成或修改故事。保留用户明确设定，只补全、润色或按要求修改。

* `story_confirmation_decision`
  给出满意和不满意两个选项，征求用户对已有故事结果的意见。已有故事结果时必须调用。

## 无故事结果

根据当前用户给出的信息选择动作。

故事主要包含以下 5 个字段：

`故事标题 / 故事简介 / 故事设定 / 地点设定 / 剧情大纲`

* 信息很少：先问用户想要一个什么样的故事，不调用工具。

* 已有部分设定但缺关键信息：只追问一个最关键的问题。
  故事字段提问优先级：

    1. 故事设定
    2. 剧情大纲
    3. 故事简介
    4. 地点设定
    5. 故事标题

* 用户几乎没有给出 5 个字段的信息，并让智能体决定：直接调用 `story_full_generate_preset`，其他有效信息填入额外信息字段 `extra_info`。

* 用户给出了 5 个字段中的部分信息：调用 `story_full_generate`，其他有效信息填入额外信息字段 `extra_info`。

## 已有故事结果

必须简单列举以下 5 个字段及其对应内容：

* 故事标题
* 故事简介
* 故事设定
* 地点设定
* 剧情大纲

列举完成后，调用 `story_confirmation_decision`，让用户确认是否满意。

## 对话规则

* 一次只问一个问题。
* 问句简短、自然、容易回答。
* 不写成问卷。
* 不重复已经明确的信息。
* 生成故事后必须进入确认。
* 用户不满意时，只问最想修改的部分。
