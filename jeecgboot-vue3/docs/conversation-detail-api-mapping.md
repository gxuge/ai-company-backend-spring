# Conversation Detail 页面接口对接清单（粗判版）

## 1. 目的

本文用于快速判断 `conversation-detail` 页面应优先对接哪些后端接口，先满足页面动态化展示，再逐步补齐交互能力。

- 页面代码：`D:\project_demo\ai-company-frontend-native\src\app\pages\conversation-detail\index.tsx`
- 入口代码：`D:\project_demo\ai-company-frontend-native\src\app\pages\chat\components\chat-header\index.tsx`
- 参考接口文档：`D:\project_demo\ai-company-backend-spring\jeecg-boot\docs\api\ts-api.md`
- TS 业务接口前缀：`/jeecg-boot/sys`

---

## 2. 页面动态区块与数据来源

`conversation-detail` 目前是静态内容，动态化后可拆成以下数据块：

1. 顶部标题、简介、背景图
2. 创作者信息（昵称、头像）
3. 关注数/热度（若展示）
4. 角色列表（头像 + 名称）
5. “故事详情”按钮跳转数据
6. “观感”卡片（可后续扩展）

---

## 3. 必接接口（MVP）

## 3.1 查询会话详情

- 方法：`GET`
- 路径：`/sys/ts-chat-sessions/detail`
- 参数：`id`（会话 ID）
- 用途：
  - 决定会话类型：`sessionType`（`single` / `story`）
  - 拿到主关联：`storyId` 或 `targetRoleId`
  - 可作为标题兜底：`sessionTitle`

示例：

```http
GET /jeecg-boot/sys/ts-chat-sessions/detail?id=1001
```

## 3.2 查询故事详情（story 会话主数据）

- 方法：`GET`
- 路径：`/sys/ts-stories/detail`
- 参数：`id`（故事 ID）
- 用途：
  - 页面标题：`title`
  - 页面描述：`storyIntro` / `storyBackground` / `storySetting`（按产品文案策略取其一或拼接）
  - 背景图：`coverUrl`
  - 热度信息：`followerCount`、`dialogueCount`
  - 角色绑定：`roleBindings`（含 `roleId`）

示例：

```http
GET /jeecg-boot/sys/ts-stories/detail?id=2001
```

## 3.3 查询角色详情（按 roleId 拉取）

- 方法：`GET`
- 路径：`/sys/ts-roles/detail`
- 参数：`id`（角色 ID）
- 用途：
  - 角色名称：`roleName`
  - 角色头像：`avatarUrl`
  - 角色补充文案：`roleSubtitle` / `introText`

示例：

```http
GET /jeecg-boot/sys/ts-roles/detail?id=3001
```

> 建议：先用 `ts-stories/detail` 拿 `roleBindings`，再并发调用多个 `ts-roles/detail` 组装角色卡片。

---

## 4. 可选接口（第二阶段）

## 4.1 故事章节列表（故事详情页或章节预览）

- 方法：`GET`
- 路径：`/sys/ts-story-chapters`
- 参数：`storyId` 必传，分页参数可带

示例：

```http
GET /jeecg-boot/sys/ts-story-chapters?storyId=2001&pageNo=1&pageSize=20
```

## 4.2 会话消息列表（用于“观感/最近互动”）

- 方法：`GET`
- 路径：`/sys/ts-chat-messages`
- 参数：`sessionId` 必传（建议）

示例：

```http
GET /jeecg-boot/sys/ts-chat-messages?sessionId=1001&pageNo=1&pageSize=20
```

## 4.3 消息附件列表（图片/音频素材）

- 方法：`GET`
- 路径：`/sys/ts-chat-message-attachments`
- 参数：`messageId`

---

## 5. 页面字段映射建议

| 页面区域 | 优先字段 | 接口 |
|---|---|---|
| 主标题 | `title`（兜底 `sessionTitle`） | `ts-stories/detail` + `ts-chat-sessions/detail` |
| 创作者昵称 | `createdName`（兜底 `createdBy`） | `ts-stories/detail` |
| 背景图 | `coverUrl` | `ts-stories/detail` |
| 描述文本 | `storyIntro`（兜底 `storyBackground`） | `ts-stories/detail` |
| 角色列表 | `roleBindings[].roleId -> roleName/avatarUrl` | `ts-stories/detail` + `ts-roles/detail` |
| 热度值 | `followerCount`、`dialogueCount` | `ts-stories/detail` |
| 故事详情跳转 | `storyId` | `ts-chat-sessions/detail` |

---

## 6. 调用顺序建议

1. 从聊天页进入详情页时，携带 `sessionId`（必传）。
2. 进入详情页先请求 `ts-chat-sessions/detail`。
3. 如果 `sessionType=story` 且 `storyId` 存在，请求 `ts-stories/detail`。
4. 从 `roleBindings` 取 `roleId`，并发请求 `ts-roles/detail`，渲染角色列表。
5. 点击“故事详情”时，再请求或复用 `ts-story-chapters`。

---

## 7. 当前缺口（需后端评估）

以下能力在当前 `ts-api.md` 中未见直接接口：

1. 关注/取消关注作者（按钮行为）
2. 创作者头像字段（`TsStoryVo` 目前无作者头像）

建议后续新增：

1. `POST /sys/ts-stories/{id}/follow` 与 `DELETE /sys/ts-stories/{id}/follow`（或等价接口）
2. 在故事详情返回中补充 `creatorAvatarUrl`，或提供用户资料查询接口

---

## 8. 前端接入注意点

1. 详情页当前为静态 mock，需要把页面文案与图片资源替换成接口字段。
2. `chat-header` 当前跳转未传参，建议改为携带 `sessionId`（例如 query params）。
3. 字段容错建议：空值时给默认文案和默认头像，避免白屏。
4. 角色详情建议并发请求并做失败降级（单角色失败不影响整体页面）。

