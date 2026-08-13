# TS 业务 API（`/sys`）

## 1. 范围
- 模块：`jeecg-module-system/jeecg-system-biz`
- 控制器范围：`Ts*Controller.java`
- 完整访问前缀（默认）：`/jeecg-boot/sys`
- 说明：大多数 TS 接口挂在 `/sys`，`TsMcpServerController` 例外，挂在 `/ts/mcp`
- 文档定位：记录 TS 业务域接口入口、资源边界、常用路由与当前生成型接口

## 2. 控制器总览

| 控制器 | 基础路径 | 主要能力 |
|---|---|---|
| `TsAiLogController` | `/sys/tsAiLog` | AI 调用监控、分页/详情查询 |
| `TsAgentChatSessionController` | `/sys` | Agent 会话 CRUD + AI 回复 |
| `TsAgentChatMessageController` | `/sys` | Agent 会话消息分页/详情 |
| `TsAgentChatMessageEventController` | `/sys` | Agent 消息 Task/Tool 事件分页/详情 |
| `TsBrowsePublicController` | `/sys` | 公开故事/角色/形象浏览 |
| `TsChatMessageAttachmentController` | `/sys/ts-chat-message-attachments` | 聊天附件 CRUD |
| `TsChatMessageController` | `/sys/ts-chat-messages` | 聊天消息 CRUD |
| `TsChatSessionController` | `/sys/ts-chat-sessions` | 聊天会话 CRUD + AI 回复/语音/候选建议 |
| `TsFeedbackController` | `/sys` | 反馈发布、分页、详情、点赞、评论、回复与追加 |
| `TsFeedbackAdminController` | `/sys` | 反馈状态管理与官方回复 |
| `TsMcpServerController` | `/ts/mcp` | TS MCP SSE / HTTP 入口 |
| `TsPresetController` | `/sys/tsPreset` | 生成预设主表 CRUD |
| `TsPresetTagController` | `/sys/tsPresetTag` | 预设-标签关联 CRUD |
| `TsPublicChannelController` | `/sys` | 公开渠道 CRUD + 下拉选项 |
| `TsPublicManageOptionsController` | `/sys/ts-public-manage` | 公开管理用户下拉 |
| `TsRoleController` | `/sys` | 角色 CRUD + 一键生成/模板生成 |
| `TsRoleImageGenerateRecordController` | `/sys/ts-role-image-generate-records` | 角色生图记录 CRUD |
| `TsRoleImageProfileController` | `/sys/ts-role-image-profiles` | 角色形象档案 CRUD |
| `TsRolePublicController` | `/sys` | 角色公开作者视图 |
| `TsRolePublicManageController` | `/sys` | 角色公开记录 CRUD + 审核流 |
| `TsRoleTagController` | `/sys` | 官方角色标签查询 |
| `TsStoryChapterController` | `/sys/ts-story-chapters` | 故事章节 CRUD |
| `TsStoryController` | `/sys` | 故事 CRUD + 分段/全量生成 |
| `TsStoryPublicManageController` | `/sys` | 故事公开记录 CRUD + 审核流 |
| `TsTagController` | `/sys/tsTag` | 生成素材标签主表 CRUD |
| `TsTagRelationController` | `/sys/tsTagRelation` | 标签关系规则 CRUD |
| `TsTagTypeController` | `/sys/tsTagType` | 生成标签类型字典 CRUD |
| `TsUserImageAssetController` | `/sys/ts-user-image-assets` | 用户图片资产 CRUD |
| `TsUserVoiceConfigController` | `/sys/ts-user-voice-config/current` | 当前用户音色配置读写 |
| `TsVoiceProfileController` | `/sys/ts-voice-profiles` | 音色档案、标签、试听、当前用户库 |
| `TsVoiceTagController` | `/sys/ts-voice-tags` | 音色标签 CRUD |

## 3. 核心接口

