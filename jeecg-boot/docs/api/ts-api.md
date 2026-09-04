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
| `TsPublicChannelController` | `/sys` | 公开渠道 CRUD + 下拉选项 |
| `TsPublicManageOptionsController` | `/sys/ts-public-manage` | 公开管理用户下拉 |
| `TsRoleController` | `/sys` | 角色 CRUD + 一键生成/模板生成 |
| `TsRoleImageGenerateRecordController` | `/sys/ts-role-image-generate-records` | 角色生图记录 CRUD |
| `TsRoleImageProfileController` | `/sys/ts-role-image-profiles` | 角色形象档案 CRUD |
| `TsRolePublicController` | `/sys` | 角色公开作者视图 |
| `TsRolePublicManageController` | `/sys` | 角色公开记录 CRUD + 审核流 |
| `TsStoryChapterController` | `/sys/ts-story-chapters` | 故事章节 CRUD |
| `TsStoryController` | `/sys` | 故事 CRUD + 分段/全量生成 |
| `TsStoryPublicManageController` | `/sys` | 故事公开记录 CRUD + 审核流 |
| `TsTagController` | `/sys/tsTag` | 固定内容标签 CRUD + 打标任务重试 |
| `TsTagTypeController` | `/sys/tsTagType` | 固定内容标签类型 CRUD |
| `TsUserImageAssetController` | `/sys/ts-user-image-assets` | 用户图片资产 CRUD |
| `TsUserVoiceConfigController` | `/sys/ts-user-voice-config/current` | 当前用户音色配置读写 |
| `TsVoiceProfileController` | `/sys/ts-voice-profiles` | 音色档案、标签、试听、当前用户库 |
| `TsVoiceTagController` | `/sys/ts-voice-tags` | 音色标签 CRUD |

## 3. 核心接口

### 3.0 内容标签
- `ts_tag_type` 固定区分角色的性格、互动风格、外在气质，以及故事的题材、情绪基调、节奏、内容体验。
- 角色和故事生成响应可选返回 `tags[{typeCode,name,score}]` 与 `tagModelVersion`。
- 角色和故事保存请求可回传上述字段；后端仅保存固定词典命中、`score >= 0.5` 且每类排名前 3 的标签。
- 未携带合法候选标签时，作品审核快照提交后创建独立异步打标任务。
- `POST /sys/tsTag/tasks/retry?taskId={id}`：重新执行失败且未超过 3 次的打标任务。

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
| POST | `/ts-chat-sessions/message-tts` | 按角色消息音色快照生成语音，支持 JSON 或 MP3 流 |
| POST | `/ts-chat-sessions/reply-suggestions` | 生成 3 条候选回复建议 |

补充说明：
- `ai-reply` 会在后端直接完成文本生成与语音生成编排。
- `message-tts` 默认 `stream=false`，继续返回统一 JSON；其中 `audioUrl` 当前为临时 `data:audio/mpeg;base64,...` 播放地址，不写入 R2。
- `message-tts` 传 `stream=true` 时直接返回 `Content-Type: audio/mpeg` 的分块响应，不使用 `Result` 包装。
- 模板角色回复会把活动角色写入消息 `senderId`，并在 `contentJson.voiceSnapshot` 固化 `roleId/voiceProfileId/voiceId/speed/pitch/volume`。
- `message-tts` 只读取消息音色快照，不查询角色或用户默认音色；旧消息缺少快照时直接返回业务错误。
- `message-tts` 不做服务端语音缓存落库；临时音频也不会写入消息 `contentJson` 或附件表。
- `audioCacheKey` 仅作为 Web 本地缓存键使用。
- `/ts-chat-sessions` 列表响应同时返回 `roleName`、`roleAvatarUrl` 和 `lastMessageText`，用于直接渲染会话列表摘要，前端无需逐条请求角色详情或消息分页。

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
| POST | `/ts-stories/optimize-scene-image-prompt` | 故事场景图片提示词优化（读取数据库模板 `story_scene_image_prompt_optimize::v1`，通过 tool call + schema 修复返回 `visualPrompt` / `negativePrompt`） |
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

#### 3.5.1 用户关注

