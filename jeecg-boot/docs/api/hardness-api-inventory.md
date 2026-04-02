# Hardness API Inventory（接口资产扫描基线）

## 1. 文档定位
- 用途：沉淀当前项目 API 资产基线，支撑跨模块需求的范围确认、改动核对与回归排查。
- 更新时间：2026-03-31
- 关联 ADR：`docs/decisions/0003-AI伴侣对话接入MiniMax文本语音并落库OSS.md`

## 2. AI 伴侣对话链路（本次重点）

### 2.1 编排接口（`system-biz`）
| 方法 | 路径 | 控制器 | 说明 |
|---|---|---|---|
| POST | `/sys/ts-chat-sessions/ai-reply` | `TsChatSessionController` | 在会话内完成“文本生成 + 语音合成 + 附件落库”的一体化编排（`sessionId` 在请求体传入） |

### 2.2 底层能力接口（`airag`）
| 方法 | 路径 | 控制器 | 说明 |
|---|---|---|---|
| POST | `/ai/minimax/chat` | `SpringAiMiniMaxDemoController` | MiniMax 文本生成能力 |
| POST | `/ai/minimax/tts` | `SpringAiMiniMaxDemoController` | MiniMax 文本转语音能力（支持上传并返回 `audioUrl`） |
| POST | `/ai/minimax/image` | `SpringAiMiniMaxDemoController` | MiniMax 文生图能力 |
| GET | `/airag/prompts/template/query` | `AiragPromptsController` | 从 classpath 加载 AI companion 固定模板（按 `code+version`） |

### 2.3 链路配套接口（TS 业务域）
| 方法 | 路径 | 控制器 | 说明 |
|---|---|---|---|
| GET | `/sys/ts-user-voice-config/current` | `TsUserVoiceConfigController` | 获取当前用户音色配置 |
| PUT | `/sys/ts-user-voice-config/current` | `TsUserVoiceConfigController` | 保存当前用户音色配置 |
| GET | `/sys/ts-voice-profiles` | `TsVoiceProfileController` | 查询音色档案（含提供商音色映射字段） |
| GET | `/sys/ts-chat-messages` | `TsChatMessageController` | 查询会话消息列表（含文本/语音消息） |
| GET | `/sys/ts-chat-message-attachments` | `TsChatMessageAttachmentController` | 查询语音附件（`file_url`、`duration_sec` 等） |

## 3. 资产归档与文档映射
- 认证接口清单：`docs/api/sys-auth-api.md`
- AIRAG 能力清单：`docs/api/airag-api.md`
- TS 业务清单：`docs/api/ts-api.md`
- 架构决策记录：`docs/decisions/0003-AI伴侣对话接入MiniMax文本语音并落库OSS.md`

## 4. 维护规则
- 新增或调整 Controller 映射后，必须同步更新对应 `docs/api/*.md` 与 `docs/changelog.md`。
- 影响跨模块调用链（如 `system-biz` 编排 `airag` 能力）时，需同步补充/更新 ADR。
- 本文档用于“范围核对与回归基线”，不替代各业务域接口明细文档。