### 3.1 聊天链路
聊天会话、消息、附件均遵循标准 CRUD 路由，当前高频自定义接口如下：

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/ts-chat-sessions` | 会话分页查询 |
| GET | `/ts-chat-sessions/detail` | 会话详情 |
| POST | `/ts-chat-sessions` | 新增会话 |
| PUT | `/ts-chat-sessions` | 编辑会话 |
| DELETE | `/ts-chat-sessions` | 删除会话 |
| GET | `/ts-chat-messages` | 消息分页查询 |
| GET | `/ts-chat-messages/detail` | 消息详情 |
| POST | `/ts-chat-messages` | 新增消息 |
| PUT | `/ts-chat-messages` | 编辑消息 |
| DELETE | `/ts-chat-messages` | 删除消息 |
| GET | `/ts-chat-message-attachments` | 附件分页查询 |
| GET | `/ts-chat-message-attachments/detail` | 附件详情 |
| POST | `/ts-chat-message-attachments` | 新增附件 |
| PUT | `/ts-chat-message-attachments` | 编辑附件 |
| DELETE | `/ts-chat-message-attachments` | 删除附件 |
| POST | `/ts-chat-sessions/ai-reply` | 会话内生成 AI 文本回复并产出语音 |
| POST | `/ts-chat-sessions/ai-reply-template` | 基于角色卡/故事卡生成模板回复 |
| POST | `/ts-chat-sessions/message-tts` | 按指定 AI 消息即时生成语音，返回前端本地缓存键 |
| POST | `/ts-chat-sessions/reply-suggestions` | 生成 3 条候选回复建议 |

补充说明：
- `ai-reply` 会在后端直接完成文本生成与语音生成编排。
- `message-tts` 只负责按消息重新获取语音，不做服务端语音缓存落库。
- `audioCacheKey` 仅作为 Web 本地缓存键使用。

### 3.2 Agent 会话链路

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/ts-agent-chat-sessions` | Agent 会话分页查询 |
| GET | `/ts-agent-chat-sessions/detail` | Agent 会话详情 |
| POST | `/ts-agent-chat-sessions` | 新增 Agent 会话 |
| PUT | `/ts-agent-chat-sessions` | 编辑 Agent 会话 |
| DELETE | `/ts-agent-chat-sessions` | 删除 Agent 会话 |
| POST | `/ts-agent-chat-sessions/ai-reply` | Agent 会话内生成回复，支持 SSE |
| GET | `/ts-agent-chat-messages` | Agent 消息分页查询 |
| GET | `/ts-agent-chat-messages/detail` | Agent 消息详情 |
| GET | `/ts-agent-chat-message-events` | Agent 消息事件分页查询 |
| GET | `/ts-agent-chat-message-events/detail` | Agent 消息事件详情 |

Agent SSE 确认交互说明：
- 确认交互通过 `tool.end` 返回，核心字段为 `contentType=options`、`interactionId`、`question` 和 `options`。
- `interactionType`、`interactionStatus`、`suspendRun`、`contextRef`、`transferData` 等运行控制字段不向前端透传；角色或故事结构化结果继续通过后端上下文传给后续节点。
- 角色流程中，`role_request_confirmation` 只生成动态问题和两个动态候选文案，不携带角色数据，也不触发节点跳转。
- 前端点击候选项后，将候选文案作为普通 `userInput` 发送；`interactionId/optionValue` 暂时保留兼容，但后端不再依据选项 Key 决定下一节点。
- 只有模型在确认四项角色字段齐全且用户明确同意继续后调用 `role_continue_generation`，后端才保存 `roleName/gender/occupation/backgroundStory` 并进入角色形象、声音节点。

事件查询参数：
- `sessionId`：可选，Agent 会话 ID。
- `messageId`：可选，触发当前 Run 的用户消息 ID，不是 Run 结束后生成的助手消息 ID。
- `type`：可选，当前为 `subagent` 或 `tool`。
- `name`：可选，SubAgent 编码或 Tool 名称。
- `nodeName`：可选，实际执行节点名称。
- `status`：可选，`1` 成功、`0` 失败、`2` 运行中或未知。
- `pageNo/pageSize`：默认 `1/20`，`pageSize` 最大为 `100`。

