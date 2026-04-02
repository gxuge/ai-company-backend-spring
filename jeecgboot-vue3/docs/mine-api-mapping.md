# Mine 页面接口对接（粗略版）

## 1. 适用范围
- 页面：`src/app/pages/mine/index.tsx`（前端项目）
- 目标：先把 `Mine` 页从静态数据切到后端真实数据
- 说明：本文档基于现有 `ts-api.md` 和后端 Controller/VO 做粗略判断

## 2. 建议对接接口

### 2.1 用户信息（头像、昵称、UID）
- 方法：`GET`
- 路径：`/sys/user/getUserInfo`
- 用途：
  - 头像：`userInfo.avatar`
  - 用户名：`userInfo.realname || userInfo.username`
  - UID：`userInfo.id`

### 2.2 我的故事（Tab: 故事）
- 方法：`GET`
- 路径：`/sys/ts-stories`
- 建议参数：
  - `pageNo=1`
  - `pageSize=20`
- 用途：
  - 故事列表（卡片）
  - 故事总数（可作为统计项之一）

### 2.3 我的角色（Tab: 角色）
- 方法：`GET`
- 路径：`/sys/ts-roles`
- 建议参数：
  - `pageNo=1`
  - `pageSize=20`
- 用途：
  - 角色列表（卡片）
  - 角色总数（可作为统计项之一）

### 2.4 可选：我的图片素材（若 Mine 要展示素材流）
- 方法：`GET`
- 路径：`/sys/ts-user-image-assets`
- 建议参数：
  - `pageNo=1`
  - `pageSize=20`

## 3. 前端字段映射建议

### 3.1 顶部信息区
- 头像：`getUserInfo.userInfo.avatar`
- 昵称：`getUserInfo.userInfo.realname || getUserInfo.userInfo.username`
- UID：`getUserInfo.userInfo.id`

### 3.2 故事 Tab 卡片（`/sys/ts-stories`）
- 封面：`coverUrl`
- 标题：`title`
- 作者名：`createdName`（兜底当前用户昵称）
- 热度/浏览（临时口径）：`dialogueCount` 或 `followerCount`

### 3.3 角色 Tab 卡片（`/sys/ts-roles`）
- 封面：`coverUrl`（兜底 `avatarUrl`）
- 标题：`roleName`
- 副标题：`roleSubtitle`

## 4. 统计区（关注/粉丝/点赞）现状判断
- 当前文档与 TS 业务接口中，未看到专门的“用户聚合统计”接口。
- 现阶段可临时处理：
  - 关注：用 `stories.total` 或 `roles.total`（按产品定义选一种）
  - 粉丝：可粗略累加 `story.followerCount`
  - 点赞：暂无明确字段，建议后端补聚合接口

## 5. 最小请求顺序（推荐）
1. 页面进入先调 `GET /sys/user/getUserInfo`
2. 默认 Tab 首次加载时调对应列表（故事或角色）
3. 切换 Tab 再调另一列表（结果可本地缓存，避免重复请求）

## 6. 额外说明
- 三个 TS 列表接口在后端实现中均按当前登录用户进行数据范围限制（用户隔离）。
- 因此可直接用于“我的”页面，不需要前端额外传 `userId`。
