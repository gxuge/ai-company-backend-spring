# 20260829-chat-message-tts-stream

## 元信息
- 任务名称：聊天消息音色快照与流式 TTS
- 分级：H2
- 负责人：Codex
- 开始时间：2026-08-29
- 关联：`POST /sys/ts-chat-sessions/message-tts`、Web Chat 语音播放

## 目标与非目标
- 目标：角色回复消息固化 `senderId` 和音色快照，播放时不重复查询角色。
- 目标：`message-tts` 通过 `stream=true` 返回 `audio/mpeg` 流，`stream=false` 保持原 JSON 响应。
- 目标：Web 前端按流读取并渐进播放 MP3。
- 非目标：不为缺少音色快照的旧消息查询角色或补写数据。
- 非目标：不修改 Agent 聊天语音能力，不新增数据库字段。

## 输入约束
- 新消息音色来源为生成回复时的活动角色。
- 流式接口继续复用现有会话归属校验和消息归属校验。
- 旧消息没有 `voiceSnapshot` 时必须明确失败，禁止查询角色表兜底。
- 保留现有非流式 `audioUrl` 返回方式。

## 任务分解
### T1
- 输入：模板回复活动角色和消息实体。
- 执行动作：写入 `senderId`，从角色 `extJson` 固化音色快照。
- 输出：新角色消息的 `contentJson.voiceSnapshot`。
- 验收标准：消息可独立确定角色及 provider 音色。
- 证据类型：代码 diff、模块编译。

### T2
- 输入：消息音色快照和 MiniMax TTS。
- 执行动作：同一接口按 `stream` 分流 JSON 与 `audio/mpeg`，流式解析 provider 音频分片。
- 输出：可直接消费的 MP3 响应流。
- 验收标准：`stream=false` 契约兼容；`stream=true` 不返回 `Result` 包装。
- 证据类型：代码 diff、模块编译。

### T3
- 输入：浏览器 Fetch、ReadableStream 和 MediaSource。
- 执行动作：API 层增加原始流响应，Chat 控制器渐进追加并播放。
- 输出：聊天消息点击后流式播放。
- 验收标准：类型检查通过；停止、切换和卸载时释放流与对象 URL。
- 证据类型：TypeScript 检查、diff check。

## 验证矩阵
| 验证项 | 方法 | 期望 | 结果证据 |
|---|---|---|---|
| 消息快照 | Java 静态检查/编译 | 新回复写 senderId 和 voiceSnapshot | Maven 输出 |
| 旧消息边界 | Java 静态检查 | 缺少快照直接失败且无角色查询 | 代码定位 |
| 非流式兼容 | Java 编译/契约检查 | 默认仍返回 Result JSON | Controller diff |
| 流式响应 | Java 编译/契约检查 | 返回 audio/mpeg 且关闭代理缓冲提示 | Controller diff |
| Web 播放 | `bun run check:types` | 无新增类型错误 | 命令输出 |
| 编码 | BOM/EOL 检查 | 保持原文件编码和换行 | 检查输出 |

## 上下文与防漏策略
- 分段执行：消息快照、后端流式、前端播放、验证四段。
- 压缩保留：接口契约、旧消息不兜底、已改文件、验证结果。
- 恢复顺序：读取本文件、`git diff`、前后端相关接口与控制器。

## 风险与回退
- 风险：浏览器不支持 MP3 MediaSource。
- 缓解：保留非流式 `audioUrl` 播放回退。
- 风险：provider 流中途失败时 HTTP 状态已发送。
- 缓解：记录调用失败并终止当前音频流，前端恢复为失败状态。
- 回退：前端改回 `stream=false`；后端保留非流式分支，不涉及数据结构回滚。

## 完成定义（DoD）
- [x] 代码改动完成
- [x] 验证矩阵执行
- [x] API 文档和变更记录同步
- [x] 未完成项列出

## 未完成项
- 未使用真实 MiniMax 密钥执行在线流式接口冒烟。
- 全仓前端 lint 受存量问题阻断；TypeScript 全量检查与新增流播放文件定向检查通过。

## 验证记录
- Maven 编译：目标模块及依赖共 8 个 Reactor 模块全部成功。
- 前端类型：`bun run check:types` 成功。
- Diff：前后端 `git diff --check` 成功。
- 边界核对：缺少 `voiceSnapshot` 的消息直接抛出业务错误，未调用角色 Mapper。
- 编码：既有源文件保持原 BOM/EOL，`ts-api.md` 保持 UTF-8 BOM/CRLF。