补充说明：
- Agent 会话响应新增 `activeNodeName/activeStage/agentFlowStateJson`：分别表示下一轮恢复节点、当前子 Agent 阶段和白名单业务状态快照。
- `activeAgentCode` 决定下一轮由哪个 Agent 处理；`activeNodeName` 决定该子 Agent 从哪个节点继续；两者均不等同于消息审计字段 `sourceNodeName`。
- `WAITING_USER` 或子 Agent 可重试失败时保存恢复状态；任务显式 Handoff 回主 Agent 后清空恢复节点、阶段和流程快照。
- 事件表为 `ts_agent_chat_message_event`，只保存完整的 SubAgent Task 与非内部 Tool 调用。
- 每条事件的 `json` 固定包含 `input/output/error/metrics`。
- 事件响应包含 `nodeName/nodeType`；`name` 仍表示 SubAgent 编码或 Tool 名称，`nodeName` 表示实际执行节点。
- Agent 消息响应包含 `sourceNodeName/sourceEventId`；子 Agent 助手消息通过 `sourceEventId` 关联对应的完整 SubAgent 事件。
- 分页与详情均通过 Agent 会话归属过滤当前登录用户。
- Session、Message、Event 分属三个 Controller，原 Session/Message 路由保持不变。

### 3.3 角色与故事核心
角色与故事主表也遵循标准 CRUD。生成型接口如下：

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/ts-roles` | 角色分页查询 |
| GET | `/ts-roles/detail` | 角色详情 |
| POST | `/ts-roles` | 新增角色 |
| PUT | `/ts-roles` | 编辑角色 |
| DELETE | `/ts-roles` | 删除角色 |
| POST | `/ts-roles/one-click-setting` | 角色一键设定生成 |
| POST | `/ts-roles/one-click-setting-preset` | 角色一键设定生成预设版 |
| POST | `/ts-roles/one-click-image` | 角色一键生图 |
| POST | `/ts-roles/optimize-image-prompt` | 角色形象提示词优化（读取数据库模板 `role_image_prompt_optimize::v1`，返回 `visualPrompt` / `negativePrompt`） |
| POST | `/ts-roles/generate-image-prompt-by-template` | 角色形象提示词生成（读取数据库模板 `role_create_image_prompt::v1`，返回 `styleUsed` / `visualPrompt` / `negativePrompt`） |
| POST | `/ts-roles/generate-image-by-prompt` | 角色形象生图（传入 `promptText`，可选 `styleName` / `referenceImageUrl`，直接调用 image-01 模型） |
| POST | `/ts-roles/one-click-voice` | 角色一键生音色 |
| POST | `/ts-roles/generate-text-by-template` | 角色模板文本生成 |
| POST | `/ts-roles/generate-role` | 随机完整角色生成 |
| GET | `/ts-roles/author-public` | 获取角色作者公开信息 |
| GET | `/ts-role-tags` | 官方角色标签查询 |
| GET | `/ts-role-image-profiles` | 角色形象档案分页查询 |
| GET | `/ts-role-image-profiles/detail` | 角色形象档案详情 |
| POST | `/ts-role-image-profiles` | 新增角色形象档案 |
| PUT | `/ts-role-image-profiles` | 编辑角色形象档案 |
| DELETE | `/ts-role-image-profiles` | 删除角色形象档案 |
| GET | `/ts-role-image-generate-records` | 角色生图记录分页查询 |
| GET | `/ts-role-image-generate-records/detail` | 角色生图记录详情 |
| POST | `/ts-role-image-generate-records` | 新增角色生图记录 |
| PUT | `/ts-role-image-generate-records` | 编辑角色生图记录 |
| DELETE | `/ts-role-image-generate-records` | 删除角色生图记录 |
| GET | `/ts-stories` | 故事分页查询 |
| GET | `/ts-stories/detail` | 故事详情 |
| POST | `/ts-stories` | 新增故事 |
| PUT | `/ts-stories` | 编辑故事 |
| DELETE | `/ts-stories` | 删除故事 |
| POST | `/ts-stories/story-full-generate` | 故事全量生成 |
| POST | `/ts-stories/story-setting-generate` | 故事设定生成 |
| POST | `/ts-stories/story--scene-generate` | 故事场景生成 |
| POST | `/ts-stories/one-click-scene-image` | 故事场景背景图片生成，仅返回原始图片地址，不保存或关联故事 |
| POST | `/ts-stories/story--outline-generate` | 故事大纲生成 |
| POST | `/ts-stories/story-full-generate-preset` | 故事全量生成预设版 |
| GET | `/ts-story-chapters` | 章节分页查询 |
| GET | `/ts-story-chapters/detail` | 章节详情 |
| POST | `/ts-story-chapters` | 新增章节 |
| PUT | `/ts-story-chapters` | 编辑章节 |
| DELETE | `/ts-story-chapters` | 删除章节 |

### 3.4 统一草稿

角色与故事草稿使用同一资源，通过 `draftType` 区分类型。所有接口仅访问当前登录用户的数据。

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/ts-drafts` | 分页查询草稿及完整 `content`，可按类型、名称和来源 ID 筛选 |
| GET | `/ts-drafts/detail` | 查询单条草稿并返回完整 `content` |
| POST | `/ts-drafts` | 新增角色或故事草稿 |
| PUT | `/ts-drafts` | 编辑角色或故事草稿 |
| DELETE | `/ts-drafts` | 软删除草稿 |

