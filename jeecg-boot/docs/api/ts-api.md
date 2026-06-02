# TS 业务 API（`/sys`）

## 1. 范围
- 模块：`jeecg-module-system/jeecg-system-biz`
- 控制器范围：`Ts*Controller.java`
- 完整访问前缀（默认）：`/jeecg-boot/sys`
- 文档定位：记录 TS 业务域接口入口、资源边界与高频路由

## 2. 控制器总览

| 控制器 | 基础路径 | 主要能力 |
|---|---|---|
| `TsChatMessageAttachmentController` | `/sys/ts-chat-message-attachments` | 聊天附件列表/详情查询与增删改 |
| `TsChatMessageController` | `/sys/ts-chat-messages` | 聊天消息列表/详情查询与增删改 |
| `TsChatSessionController` | `/sys/ts-chat-sessions` | 聊天会话列表/详情查询与增删改 |
| `TsRoleController` | `/sys/ts-roles` | 角色列表/详情查询与增删改 |
| `TsRoleImageGenerateRecordController` | `/sys/ts-role-image-generate-records` | 角色生图记录管理 |
| `TsRoleImageProfileController` | `/sys/ts-role-image-profiles` | 角色形象档案管理 |
| `TsStoryChapterController` | `/sys/ts-story-chapters` | 故事章节管理 |
| `TsStoryController` | `/sys/ts-stories` | 故事主表管理 |
| `TsUserImageAssetController` | `/sys/ts-user-image-assets` | 用户图片资产管理 |
| `TsUserVoiceConfigController` | `/sys/ts-user-voice-config/current` | 当前用户音色配置读取与保存 |
| `TsVoiceProfileController` | `/sys/ts-voice-profiles` | 音色档案管理与标签关联 |
| `TsVoiceTagController` | `/sys/ts-voice-tags` | 音色标签管理 |

## 3. 核心接口（高频）

### 3.1 聊天链路（`TsChatSessionController` / `TsChatMessageController` / `TsChatMessageAttachmentController`）
| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/ts-chat-sessions` | 查询会话列表 |
| GET | `/ts-chat-sessions/detail` | 查询会话详情 |
| POST | `/ts-chat-sessions` | 新增会话 |
| GET | `/ts-chat-messages` | 查询消息列表 |
| GET | `/ts-chat-messages/detail` | 查询消息详情 |
| POST | `/ts-chat-messages` | 新增消息 |
| GET | `/ts-chat-message-attachments` | 查询附件列表 |
| POST | `/ts-chat-message-attachments` | 新增附件 |
| POST | `/ts-chat-sessions/ai-reply` | 在会话内生成 AI 文本回复并产出可播放语音（`sessionId` 在请求体传入） |

### 3.2 角色与故事（`TsRoleController` / `TsRoleImageProfileController` / `TsRoleImageGenerateRecordController` / `TsStoryController` / `TsStoryChapterController`）
| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/ts-roles` | 查询角色列表 |
| POST | `/ts-roles` | 新增角色 |
| POST | `/ts-roles/one-click-setting` | 角色设定一键生成（四核心字段：名称/性别/职业/背景） |
| POST | `/ts-roles/one-click-image` | 角色形象一键生成（可不依赖完整设定，若传设定字段则用于提示词上下文） |
| POST | `/ts-roles/one-click-voice` | 角色声音一键生成（可不依赖完整设定，若传设定字段则用于提示词上下文） |
| POST | `/ts-roles/generate-role` | 随机完整角色生成（融合设定+形象+声音，`storySetting/storyBackground` 可选） |
| GET | `/ts-role-image-profiles` | 查询角色形象档案 |
| POST | `/ts-role-image-profiles` | 新增角色形象档案 |
| GET | `/ts-role-image-generate-records` | 查询角色生图记录 |
| POST | `/ts-role-image-generate-records` | 新增角色生图记录 |
| GET | `/ts-stories` | 查询故事列表 |
| POST | `/ts-stories` | 新增故事 |
| POST | `/ts-stories/story-full-generate` | 故事全量生成（旧入口，当前复用 preset 全量链路；返回核心5字段与分段结果） |
| POST | `/ts-stories/story-full-generate-preset` | 故事全量生成（预设版）：随机 story 预设+绑定标签映射填充核心模板，统一串联设定/场景/大纲生成 |
| GET | `/ts-story-chapters` | 查询章节列表 |
| POST | `/ts-story-chapters` | 新增章节 |

### 3.3 AI Prompt 模板驱动接口（重点）