用户关注以当前登录用户为发起方，只允许关注正常且未删除的其他用户。

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/ts-user-follows/following` | 分页查询当前用户关注的用户 |
| GET | `/ts-user-follows/followers` | 分页查询当前用户的粉丝 |
| GET | `/ts-user-follows/status` | 查询当前用户对目标用户的关注状态和目标用户实时计数 |
| POST | `/ts-user-follows` | 关注目标用户 |
| DELETE | `/ts-user-follows` | 取消关注目标用户 |

分页参数为 `pageNo/pageSize/keyword`，其中 `pageSize` 最大为 `100`。
操作和状态参数为 `targetUserId`。禁止关注自己；关注与取消关注均为幂等操作。
状态响应包含 `followed/followerCount/followingCount`。

#### 3.5.2 角色与故事点赞

点赞与收藏是两套独立关系，仅允许点赞当前在线公开的角色或故事。

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/ts-user-resource-likes` | 分页查询当前用户点赞的角色和故事 |
| GET | `/ts-user-resource-likes/status` | 查询指定资源点赞状态和实时点赞总数 |
| POST | `/ts-user-resource-likes` | 点赞在线公开角色或故事 |
| DELETE | `/ts-user-resource-likes` | 取消角色或故事点赞 |

分页参数与收藏一致。操作和状态参数为 `resourceType/resourceId`，
其中 `resourceType` 仅支持 `role` 或 `story`。点赞与取消点赞均为幂等操作；
状态响应包含 `liked/likeCount`。

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

`GET /ts-roles/public/detail` 支持按 `id`、`publicId` 或 `channelCode` 查询在线且审核通过的公开角色。详情除公开列表基础字段外，还返回：
- 角色资料：`greeting`、`backgroundStory`、`dialoguePreview`、`dialogueLength`、`toneTendency`、`interactionMode`、`voiceName`。
- 角色统计：`connectorCount`、`followerCount`、`dialogueCount`。角色暂无统计记录时均返回 `0`。

### 3.6 预设与标签资源
以下资源型控制器均遵循统一的标准 CRUD 形态：`list / add / edit / queryById / delete / deleteBatch`。

| 控制器 | 基础路径 | 说明 |
|---|---|---|
| `TsPresetController` | `/sys/tsPreset` | 生成预设主表 |
| `TsTagController` | `/sys/tsTag` | 固定内容标签词典 |
| `TsTagTypeController` | `/sys/tsTagType` | 固定内容标签类型 |

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

`POST /sys/ts-stories/one-click-scene-image` 接收 `title/storySetting/siteSetting/plotOutline/styleName/aspectRatio/referenceImageUrl`，以及可选的 `time/weather/mood` 场景选项对象。每个场景选项只包含英文 `key` 与 `description`，例如 `{ "key": "day", "description": "Bright natural daylight with clear visual layers" }`。其中 `storySetting`、`siteSetting` 与三个选项描述均可作为场景上下文；默认风格为“写实影视级场景概念图”，默认比例为 `9:16`。接口只返回供应商原始 `imageUrl`、`promptCode` 和 `promptVersion`，不导入用户素材，也不更新故事 `sceneImageUrl`。

`POST /sys/ts-stories/optimize-scene-image-prompt` 仅接收 `promptText`，读取模板 `story_scene_image_prompt_optimize::v1`，返回润色后的 `visualPrompt`、`negativePrompt`、`promptCode`、`promptVersion`、`renderedPrompt` 和 `snapshotKey`。

### 4.3 聊天生成
- `POST /ts-chat-sessions/ai-reply`
- `POST /ts-chat-sessions/ai-reply-template`
- `POST /ts-chat-sessions/reply-suggestions`
- `POST /ts-agent-chat-sessions/ai-reply`

### 4.4 当前语音链路说明
- `POST /ts-chat-sessions/ai-reply` 会在后端直接产出语音元信息。
- `POST /ts-chat-sessions/message-tts` 默认返回临时 data URL；`stream=true` 时返回 `audio/mpeg` 流。
- 消息 TTS 的角色和音色以消息 `senderId + contentJson.voiceSnapshot` 为准，不在播放阶段查询角色表。
- 旧消息没有音色快照时不兜底、不补写，直接返回“当前消息未保存音色快照”。
- `audioCacheKey` 仅作为 Web 本地缓存键使用。

### 4.5 模板与修复约束
- 模板来源通过 AI 应用 metadata 的 `code + version` 定位。
- ToolCall required 字段校验失败时，必须进入 JSON Repair 修复链路。
- 当前修复链路应与工具 schema 保持一致，避免输出字段漂移。

### 4.6 反馈中心

