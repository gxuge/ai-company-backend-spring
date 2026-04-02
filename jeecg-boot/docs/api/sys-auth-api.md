# 系统鉴权 API（`/sys`）

## 1. 范围
- 控制器：`jeecg-module-system/jeecg-system-biz/.../LoginController.java`
- 基础路径：`/sys`
- 完整访问前缀（默认）：`/jeecg-boot/sys`

## 2. 核心接口清单

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/login` | 账号密码登录 |
| GET | `/user/getUserInfo` | 获取当前登录用户信息 |
| * | `/logout` | 退出登录 |
| GET | `/loginfo` | 首页统计信息 |
| GET | `/visitInfo` | 访问数据统计 |
| PUT/POST | `/selectDepart` | 选择部门并切换会话信息 |
| POST | `/sms` | 发送短信验证码 |
| POST | `/phoneLogin` | 手机号验证码登录 |
| GET | `/getEncryptedString` | 获取加密参数 |
| GET | `/randomImage/{key}` | 获取图形验证码 |
| POST | `/mLogin` | 多租户/多部门登录流程 |
| POST | `/checkCaptcha` | 校验图形验证码 |
| GET | `/getLoginQrcode` | 获取扫码登录二维码 |
| POST | `/scanLoginQrcode` | 扫码登录确认 |
| GET | `/getQrcodeToken` | 查询二维码登录结果 |
| POST | `/sendChangePwdSms` | 发送修改密码短信验证码 |
| POST | `/smsCheckCaptcha` | 校验短信验证码 |
| POST | `/loginGetUserDeparts` | 登录后查询用户部门 |

## 3. 参数与返回约定
- 统一返回：`Result<T>`。
- 登录类接口常用请求体：`SysLoginModel` 或 `JSONObject`。
- 鉴权相关异常统一通过结果码返回，不建议前端依赖异常堆栈文本。

## 4. 安全注意事项
- 验证码校验失败要有频控策略（防暴力尝试）。
- 登录接口变更必须做回归测试：密码登录、短信登录、扫码登录。
- 任何涉及 token 或 session 行为变化，必须同步更新前端适配说明。

## 5. 维护说明
- 新增或修改 `LoginController` 映射后，需同步更新本文件和 `docs/changelog.md`。
