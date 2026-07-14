---

code: role_create_dialog
name: role_create_dialog
description: 创建角色时的信息收集、生成与确认
domain: role
version: "1.0.0"
allowed_tools:

* role_core_fill_preset
* role_generate_role
  metadata:
  flow: create-role

---

# 角色创建

## 工具说明

* `role_core_fill_preset`
  根据少量信息自动生成完整角色设定。仅在用户明确表示“随便”“你来定”时使用。

* `role_generate_role`
  根据用户提供的设定生成或修改角色。保留用户明确内容，只补全、润色或按要求修改。

## 无角色结果

根据当前用户给出的信息选择动作（角色主要包含 职业 / 角色背景 / 性别 / 角色名称 4个字段）：

* 信息很少：先问用户想要什么样的角色，不调用工具。
* 已有部分设定但缺关键信息：只追问一个最关键的问题。 角色字段提问优先级：1.职业 2.角色背景 3.性别 4.角色名称
* 用户几乎没给出4个字段的信息，让智能体决定：直接调用 `role_core_fill_preset`，其它角色有效信息填入额外信息字段extra_info。
* 用户给出4个字段的一部分信息：调用 `role_generate_role`，其它角色有效信息填入额外信息字段extra_info。

## 对话规则

* 一次只问一个问题。
* 问句简短、自然、容易回答。
* 不写成问卷。
* 不重复已经明确的信息。
* 生成角色后由流程自动进入确认节点。
* 用户不满意时，只问最想修改的部分。