用户端接口均要求登录。`GET` 接口的资源 ID 使用查询参数传递，`POST/PUT` 接口的资源 ID 使用 JSON Body 传递：

- `POST /sys/ts-feedback`：发布反馈，支持 `image/screenshot/log` 附件引用；新内容默认进入 `pending` 审核状态。
- `GET /sys/ts-feedback`：公开反馈分页，支持 `type/status/sort/keyword/pageNo/pageSize`，仅返回审核通过内容。
- `GET /sys/ts-feedback/detail?feedbackId=1`：反馈详情、追加内容与附件。
- `GET /sys/ts-my-feedback`：当前用户反馈分页，包含本人待审核或已驳回内容及审核原因。
- `POST /sys/ts-feedback/like`：幂等点赞已审核通过的反馈，Body：`{"feedbackId":1}`。
- `POST /sys/ts-feedback/append`：反馈发起人追加内容并进入审核，Body：`{"feedbackId":1,"content":"补充内容"}`。
- `GET /sys/ts-feedback/comments?feedbackId=1`：一级评论分页，每条默认附带前 2 条回复。
- `POST /sys/ts-feedback/comments`：发布一级评论并进入审核，Body：`{"feedbackId":1,"content":"评论内容"}`。
- `GET /sys/ts-comments/replies?commentId=1`：指定一级评论的二级回复分页。
- `POST /sys/ts-comments/reply`：回复已通过审核的评论并进入审核，Body：`{"commentId":1,"content":"回复内容"}`；回复二级评论时仍归入其一级评论。
- `POST /sys/ts-comments/like`：幂等点赞已审核通过的评论，Body：`{"commentId":1}`。

管理端接口：

- `PUT /sys/ts-admin-feedback/status`：更新状态，Body：`{"feedbackId":1,"status":"processing"}`，权限 `feedback:admin:status`。
- `POST /sys/ts-admin-feedback/reply`：发布官方回复，Body：`{"feedbackId":1,"content":"官方回复"}`，权限 `feedback:admin:reply`。
- `GET /sys/ts-admin-feedback/audit`：统一审核队列，支持 `targetType/auditStatus/keyword/pageNo/pageSize`，权限 `feedback:admin:audit`。
- `PUT /sys/ts-admin-feedback/audit`：审核反馈、评论/回复或追加内容，Body：`{"targetType":"comment","targetId":1,"auditStatus":"rejected","auditReason":"原因"}`，权限 `feedback:admin:audit`。

反馈类型为 `feature/bug/experience`，业务处理状态为 `received/processing/completed`，审核状态为 `pending/approved/rejected`，排序支持 `latest/hot`。审核通过或驳回均写入审核日志；官方回复默认通过。反馈的 `commentCount` 只统计审核通过的评论及回复，评论进入或离开 `approved` 时原子增减。

### 4.7 积分、充值与统一账单

用户积分接口均要求登录，用户 ID 只从当前登录态读取：

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/sys/ts-points/account` | 查询当前用户积分账户，不存在时创建零余额账户 |
| GET | `/sys/ts-points/transactions` | 分页查询当前用户积分流水 |
| GET | `/sys/ts-points/transactions/detail?id=1` | 查询当前用户积分流水详情 |
| GET | `/sys/ts-points-recharge/products` | 查询启用的积分充值商品 |
| POST | `/sys/ts-points-recharge/order` | 创建积分充值订单和第三方支付 |
| POST | `/sys/ts-points-recharge/order/detail` | 查询当前用户积分充值订单及渠道状态 |
| GET | `/sys/ts-billing/records` | 查询用户视角统一账单 |
| GET | `/sys/ts-billing/records/detail` | 查询用户视角账单详情 |

可信业务模块优先直接调用 `ITsPointsService`；HTTP 内部接口要求
`ts:points:internal` 权限：

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/sys/internal/ts-points/add` | 增加积分 |
| POST | `/sys/internal/ts-points/consume` | 消费积分 |
| POST | `/sys/internal/ts-points/refund` | 关联原消费流水返还积分 |