查询参数：
- `pageNo/pageSize`：默认 `1/10`，`pageSize` 最大为 `100`。
- `draftType`：可选，只允许 `role` 或 `story`。
- `keyword`：可选，按草稿名称模糊查询。
- `sourceId`：可选，筛选来自指定正式角色或故事的草稿。

新增或编辑参数：
- `id`：仅编辑时必填。
- `draftType`：必填，只允许 `role` 或 `story`。
- `draftName`：必填，最大 200 个字符。
- `sourceId`：可选，记录来源正式资源 ID，不会修改该资源。
- `content`：必填 JSON 对象，保存页面完整状态快照。

响应说明：
- 列表、详情、新增和编辑均返回完整结构化 `content`；前端直接从该字段组装草稿卡片并恢复页面。
- 当前草稿箱按较小分页读取完整 JSON，不额外维护重复的 `cardData`。
- 删除设置 `status=0`；已删除草稿不会出现在列表或详情中。

### 3.5 用户收藏

角色与故事共用同一收藏资源，通过 `resourceType` 区分类型。所有接口仅访问当前登录用户的数据，且收藏列表只返回仍在线公开的资源。

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/ts-user-favorites` | 分页查询当前用户收藏，支持全部、角色、故事和关键字筛选 |
| GET | `/ts-user-favorites/status` | 查询当前用户是否已收藏指定资源 |
| POST | `/ts-user-favorites` | 收藏在线公开的角色或故事 |
| DELETE | `/ts-user-favorites` | 取消指定角色或故事收藏 |

分页查询参数：
- `pageNo/pageSize`：默认 `1/10`，`pageSize` 最大为 `100`。
- `resourceType`：可选，只允许 `role` 或 `story`。
- `keyword`：可选，按角色名称或故事标题模糊查询。

收藏、取消和状态查询参数：
- `resourceType`：必填，只允许 `role` 或 `story`。
- `resourceId`：必填，角色或故事主表 ID。

收藏与取消操作均为幂等操作。资源不存在、已删除或没有在线公开记录时禁止新增收藏。

### 3.6 用户浏览记录

角色与故事共用同一浏览记录资源，通过 `resourceType` 区分类型。重复浏览不会新增记录，而是累加浏览次数并更新最近浏览时间。

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/ts-user-browse-history` | 分页查询当前用户浏览记录，支持全部、角色、故事和关键字筛选 |
| POST | `/ts-user-browse-history` | 记录一次在线公开角色或故事浏览行为 |
| DELETE | `/ts-user-browse-history` | 删除指定角色或故事浏览记录 |
| DELETE | `/ts-user-browse-history/clear` | 清空当前用户全部浏览记录 |

分页查询参数：
- `pageNo/pageSize`：默认 `1/10`，`pageSize` 最大为 `100`。
- `resourceType`：可选，只允许 `role` 或 `story`。
- `keyword`：可选，按角色名称或故事标题模糊查询。

