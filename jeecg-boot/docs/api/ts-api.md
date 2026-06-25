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
| `TsBrowsePublicController` | `/sys` | 公开故事/角色/形象浏览 |
| `TsChatMessageAttachmentController` | `/sys/ts-chat-message-attachments` | 聊天附件 CRUD |
| `TsChatMessageController` | `/sys/ts-chat-messages` | 聊天消息 CRUD |
| `TsChatSessionController` | `/sys/ts-chat-sessions` | 聊天会话 CRUD + AI 回复/语音/候选建议 |
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

### 3.2 角色与故事核心
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
| POST | `/ts-stories/story--outline-generate` | 故事大纲生成 |
| POST | `/ts-stories/story-full-generate-preset` | 故事全量生成预设版 |
| GET | `/ts-story-chapters` | 章节分页查询 |
| GET | `/ts-story-chapters/detail` | 章节详情 |
| POST | `/ts-story-chapters` | 新增章节 |
| PUT | `/ts-story-chapters` | 编辑章节 |
| DELETE | `/ts-story-chapters` | 删除章节 |

### 3.3 公开浏览与公开管理
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

### 3.4 预设与标签资源
以下资源型控制器均遵循统一的标准 CRUD 形态：`list / add / edit / queryById / delete / deleteBatch`。

| 控制器 | 基础路径 | 说明 |
|---|---|---|
| `TsPresetController` | `/sys/tsPreset` | 生成预设主表 |
| `TsPresetTagController` | `/sys/tsPresetTag` | 预设-标签关联 |
| `TsTagController` | `/sys/tsTag` | 生成素材标签主表 |
| `TsTagTypeController` | `/sys/tsTagType` | 标签类型字典 |
| `TsTagRelationController` | `/sys/tsTagRelation` | 标签关系规则 |

### 3.5 音色与资产
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
| GET | `/ts-voice-tags` | 音色标签分页查询 |
| POST | `/ts-voice-tags` | 新增音色标签 |
| DELETE | `/ts-voice-tags` | 删除音色标签 |

### 3.6 AI 日志与 MCP
| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/tsAiLog/list` | AI 调用日志分页查询 |
| GET | `/tsAiLog/queryById` | AI 调用日志按 id 查询 |
| GET | `/tsAiLog/detail` | AI 调用日志详情 |
| GET | `/ts/mcp/sse` | TS MCP SSE 连接端点 |
| POST | `/ts/mcp/sse` | TS MCP Streamable HTTP 端点 |
| POST | `/ts/mcp/message` | TS MCP 消息处理 |
| GET | `/ts/mcp/info` | TS MCP 说明与工具信息 |

## 4. AI Prompt 与 ToolCall 接口

以下接口属于 Prompt 模板驱动或 ToolCall 风格的生成型接口，通常由后端根据 AI 应用 metadata 的 `code+version` 定位模板，并在失败时进入 JSON Repair 修复链路。

### 4.1 角色生成
- `POST /ts-roles/one-click-setting`
- `POST /ts-roles/one-click-setting-preset`
- `POST /ts-roles/one-click-image`
- `POST /ts-roles/one-click-voice`
- `POST /ts-roles/generate-text-by-template`
- `POST /ts-roles/generate-role`

### 4.2 故事生成
- `POST /ts-stories/story-full-generate`
- `POST /ts-stories/story-setting-generate`
- `POST /ts-stories/story--scene-generate`
- `POST /ts-stories/story--outline-generate`
- `POST /ts-stories/story-full-generate-preset`

### 4.3 聊天生成
- `POST /ts-chat-sessions/ai-reply`
- `POST /ts-chat-sessions/ai-reply-template`
- `POST /ts-chat-sessions/reply-suggestions`

### 4.4 当前语音链路说明
- `POST /ts-chat-sessions/ai-reply` 会在后端直接产出语音元信息。
- `POST /ts-chat-sessions/message-tts` 只负责按消息即时生成语音，不依赖服务端缓存表。
- `audioCacheKey` 仅作为 Web 本地缓存键使用。

### 4.5 模板与修复约束
- 模板来源通过 AI 应用 metadata 的 `code + version` 定位。
- ToolCall required 字段校验失败时，必须进入 JSON Repair 修复链路。
- 当前修复链路应与工具 schema 保持一致，避免输出字段漂移。

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