积分后台接口要求管理员角色：

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/sys/ts-points-admin/account/page` | 分页查询积分账户 |
| POST | `/sys/ts-points-admin/transaction/page` | 分页查询积分流水 |
| POST | `/sys/ts-points-admin/adjust` | 后台增加或扣减积分，操作人取当前管理员 |
| POST | `/sys/ts-points-admin/recharge/page` | 分页查询积分充值订单 |
| GET | `/sys/ts-points-admin/product/list` | 查询积分充值商品配置 |
| POST | `/sys/ts-points-admin/product/save` | 新增或编辑积分充值商品 |
| GET | `/sys/ts-points-admin/member-gift-rule/list` | 查询会员积分赠送规则 |
| POST | `/sys/ts-points-admin/member-gift-rule/save` | 新增或编辑会员积分赠送规则 |
| POST | `/sys/ts-billing-admin/page` | 查询平台视角统一账单 |
| POST | `/sys/ts-billing-admin/detail` | 查询平台视角账单详情 |
| POST | `/sys/ts-billing-admin/summary` | 汇总平台现金和积分收支 |

账单使用 `moneyDirection` 与 `pointsDirection` 分别表达现金和积分方向。
用户视角下购买会员为现金支出，充值积分为现金支出和积分收入；平台视角
方向相反。充值订单作为复合账单展示，其关联的 `RECHARGE` 积分流水不会
在统一账单中重复展示，但仍可在积分流水接口查询。

余额不足响应示例：

```json
{
  "success": false,
  "code": 409,
  "errorCode": "POINTS_NOT_ENOUGH",
  "errorCategory": "POINTS",
  "retryable": false,
  "message": "积分余额不足",
  "errorArgs": {
    "required": 100,
    "balance": 60
  }
}
```

### 4.8 活动中心

用户活动接口要求登录，用户 ID 只从当前登录态读取：

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/sys/ts-activity/home` | 查询签到状态、连续天数、七天奖励日历、每日/每周任务和星钻余额 |
| POST | `/sys/ts-activity/sign` | 幂等执行每日签到，并按配置发放每日及周期里程碑星钻 |
| GET | `/sys/ts-activity/tasks` | 查询当前周期任务，可使用 `category` 查询参数 |
| POST | `/sys/ts-activity/task/receive` | 领取任务奖励，`taskId` 通过 JSON Body 传递 |
| GET | `/sys/ts-activity/rewards` | 分页查询当前用户奖励记录 |

活动后台接口要求管理员角色：

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/sys/ts-activity-admin/task/page` | 分页查询活动任务 |
| POST | `/sys/ts-activity-admin/task/create` | 创建活动任务 |
| POST | `/sys/ts-activity-admin/task/update` | 编辑活动任务，`id` 通过 JSON Body 传递 |
| POST | `/sys/ts-activity-admin/user-task/page` | 分页查询用户任务进度 |
| POST | `/sys/ts-activity-admin/reward/page` | 分页查询活动奖励记录 |
| GET | `/sys/ts-activity-admin/reward-rule/list` | 查询会员奖励加成规则 |
| POST | `/sys/ts-activity-admin/reward-rule/save` | 保存会员奖励加成规则 |
| GET | `/sys/ts-activity-admin/sign-milestone/list` | 查询签到周期里程碑奖励规则，可选 `taskId` |
| POST | `/sys/ts-activity-admin/sign-milestone/save` | 保存签到周期里程碑奖励规则 |

创建或编辑活动任务时可传 `rewardClaimMode`：
- `MANUAL`：任务完成后通过 `/sys/ts-activity/task/receive` 手动领取，默认值。
- `AUTO`：任务首次完成后自动提交奖励事件，无需调用领取接口。

自动任务完成后奖励状态先变为 `GRANTING`；奖励成功后变为 `CLAIMED`。
发放失败由统一奖励事件定时重试，同一用户、任务和周期只会成功发放一次。

签到里程碑规则只能绑定 `SIGN` 任务，`milestoneDay` 取值为 `1-7`。
连续签到按七天循环，第 8 天进入下一轮第 1 天，第 11、14 天会再次命中
第 4、7 天规则。里程碑奖励独立生成 `SIGN_MILESTONE` 奖励记录，不重复
应用会员加成；幂等键包含用户、任务、周期轮次和周期天。
同时包含周期起始日期，断签后重新连续签到不会与旧周期撞键。
数据库迁移会为现有每日签到任务默认补充第 4 天 10 星钻、第 7 天 20 星钻，
已有相同周期天规则时不覆盖。

活动首页的 `signRewards` 固定返回七天周期奖励；每项包含 `day`、
`baseRewardAmount`、`milestoneRewardAmount` 和 `rewardAmount`。当前没有
启用中的每日签到任务时返回空数组。默认独立数据补丁配置每日基础奖励
10 星钻，第 4 天额外 10 星钻，第 7 天额外 20 星钻。

可信业务模块优先直接调用 `ITsActivityService`；HTTP 内部接口要求
`ts:activity:internal` 权限：

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/sys/internal/ts-activity/progress` | 幂等上报用户行为进度 |

