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
| POST | `/ts-stories/story-setting-generate` | 故事设定生成（标题/简介/设定/背景，按模板+模型返回结构化字段） |
| POST | `/ts-stories/story--scene-generate` | 场所设定生成（场所快照名/场景摘要/场景元素） |
| POST | `/ts-stories/story--outline-generate` | 剧情大纲生成（支持 `storySetting/sceneSetting` 缺省，按上下文补全章节） |
| GET | `/ts-story-chapters` | 查询章节列表 |
| POST | `/ts-story-chapters` | 新增章节 |

### 3.3 音色与资产（`TsVoiceProfileController` / `TsVoiceTagController` / `TsUserVoiceConfigController` / `TsUserImageAssetController`）
| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/ts-voice-profiles` | 查询音色档案列表 |
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