记录和单条删除参数：
- `resourceType`：必填，只允许 `role` 或 `story`。
- `resourceId`：必填，角色或故事主表 ID。

列表按 `lastViewedAt` 倒序，只返回仍在线公开的资源。删除和清空均为软删除；删除后的资源再次被浏览时，从一次浏览重新记录。

### 3.7 公开浏览与公开管理
公开浏览接口默认用于前台访问；公开管理接口用于上架、审核、下架流程。

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/ts-stories/public` | 公开故事列表 |
| GET | `/ts-stories/public/detail` | 公开故事详情 |
| GET | `/ts-roles/public` | 公开角色列表 |
| GET | `/ts-roles/public/detail` | 公开角色详情 |
| GET | `/ts-role-image-profiles/public` | 公开角色形象列表 |
| GET | `/ts-public-channels` | 公开渠道分页查询 |
| GET | `/ts-public-channels/detail` | 公开渠道详情 |
| POST | `/ts-public-channels` | 新增公开渠道 |
| PUT | `/ts-public-channels` | 编辑公开渠道 |
| DELETE | `/ts-public-channels` | 删除公开渠道 |
| GET | `/ts-public-channels/options` | 公开渠道下拉选项 |
| GET | `/ts-public-manage/user-options` | 公开管理用户下拉 |
| GET | `/ts-role-publics` | 角色公开记录分页查询 |
| GET | `/ts-role-publics/detail` | 角色公开记录详情 |
| POST | `/ts-role-publics` | 新增角色公开记录 |
| PUT | `/ts-role-publics` | 编辑角色公开记录 |
| DELETE | `/ts-role-publics` | 删除角色公开记录 |
| POST | `/ts-role-publics/submit` | 提交角色公开记录 |
| POST | `/ts-role-publics/approve` | 审核通过角色公开记录 |
| POST | `/ts-role-publics/reject` | 驳回角色公开记录 |
| POST | `/ts-role-publics/online` | 上架角色公开记录 |
| POST | `/ts-role-publics/offline` | 下架角色公开记录 |
| GET | `/ts-role-publics/role-options` | 角色公开目标下拉 |
| GET | `/ts-story-publics` | 故事公开记录分页查询 |
| GET | `/ts-story-publics/detail` | 故事公开记录详情 |
| POST | `/ts-story-publics` | 新增故事公开记录 |
| PUT | `/ts-story-publics` | 编辑故事公开记录 |
| DELETE | `/ts-story-publics` | 删除故事公开记录 |
| POST | `/ts-story-publics/submit` | 提交故事公开记录 |
| POST | `/ts-story-publics/approve` | 审核通过故事公开记录 |
| POST | `/ts-story-publics/reject` | 驳回故事公开记录 |
| POST | `/ts-story-publics/online` | 上架故事公开记录 |
| POST | `/ts-story-publics/offline` | 下架故事公开记录 |
| GET | `/ts-story-publics/story-options` | 故事公开目标下拉 |

### 3.6 预设与标签资源
以下资源型控制器均遵循统一的标准 CRUD 形态：`list / add / edit / queryById / delete / deleteBatch`。

| 控制器 | 基础路径 | 说明 |
|---|---|---|
| `TsPresetController` | `/sys/tsPreset` | 生成预设主表 |
| `TsPresetTagController` | `/sys/tsPresetTag` | 预设-标签关联 |
| `TsTagController` | `/sys/tsTag` | 生成素材标签主表 |
| `TsTagTypeController` | `/sys/tsTagType` | 标签类型字典 |
| `TsTagRelationController` | `/sys/tsTagRelation` | 标签关系规则 |

### 3.7 音色与资产
| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/ts-voice-profiles` | 音色档案分页查询 |
| GET | `/ts-user-voice-profiles` | 当前用户音色库分页查询 |
| PUT | `/ts-user-voice-profiles/{id}` | 重命名当前用户音色 |
| DELETE | `/ts-user-voice-profiles/{id}` | 从当前用户音色库移除音色 |
| DELETE | `/ts-voice-profiles` | 删除公共音色档案 |
| GET | `/ts-voice-profiles/tags` | 查询音色标签关系 |
| PUT | `/ts-voice-profiles/tags` | 保存音色标签关系 |
| POST | `/ts-voice-profiles/preview` | 按选定音色试听 |
| GET | `/ts-user-voice-config/current` | 查询当前用户音色配置 |
| PUT | `/ts-user-voice-config/current` | 保存当前用户音色配置 |
| GET | `/ts-user-image-assets` | 用户图片资产分页查询 |
| GET | `/ts-user-image-assets/detail` | 用户图片资产详情 |
| POST | `/ts-user-image-assets` | 新增用户图片资产 |
| PUT | `/ts-user-image-assets` | 编辑用户图片资产 |
| DELETE | `/ts-user-image-assets` | 删除用户图片资产 |
| POST | `/ts-images/download` | 代理下载公网图片；仅返回附件流，不入库或关联业务资源 |
| GET | `/ts-voice-tags` | 音色标签分页查询 |
| POST | `/ts-voice-tags` | 新增音色标签 |
| DELETE | `/ts-voice-tags` | 删除音色标签 |