行为请求 Body 包含 `userId`、`conditionType`、`count`、`bizId`。
`bizId` 在同一用户和行为类型下唯一，重复上报返回 `duplicate=true`，
不重复增加进度。每日、每周和长期任务分别使用日期、ISO 周和 `LONG`
作为周期键。

`conditionType` 支持：
- `LOGIN`：登录或签到。
- `CHAT_COUNT`：AI 角色有效回复次数，同一次回复可同时推进每日和每周任务。
- `ROLE_CREATE`、`STORY_CREATE`：角色或故事创建成功。
- `ROLE_IMAGE_GENERATE`、`STORY_BACKGROUND_GENERATE`：角色图片或故事背景生成成功。
- `STORY_INTERACTION_COUNT`：关联 `storyId` 的故事会话有效回复次数。
- `IMAGE_GENERATE`、`VOICE_USE`：保留的历史通用图片和语音条件。

业务入口通过事务提交后安全上报器推进活动进度；活动上报失败只记录告警，
不会回滚聊天、创建或图片生成。独立补丁
`activity-default-tasks-patch.sql` 可为现有数据库初始化以下自动发奖任务，
该补丁不包含在完整数据库基线及 Jeecg Boot 版本迁移中：

| 周期 | 任务 | 条件 | 奖励 |
|---|---|---|---|
| 每日 | 与 AI 角色聊天 10 次 | `CHAT_COUNT=10` | 5 星钻 |
| 每日 | 生成角色图片 | `ROLE_IMAGE_GENERATE=1` | 10 星钻 |
| 每日 | 生成故事背景 | `STORY_BACKGROUND_GENERATE=1` | 10 星钻 |
| 每日 | 创建角色 | `ROLE_CREATE=1` | 20 星钻 |
| 每日 | 创建故事 | `STORY_CREATE=1` | 20 星钻 |
| 每日 | 故事互动 5 次 | `STORY_INTERACTION_COUNT=5` | 10 星钻 |
| 每周 | 累计聊天 100 次 | `CHAT_COUNT=100` | 20 星钻 |

星钻奖励统一经过 `ITsRewardService -> ITsPointsService.add()`，签到使用
积分业务类型 `SIGN_IN`，其他任务使用 `ACTIVITY_REWARD`。现有会员
`FREE/PRO/ULTRA` 在活动域映射为 `NORMAL/VIP/SVIP`。当前仅启用
`STAR_DIAMOND` 发放器，`ITEM/TITLE/AVATAR_FRAME` 保留扩展模型。

### 4.9 统一奖励事件管理

