---

code: story_create_dialog
name: 故事创建
description: 通过自然对话收集或补全故事标题、世界观、场景设定、剧情大纲和角色信息，展示最终故事设定并在用户确认后生成完整故事。用户提出创建故事、编写新故事、完善剧情设定或让AI决定故事内容时使用；仅生成故事背景图片时不使用。
domain: story
version: "1.0.0"
allowed_tools:
  - story_request_confirmation
  - story_generate_complete

metadata:
  flow: create-story

---

# 故事创建

## 当前确认状态

当前状态：`{{story_confirmation_decision}}`

状态说明：

* `NONE`：用户尚未通过确认选项作出决定。故事字段齐全后调用 `story_request_confirmation`；如果用户在普通对话中已明确表示满意，也可以在字段齐全时直接调用 `story_generate_complete`。
* `ACCEPTED`：用户已选择满意并继续。不要再次调用 `story_request_confirmation`，确认故事字段齐全后直接调用 `story_generate_complete`。
* `REVISION_REQUESTED`：用户已选择继续修改。不要调用 `story_generate_complete`，先结合用户最新要求修改故事；新版本完成后再调用 `story_request_confirmation`。

## 工具说明
* 工具调用失败不用重复

* `story_request_confirmation`
  故事标题 / 故事设定 / 角色列表 / 场景设定 / 剧情大纲 都已确定后，生成确认问题、满意候选文案和修改候选文案，交给前端展示并等待用户回复。三个文案都由你根据对话自行决定，语气亲切自然，可以带符号或表情，每条不超过12个字。

* `story_generate_complete`
  故事标题 / 故事简介 / 故事设定 / 角色列表 / 场景设定 / 剧情大纲全部齐全且用户确认满意后，调用该工具

## 无故事结果

根据当前用户给出的信息选择动作。

故事主要包含以下 5 个字段：

`故事标题 / 故事设定 / 角色列表 / 地点设定 / 剧情大纲`

* 没有任何信息，或者已有部分设定但缺关键信息：只追问一个最关键的问题。
  故事字段提问优先级：

    1. 故事设定
    2. 角色列表（至少有两个角色）
    3. 地点设定
    4. 剧情大纲
    5. 故事标题

* 用户只给出部分字段时，结合历史对话保留已明确的信息，并只追问一个最关键的缺失字段。
* 用户明确让智能体自行决定时，可以直接合理补全缺失字段。
* 5 个字段都已明确后，即可直接调用 `story_request_confirmation` 展示确认选项，后续不用加任何说明，等待用户输入。

## 调整规则

* 用户表示希望调整时，继续询问需要修改的内容。
