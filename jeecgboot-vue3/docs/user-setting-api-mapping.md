# User Setting 页面接口对接（粗判断）

## 1. 目标与范围
- 页面来源：`ai-company-frontend-native/src/app/pages/user-setting/components/AccountSettings.tsx`
- 参考文档：`ai-company-backend-spring/jeecg-boot/docs/api/ts-api.md`
- 本文性质：先做“可快速落地”的接口映射，后续可再细化 DTO/权限/字段约束。

## 2. 页面字段与接口映射

| 页面项 | 读取接口 | 保存接口 | 说明 |
|---|---|---|---|
| 头像（avatar） | `GET /sys/user/login/setting/getUserData` | `POST /sys/user/login/setting/userEdit`（传 `avatar`） | 头像上传建议先走 `POST /sys/common/upload`，拿到 URL 后再保存到用户资料。 |
| 昵称（nickname） | `GET /sys/user/login/setting/getUserData` | `POST /sys/user/login/setting/userEdit`（传 `realname`） | `user-setting` 页面中的“昵称”可映射为 `SysUser.realname`。 |
| 用户 ID（只读） | `GET /sys/user/login/setting/getUserData` | 无 | 前端只展示，不编辑。 |
| 性别（gender） | `GET /sys/user/login/setting/getUserData` | `POST /sys/user/login/setting/userEdit`（传 `sex`） | 前端需做枚举映射（如 `male/female/secret` -> 后端约定值）。 |
| 生日（birthday） | `GET /sys/user/login/setting/getUserData` | `POST /sys/user/login/setting/userEdit`（传 `birthday`） | 前端日期格式需和后端约定一致（建议 `yyyy-MM-dd`）。 |
| 内容偏好（tags） | 当前无直接接口 | 当前无直接接口 | 若“内容偏好”是题材标签（如都市/职场/情感），建议新增独立接口。 |
| 音色偏好（若纳入设置页） | `GET /sys/ts-user-voice-config/current` | `PUT /sys/ts-user-voice-config/current` | 此项在 `ts-api.md` 内，适合“语音设置”场景。 |
| 用户图片资产（若做头像历史库） | `GET /sys/ts-user-image-assets` | `POST/PUT/DELETE /sys/ts-user-image-assets` | 此项在 `ts-api.md` 内，适合“素材库/历史头像”能力。 |

## 3. 与 `ts-api.md` 的对应关系

### 3.1 已在 `ts-api.md` 中
- `GET /sys/ts-user-voice-config/current`
- `PUT /sys/ts-user-voice-config/current`
- `GET /sys/ts-user-image-assets`
- `POST /sys/ts-user-image-assets`
- （代码中还存在）`GET /sys/ts-user-image-assets/detail`、`PUT /sys/ts-user-image-assets`、`DELETE /sys/ts-user-image-assets`

### 3.2 不在 `ts-api.md` 但建议用于本页
- `GET /sys/user/login/setting/getUserData`
- `POST /sys/user/login/setting/userEdit`
- `POST /sys/common/upload`

## 4. 建议的前端调用顺序

1. 页面加载时并行拉取：
   - `GET /sys/user/login/setting/getUserData`
   - （可选）`GET /sys/ts-user-voice-config/current`
2. 点击头像编辑时：
   - `POST /sys/common/upload` 上传图片
   - `POST /sys/user/login/setting/userEdit` 保存 `avatar`
3. 点击“保存”时：
   - `POST /sys/user/login/setting/userEdit` 保存 `realname/sex/birthday`
4. 若设置页包含音色：
   - `PUT /sys/ts-user-voice-config/current`

## 5. 待确认项（上线前）
- 性别枚举值最终约定（`sex` 的取值定义）。
- 生日字段格式（字符串格式或时间戳）。
- `userEdit` 接口权限是否对 App 端放开（当前代码包含权限注解）。
- “内容偏好”是否定义为业务标签：若是，建议新增 `TsUserPreference` 相关接口。