### 3.8 AI 日志与 MCP
| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/tsAiLog/list` | AI 调用日志分页查询 |
| GET | `/tsAiLog/queryById` | AI 调用日志按 id 查询 |
| GET | `/tsAiLog/detail` | AI 调用日志详情 |
| GET | `/ts/mcp/sse` | TS MCP SSE 连接端点 |
| POST | `/ts/mcp/sse` | TS MCP Streamable HTTP 端点 |
| POST | `/ts/mcp/message` | TS MCP 消息处理 |
| GET | `/ts/mcp/info` | TS MCP 说明与工具信息 |

### 3.9 会员与支付后台
以下接口仅允许管理员访问，查询参数统一通过 JSON Body 传递。

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/sys/ts-member-admin/payment/page` | 按订单号、用户、支付渠道和支付状态分页查询支付流水 |
| POST | `/sys/ts-member-admin/payment/detail` | 查询支付流水、关联会员订单及脱敏后的渠道响应 |

## 4. AI Prompt 与 ToolCall 接口

以下接口属于 Prompt 模板驱动或 ToolCall 风格的生成型接口，通常由后端根据 AI 应用 metadata 的 `code+version` 定位模板，并在失败时进入 JSON Repair 修复链路。

### 4.1 角色生成
- `POST /ts-roles/one-click-setting`
- `POST /ts-roles/one-click-setting-preset`
- `POST /ts-roles/one-click-image`
- `POST /ts-roles/one-click-voice`
- `POST /ts-roles/generate-text-by-template`
- `POST /ts-roles/generate-role`

`POST /sys/ts-roles/one-click-setting` 支持可选请求字段 `extraInfo`，并兼容 `extra_info`。该字段映射到 `role_core_fill` 的 `{{extra_info}}`；未传、`null` 或空白字符串时按 `null` 处理。preset 接口因复用 DTO 可以接收该字段，但不会传入 `role_core_fill_preset`。

### 4.2 故事生成
- `POST /ts-stories/story-full-generate`
- `POST /ts-stories/story-setting-generate`
- `POST /ts-stories/story--scene-generate`
- `POST /ts-stories/one-click-scene-image`
- `POST /ts-stories/story--outline-generate`
- `POST /ts-stories/story-full-generate-preset`

`POST /sys/ts-stories/story-full-generate` 支持可选请求字段 `extraInfo`，并兼容 `extra_info`。该字段映射到 `story_core_fill` 的 `{{extra_info}}`；未传、`null` 或空白字符串时按 `null` 处理。preset 接口因复用 DTO 可以接收该字段，但不会传入 `story_core_fill_preset`。

`POST /sys/ts-stories/one-click-scene-image` 接收 `title/storySetting/sceneSetting/plotOutline/styleName/aspectRatio/referenceImageUrl`。其中 `storySetting` 与 `sceneSetting` 不能同时为空；默认风格为“写实影视级场景概念图”，默认比例为 `9:16`。接口只返回供应商原始 `imageUrl`、`promptCode` 和 `promptVersion`，不导入用户素材，也不更新故事 `sceneImageUrl`。

