# API 文档目录索引

## 说明
- 本文件仅维护 API 文档目录，不承载接口开发规范。
- Spring Boot 接口开发规范请使用 `docs/spring-boot-dev.skill`。

## 文档目录
- `sys-auth-api.md`：系统登录鉴权接口
- `airag-api.md`：AIRAG 业务接口
- `ts-api.md`：TS 业务接口

## 跨模块链路索引（由 inventory 文档拆分）
- AI 伴侣对话编排入口：见 `ts-api.md` 中 `POST /sys/ts-chat-sessions/ai-reply`
- MiniMax 底层能力接口：见 `airag-api.md` 中 `/ai/minimax/chat`、`/ai/minimax/tts`、`/ai/minimax/image`
- Prompt 模板查询接口：见 `airag-api.md` 中 `GET /airag/prompts/template/query`
- 配套业务接口（音色配置/角色一键生成/消息附件）：见 `ts-api.md` 对应章节

## 维护规则
- 新增或调整 Controller 映射后，必须同步更新对应 `docs/api/*.md` 与 `docs/changelog.md`。
- 影响跨模块调用链（如 `system-biz` 编排 `airag` 能力）时，需同步补充/更新 ADR。
