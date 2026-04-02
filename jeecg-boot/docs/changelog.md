# 变更记录（文档驱动）

## 记录格式
`[YYYY-MM-DD] [type] [module] 摘要 (PR/Issue)`

`type` 推荐值：`feat`、`fix`、`refactor`、`breaking`、`security`、`docs`

## 记录
- [2026-03-30] [docs] [governance] 初始化文档骨架（AGENTS/PLANS/API/ADR）
- [2026-03-30] [docs] [architecture] 文档升级为项目上下文化版本（模块/启动入口/Profile/配置策略）
- [2026-03-30] [docs] [api] 新增 `sys-auth-api.md` 与 `airag-api.md`
- [2026-03-30] [docs] [config] 新增 `configuration.md`，补充环境配置与敏感信息规范
- [2026-03-30] [docs] [api] 新增 `hardness-api-inventory.md`，沉淀全项目接口扫描基线
- [2026-03-30] [docs] [api] 新增 `ts-api.md`，拆分 `Ts*Controller` 接口清单
- [2026-03-31] [docs] [api] 更新 `docs/api/README.md`，补充 `hardness-api-inventory.md` 命名规则与文档落地清单
- [2026-03-31] [docs] [api] 补齐 `docs/api/hardness-api-inventory.md`，沉淀 AI 伴侣对话链路接口资产基线
- [2026-03-31] [docs] [decision] 更新 ADR `0003`，记录本次 API 文档对齐动作
- [2026-03-31] [feat] [ts-chat] `POST /sys/ts-chat-sessions/ai-reply` 改为请求体传 `sessionId`（移除路径变量），对齐 API 传参规范
- [2026-03-31] [feat] [airag-prompts] 新增 classpath prompt 模板落地与查询接口 `GET /airag/prompts/template/query`（支持 `code+version`）
- [2026-04-01] [refactor] [airag/minimax] MiniMax 接口迁移至 `jeecg-system-biz/openapi`，并将 `MINIMAX_*` / `AIRAG_MINIMAX_*` 配置迁移到 `jeecg-system-start`。
- [2026-04-01] [refactor] [airag/minimax] 补充迁移 MiniMax service/dto/vo/config 到 `jeecg-system-biz/openapi` 现有目录，并迁移 `resources/prompts` 到 `jeecg-system-biz`。