### 4.3 聊天生成
- `POST /ts-chat-sessions/ai-reply`
- `POST /ts-chat-sessions/ai-reply-template`
- `POST /ts-chat-sessions/reply-suggestions`
- `POST /ts-agent-chat-sessions/ai-reply`

### 4.4 当前语音链路说明
- `POST /ts-chat-sessions/ai-reply` 会在后端直接产出语音元信息。
- `POST /ts-chat-sessions/message-tts` 只负责按消息即时生成语音，不依赖服务端缓存表。
- `audioCacheKey` 仅作为 Web 本地缓存键使用。

### 4.5 模板与修复约束
- 模板来源通过 AI 应用 metadata 的 `code + version` 定位。
- ToolCall required 字段校验失败时，必须进入 JSON Repair 修复链路。
- 当前修复链路应与工具 schema 保持一致，避免输出字段漂移。

### 4.6 反馈中心

用户端接口均要求登录。`GET` 接口的资源 ID 使用查询参数传递，`POST/PUT` 接口的资源 ID 使用 JSON Body 传递：

- `POST /sys/ts-feedback`：发布反馈，支持 `image/screenshot/log` 附件引用。
- `GET /sys/ts-feedback`：反馈分页，支持 `type/status/sort/keyword/pageNo/pageSize`。
- `GET /sys/ts-feedback/detail?feedbackId=1`：反馈详情、追加内容与附件。
- `GET /sys/ts-my-feedback`：当前用户反馈分页。
- `POST /sys/ts-feedback/like`：幂等点赞反馈，Body：`{"feedbackId":1}`。
- `POST /sys/ts-feedback/append`：反馈发起人追加内容，Body：`{"feedbackId":1,"content":"补充内容"}`。
- `GET /sys/ts-feedback/comments?feedbackId=1`：一级评论分页，每条默认附带前 2 条回复。
- `POST /sys/ts-feedback/comments`：发布一级评论，Body：`{"feedbackId":1,"content":"评论内容"}`。
- `GET /sys/ts-comments/replies?commentId=1`：指定一级评论的二级回复分页。
- `POST /sys/ts-comments/reply`：回复评论，Body：`{"commentId":1,"content":"回复内容"}`；回复二级评论时仍归入其一级评论。
- `POST /sys/ts-comments/like`：幂等点赞评论，Body：`{"commentId":1}`。

管理端接口：

- `PUT /sys/ts-admin-feedback/status`：更新状态，Body：`{"feedbackId":1,"status":"processing"}`，权限 `feedback:admin:status`。
- `POST /sys/ts-admin-feedback/reply`：发布官方回复，Body：`{"feedbackId":1,"content":"官方回复"}`，权限 `feedback:admin:reply`。

反馈类型为 `feature/bug/experience`，状态为 `received/processing/completed`，排序支持 `latest/hot`。反馈、评论、回复、官方回复和状态变化均维护冗余计数或预留通知事件。

## 5. 权限约定
- 当前 `Ts*Controller` 代码未统一显式标注 `@RequiresPermissions`。
- 聊天、角色、故事等大部分接口使用 `@RequiresAuthentication` 或系统统一鉴权链路。
- `TsBrowsePublicController`、`TsRolePublicController`、`TsMcpServerController` 中部分接口属于公开或特定鉴权策略接口，需按代码实际注解判断。

## 6. 配置依赖（摘要）
- 路由实现位于：`jeecg-module-system/jeecg-system-biz/src/main/java/org/jeecg/modules/system/controller/Ts*Controller.java`
- 数据层实现位于：`jeecg-module-system/jeecg-system-biz/src/main/java/org/jeecg/modules/system/mapper` 与 `entity`
- 当前 TS 业务接口无独立配置文件，复用 `jeecg-system-biz` 与全局 Spring Boot 配置。

## 7. 维护说明
- 每次新增/变更 `Ts*Controller` 映射时，同步更新本文件与 `docs/changelog.md`。