奖励事件后台接口要求管理员角色，只提供监控和失败重试，不允许修改奖励金额：

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/sys/ts-reward-admin/event/page` | 分页查询奖励事件 |
| POST | `/sys/ts-reward-admin/event/summary` | 按查询条件汇总事件状态 |
| GET | `/sys/ts-reward-admin/event/detail` | 查询事件详情，`id` 使用查询参数 |
| POST | `/sys/ts-reward-admin/event/retry` | 重试失败事件，`eventId` 通过 JSON Body 传递 |

事件类型包括 `SIGN_COMPLETED`、`SIGN_MILESTONE_COMPLETED`、
`TASK_REWARD_RECEIVED` 和 `MEMBER_ACTIVATED`；状态包括
`PENDING`、`PROCESSING`、`SUCCESS`
和 `FAILED`。只有 `FAILED` 且 `retryCount < maxRetryCount` 的事件
可以手动重试，重复执行继续由事件 ID 和积分流水幂等 Key 防重。

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

## 8. 作品审核接口

### 8.1 自动触发
- `POST /sys/ts-roles`、`PUT /sys/ts-roles`、完整角色生成完成后自动提交角色审核。
- `POST /sys/ts-stories`、`PUT /sys/ts-stories` 在故事和角色绑定落库后自动提交故事审核。
- 保存时作品立即转为私有，返回对象包含 `contentVersion`、`reviewStatus`、`currentReviewId`、`desiredPublic`。
- 内容更新会生成新版本，旧的进行中任务转为 `OBSOLETE`。

### 8.2 用户查询
- `GET /sys/ts-work-reviews/current`
- 鉴权：登录用户，仅可查询自己的作品。
- 查询参数：`workType=ROLE|STORY`、`workId`。
- 返回：当前审核任务、版本、快照摘要、AI/管理员结论、审核项和日志。

### 8.3 管理员接口
- 以下接口要求 `admin` 角色。
- `GET /sys/ts-admin-work-reviews`：按 `workType/status/ownerUserId/pageNo/pageSize` 分页。
- `GET /sys/ts-admin-work-reviews/detail?id=1`：查询完整快照、文本/图片项和日志。
- `POST /sys/ts-admin-work-reviews/approve`：Body 为 `{"id":1,"reason":"审核意见"}`。
- `POST /sys/ts-admin-work-reviews/reject`：Body 为 `{"id":1,"reason":"必填驳回原因"}`。
- `POST /sys/ts-admin-work-reviews/retry-ai`：Body 为 `{"id":1}`。

### 8.4 状态与公开门禁
- 状态流转：`PENDING_AI -> PENDING_ADMIN -> APPROVED/REJECTED`，内容更新后旧任务为 `OBSOLETE`。
- 管理员只允许处理当前版本的 `PENDING_ADMIN`；AI 失败保持 `PENDING_AI`。
- 通过后作品恢复作者保存时的 `desiredPublic`，驳回后保持私有。
- 角色和故事公开记录写操作及游客公开列表/详情均要求当前作品 `reviewStatus=APPROVED`。

## 9. 海报与广告运营管理

### 9.1 管理员接口
以下接口要求 `admin` 角色：

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/sys/ts-ad-admin/slot/page` | 分页查询广告位 |
| GET | `/sys/ts-ad-admin/slot/detail?id=1` | 查询广告位详情 |
| POST | `/sys/ts-ad-admin/slot/create` | 创建广告位 |
| POST | `/sys/ts-ad-admin/slot/update` | 更新广告位，ID通过JSON Body传递 |
| POST | `/sys/ts-ad-admin/slot/delete` | 删除空广告位 |
| POST | `/sys/ts-ad-admin/slot/status` | 启用或停用广告位 |
| POST | `/sys/ts-ad-admin/content/page` | 分页查询广告内容 |
| GET | `/sys/ts-ad-admin/content/detail?id=1` | 查询广告内容详情 |
| POST | `/sys/ts-ad-admin/content/create` | 创建草稿内容 |
| POST | `/sys/ts-ad-admin/content/update` | 更新内容并自动回到草稿 |
| POST | `/sys/ts-ad-admin/content/delete` | 删除广告内容 |
| POST | `/sys/ts-ad-admin/content/publish` | 发布广告内容 |
| POST | `/sys/ts-ad-admin/content/offline` | 下线广告内容 |
| GET | `/sys/ts-ad-admin/delivery-rule?contentId=1` | 查询投放规则 |
| POST | `/sys/ts-ad-admin/delivery-rule/save` | 保存投放规则 |
| POST | `/sys/ts-ad-admin/stats/summary` | 汇总曝光、点击和点击率 |

投放规则支持平台 `ALL/WEB/IOS/ANDROID`、受众
`ALL/LOGIN/ANONYMOUS/USER_LIST`、会员等级
`ALL/FREE/PRO/ULTRA`。`USER_LIST` 必须配置 `userIds`。

广告内容保存参数支持以下媒体与动作字段：

- `sourceType`：`SELF` 自有素材、`EXTERNAL` 外部素材；后端预留
  `AD_NETWORK`，当前管理端不开放。
- `mediaType`：`IMAGE`、`VIDEO`、`CARD`。图片和视频使用 `mediaUrl`，
  视频可选 `posterUrl`；卡片使用 `cardType` 与 `payloadJson`，不要求媒体地址。
- `actionType`：`NONE`、`URL`、`ROUTE`、`ROLE`、`STORY`、`DEEP_LINK`，
  目标值通过 `actionPayload` 传递。`imageUrl/linkType/linkValue` 继续保留，
  用于兼容既有调用，后端会同步写入规范化字段。
- `EXTERNAL` 素材地址和 `URL` 动作目标必须为 HTTP/HTTPS 地址；卡片
  `payloadJson` 必须是 JSON 对象。

### 9.2 前端投放接口