以下接口均走 Prompt 模板 + ToolCall 结构化输出链路，模板通过 AI 应用 metadata 的 `code+version` 定位，失败时进入 JSON Repair 修复链路。

#### 3.3.1 `POST /ts-stories/story-full-generate`
- 用途：故事核心字段生成（旧链路入口，当前复用 full preset 生成逻辑）。
- 请求体（`TsStoryFullGenerateDto`）：
  - `storyId?: number`
  - `storyMode?: 'normal' | 'chapter'`
  - `templateText?: string`
  - `extraRequirements?: string`
  - `skipOutlineWhenChapter?: boolean`
- 响应体关键字段（`TsStoryFullGenerateVo`）：
  - 核心5字段：`title`、`storyIntro`、`storySetting`、`siteSetting`、`plotOutline`
  - 生成分段结果：`settingResult`、`sceneResult`、`outlineResult`
  - 模板追踪：`promptCode`、`promptVersion`、`renderedPrompt`、`snapshotKey`
- 前端回填建议（当前约定）：
  - 仅当前-故事设定：只取 `storySetting`
  - 仅当前-场景设定：只取 `siteSetting`
  - 仅当前-剧情大纲：只取 `plotOutline`（chapter 模式优先 `outlineResult.chapters`）

#### 3.3.2 `POST /ts-stories/story-full-generate-preset`
- 用途：全量生成入口（随机选一个 `story` 预设并读取绑定标签，按模板映射生成核心字段）。
- 预设与标签取值方式：
  - 从 `ts_preset` 选 `target_type='story' and enabled=1`
  - 通过 `ts_preset_tag` 读取关系
  - 通过 `ts_tag` 读取标签，并按 `type_id` 分组去重聚合
- 请求体：同 `TsStoryFullGenerateDto`
- 响应体：同 `TsStoryFullGenerateVo`，额外包含：
  - `presetId`、`presetName`、`presetDescription`
  - `presetTags[]`

#### 3.3.3 模板解析与修复约束
- 模板来源：
  - 故事模板：`storyPromptTemplate` / `storyPromptTemplates.*` / 场景级 code+version
  - 修复模板：`toolcallJsonRepairPromptTemplate` / `jsonRepairPromptTemplate` / `storyJsonRepairPromptTemplate`
- ToolCall required 字段校验失败时，必须进入修复链路并再次校验。
- 修复变量至少包含：`scene`、`raw_content`、`tool_schema`、`required_fields`、`required_field_hints`。

### 3.4 音色与资产（`TsVoiceProfileController` / `TsVoiceTagController` / `TsUserVoiceConfigController` / `TsUserImageAssetController`）
| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/ts-voice-profiles` | 查询音色档案列表 |
| GET | `/ts-user-voice-profiles` | 查询当前用户音色库列表 |
| PUT | `/ts-user-voice-profiles/{id}` | 重命名当前用户音色（`id` 为 voiceProfileId） |
| DELETE | `/ts-user-voice-profiles/{id}` | 从当前用户音色库移除音色（`id` 为 voiceProfileId） |
| DELETE | `/ts-voice-profiles` | 删除音色档案 |
| GET | `/ts-voice-profiles/tags` | 查询音色标签关系 |
| PUT | `/ts-voice-profiles/tags` | 保存音色标签关系 |
| GET | `/ts-voice-tags` | 查询音色标签列表 |
| POST | `/ts-voice-tags` | 新增音色标签 |
| GET | `/ts-user-voice-config/current` | 查询当前用户音色配置 |
| PUT | `/ts-user-voice-config/current` | 保存当前用户音色配置 |
| GET | `/ts-user-image-assets` | 查询用户图片资产 |
| POST | `/ts-user-image-assets` | 新增用户图片资产 |

## 4. 权限约定
- 当前 `Ts*Controller` 代码未显式标注 `@RequiresPermissions`。
- 接口默认复用系统统一鉴权链路（登录态、租户等基础校验）。
- 如新增后台敏感操作，建议同步补充权限编码与注解声明。

## 5. 配置依赖（摘要）
- 路由实现位于：`jeecg-module-system/jeecg-system-biz/src/main/java/org/jeecg/modules/system/controller/Ts*Controller.java`
- 数据层实现位于：`jeecg-module-system/jeecg-system-biz/src/main/java/org/jeecg/modules/system/mapper` 与 `entity`
- 当前 TS 业务接口无独立配置文件，复用 `jeecg-system-biz` 与全局 Spring Boot 配置。

## 6. 维护说明
- 每次新增/变更 `Ts*Controller` 映射时，同步更新本文件与 `docs/changelog.md`。
