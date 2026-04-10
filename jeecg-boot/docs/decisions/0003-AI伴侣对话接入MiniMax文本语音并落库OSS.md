# ADR 0003：AI伴侣对话接入 MiniMax 文本与语音并落库 OSS

## 状态
Proposed

## 日期
2026-03-31

## 背景
- 当前 `TS` 业务接口（`/sys/ts-chat-*`）主要提供会话、消息、附件的 CRUD，缺少“对话生成 + 语音合成 + 存储落库”的一体化接口。
- `airag` 模块已具备 MiniMax 能力：`/ai/minimax/chat|tts|image`，其中 `tts` 已支持上传到本地/MinIO/OSS 并返回 `audioUrl`。
- 数据模型已具备语音落库基础：`ts_chat_message.message_type` 支持 `voice`，`ts_chat_message_attachment` 支持 `file_type=voice`、`file_url`、`duration_sec`、`mime_type`。
- `ts_voice_profile` 当前未维护 MiniMax 音色映射字段，无法直接从业务音色配置定位到可用 `voiceId`。

## 备选方案
1. 前端分别调用 `/ai/minimax/*` 与 `/sys/ts-chat-*` 自行编排。
2. 在 `system-biz` 新增编排接口，后端统一完成文本生成、TTS、OSS 上传、消息与附件落库。
3. 直接改造 `/airag/chat` 主链路并与 `ts_*` 表强绑定。

## 决策
选择方案 2：在 `system-biz` 新增编排接口（建议：`POST /sys/ts-chat-sessions/ai-reply`，`sessionId` 放在请求体）。

建议流程：
- 校验会话归属与输入参数。
- 写入用户消息（`ts_chat_message`）。
- 调 MiniMax chat 生成 AI 文本。
- 解析用户音色配置，映射出 MiniMax `voiceId`。
- 调 MiniMax tts，上传语音到 OSS（复用现有上传配置）。
- 写入 AI 消息与语音附件（`ts_chat_message_attachment`）。
- 返回 AI 文本、音频 URL、时长与消息 ID，供前端直接渲染与播放。

## 影响
- 正向影响：
  - 前端调用链收敛，减少多次请求与编排复杂度。
  - 语音资产与消息统一归档，便于检索与审计。
  - 复用现有 MiniMax 与 OSS 能力，开发成本低。
- 负向影响：
  - `system-biz` 与 `airag` 的依赖耦合增强。
  - 需要补充音色映射字段与迁移脚本。
- 缓解措施：
  - 通过独立服务类隔离编排逻辑，避免控制器膨胀。
  - 增加接口级开关与超时/重试兜底。
  - 为新增字段提供默认值与兼容分支。

## 回滚与迁移
- 迁移：
  - 新增 `ts_voice_profile.provider_voice_id`（或 `minimax_voice_id`）字段，并补齐已有音色数据。
  - 新增接口后保持原 CRUD 接口不变，前端分阶段切换。
- 回滚：
  - 关闭新接口路由或特性开关，前端回退至旧链路。
  - 新增字段保留，不影响旧功能运行。

## 待办与风险
- 高优先级：轮换并下线明文 `MINIMAX_API_KEY`，统一改为环境变量注入。
- 中优先级：接口资产基线已拆分到 `docs/api/Index.md`（跨模块索引）以及 `docs/api/airag-api.md`、`docs/api/ts-api.md`（明细），后续需按接口变更持续维护。
- 中优先级：统一 `docs` 目录编码为 UTF-8，避免中文乱码。

## 关联信息
- 参考接口文档：`docs/api/airag-api.md`、`docs/api/ts-api.md`
- 参考实现：`SpringAiMiniMaxDemoController`、`MiniMaxDemoServiceImpl`、`MiniMaxMediaServiceImpl`

## 实施记录
### 2026-03-31（第一版落地）
- 新增会话编排接口：`POST /sys/ts-chat-sessions/ai-reply`（`sessionId` 在请求体传入）
- 新增 DTO/VO：
  - `TsChatAiReplyDto`
  - `TsChatAiReplyVo`
- 新增服务：
  - `ITsChatAiReplyService`
  - `TsChatAiReplyServiceImpl`
- 关键实现：
  - 会话归属校验后，写入用户消息并生成 AI 回复文本
  - 基于用户音色配置（或请求覆盖）解析 `voiceId`
  - 调用 MiniMax TTS，获取 `audioUrl` 并写入消息附件
  - 更新 `ts_chat_session.last_message_id/last_message_at`
- 数据与文档更新：
  - 新增迁移脚本：`db/alter_20260331_add_provider_voice_id.sql`
  - `TsVoiceProfile` 与 `TsVoiceProfileVo` 增加 `providerVoiceId`
  - `docs/api/ts-api.md` 增补 `ai-reply` 接口说明
- 验证：
  - 执行 `mvn -pl jeecg-module-system/jeecg-system-biz -am -DskipTests compile`，编译通过

### 2026-03-31（文档对齐补录）
- 按 `docs/api/Index.md` 目录规则将接口资产基线拆分到 `docs/api/Index.md`（索引）与各业务文档（`airag-api.md`、`ts-api.md`）。
- 更新 `docs/api/Index.md` 与 `docs/README.md` 的 API 文档清单，移除已删除的 `hardness-api-inventory.md` 引用。
- 在 `docs/changelog.md` 追加本次文档治理变更，确保接口文档更新有留痕可追溯。

### 2026-03-31（规范约束对齐）
- 对 `POST /sys/ts-chat-sessions/ai-reply` 相关代码按 `docs/api/README.md` 补齐中文注释：
  - `TsChatAiReplyDto`、`TsChatAiReplyVo`
  - `ITsChatAiReplyService`、`TsChatMessageMapper`
  - `TsChatSessionController`（含 `ai-reply` 在内的会话入口方法）
  - `TsVoiceProfile`、`TsVoiceProfileVo`（补齐 `providerVoiceId` 及全字段注释）
- 对 `TsChatAiReplyServiceImpl` 做结构约束收敛：仅保留 `@Override createAiReply` 业务方法，移除 private helper 并内联实现。
- 按 `docs/api/README.md` 的 ID 传参约定，将 `ai-reply` 从路径变量 `sessionId` 调整为请求体字段 `sessionId`。
- 对 `TsChatAiReplyServiceImpl` 做可维护性收敛：将发送者类型/消息类型/MIME/prompt 关键文案等隐式魔法值提取为常量并补充中文注释，保持行为不变。
- 验证：
  - 执行 `mvn -pl jeecg-module-system/jeecg-system-biz -am -DskipTests compile`，编译通过。