| 鉴权 | 方法 | 路径 | 说明 |
|---|---|---|---|
| 登录 | GET | `/sys/ts-ads/delivery?slotCodes=HOME_BANNER&platform=WEB` | 查询登录用户可见广告 |
| 匿名 | GET | `/sys/ts-ads/public/delivery?slotCodes=HOME_BANNER&platform=WEB` | 查询匿名用户可见广告 |
| 登录 | POST | `/sys/ts-ads/event` | 上报曝光或点击事件 |
| 匿名 | POST | `/sys/ts-ads/public/event` | 上报曝光或点击事件 |

投放接口仅返回启用广告位中已发布且处于有效时间窗的内容。登录状态、
用户ID和会员等级均由后端计算；匿名事件必须提供 `visitorId`。事件 Body
包含 `eventId/contentId/slotCode/eventType/platform/occurredAt`，
`eventType` 支持 `IMPRESSION/CLICK`，重复 `eventId` 返回 `false`。
广告事件的 `occurredAt` 时间格式与业务行为埋点一致，推荐使用带时区的
ISO 8601 格式，同时兼容按 GMT+8 解释的 `yyyy-MM-dd HH:mm:ss` 和毫秒时间戳。

### 9.3 管理后台页面

- 菜单：探拾 / 运营内容管理。
- 路由：`/tanshi/adCenter`。
- 组件：`system/tanshi/adCenter/index`。
- 页面包含广告位、广告内容、投放数据三个页签；广告内容支持自有/外部图片、
  视频、卡片内容录入与预览，投放规则通过独立抽屉维护。
- 菜单迁移：`db/V3.9.1_49__add_tanshi_ad_center_menu.sql`，默认授权 `admin` 角色。
- 字段迁移：`db/V3.9.1_51__expand_ts_ad_content_media.sql`，将既有图片和跳转字段
  回填到规范化媒体/动作字段。

## 10. 业务行为分析埋点接口

接口要求登录，用户 ID 只从后端登录态读取。请求线程仅校验并异步提交
Kafka，由明细消费者写入 ClickHouse：

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/sys/ts-events/collect` | 上报单条白名单业务事实 |
| POST | `/sys/ts-events/collect/batch` | 批量上报行为，单批最多100条 |

当 `TS_BEHAVIOR_KAFKA_ENABLED=false` 时，接口正常返回成功结果且
`acceptedCount=0`，不会发送 Kafka 或写入 ClickHouse。

事件必填 `eventId/eventType/sessionId`；可选
`resourceType/resourceId/pagePath/platform/properties/occurredAt`。
客户端仍不得上传标签或内容版本。服务端会将事件统一升级为 v3，并根据
`resourceType + resourceId` 补充行为发生时的 `contentVersion/tagIds/tagScores`
后写入 Kafka 和 ClickHouse；无标签内容使用空数组。
`occurredAt` 推荐使用带时区的 ISO 8601 格式，例如
`2026-08-26T12:15:39.125Z` 或 `2026-08-26T20:15:39.125+08:00`；
为兼容既有调用，也接受按 GMT+8 解释的 `yyyy-MM-dd HH:mm:ss` 和毫秒时间戳。
扩展 JSON 最大8KB，事件时间允许最近7天至未来5分钟。

允许的事件类型如下，不接受标签、停留时长等旧推荐字段。推荐曝光仅允许
`scene/requestId/position` 三个归因属性，`position` 必须为正整数：

| 事件类型 | 资源 | 扩展属性 |
|---|---|---|
| `user_language` | 无 | 必填 `language` |
| `detail_view` | `role` 或 `story` | 无 |
| `impression` | `role` 或 `story` | 必填 `scene/requestId/position` |
| `favorite` | `role` 或 `story` | 无 |
| `unfavorite` | `role` 或 `story` | 无，仅后端业务事件 |
| `connection` | `role` 或 `story` | 无 |
| `chat_message` | `role` 或 `story` | 无 |
| `role_create` | `role` | 可选 `gender` |
| `story_create` | `story` | 无 |
| `role_image_generate` | `role_image` | 可选 `gender/style` |
| `story_background_generate` | `story_background` | 可选 `style` |

前端负责 `user_language`、角色/故事 `detail_view` 和推荐卡片 `impression`；
收藏、取消收藏、连接、聊天、创建和生成事件由后端业务成功点可信上报。
`unfavorite` 仅在有效收藏实际变为取消状态时产生，重复取消不会重复上报。
旧的评论、点赞、发布、泛生成 AOP 埋点及 Redis 实时特征消费者已移除。
