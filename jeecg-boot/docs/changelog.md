# 变更记录（文档驱动）

## 记录格式
`[YYYY-MM-DD] [type] [module] 摘要 (PR/Issue)`

`type` 推荐值：`feat`、`fix`、`refactor`、`breaking`、`security`、`docs`

## 记录
- [2026-07-17] [refactor] [agent-runtime] 角色确认拆分为展示型 `role_request_confirmation` 与推进型 `role_continue_generation`；按钮文案按普通用户输入处理，只有后者携带四项角色数据并进入形象、声音生成
- [2026-07-17] [refactor] [agent-runtime] 角色/故事确认工具移除 `summary` 入参和 SSE 字段，确认型 `tool.end` 仅保留问题、选项与交互控制字段
- [2026-07-14] [feat] [agent-runtime] Agent 会话新增活动节点、阶段和白名单流程快照；角色/故事子 Agent 可跨消息从具体节点续跑，完成后显式 Handoff 回主 Agent
- [2026-07-14] [feat] [agent-chat] Agent 事件新增 `node_name/node_type`，正式助手消息新增 `source_node_name/source_event_id`，可定位子 Agent 最终产出节点并关联完整 Task 事件
- [2026-07-14] [refactor] [agent-chat] 新增 `ts_agent_chat_message_event` 独立保存 Agent SubAgent/Tool 完整事件，新增事件分页/详情接口，并将 Agent Session 与 Message Controller 拆分且保持原路由兼容
- [2026-07-14] [feat] [ts-role/ts-story] `role_core_fill` 与 `story_core_fill` 普通生成接口新增可选 `extraInfo`（兼容 `extra_info`），空值按 `null` 注入 Prompt；故事 Agent 普通生成工具同步声明 inputSchema 并透传
- [2026-07-14] [refactor] [agent-runtime] `ts_chat_message_events` 收敛为 SubAgent Task 与 Tool 完整事件：仅在结束节点各保存一条包含输入、结果、错误和耗时的记录，停止持久化 LLM 与 Agent 控制事件
- [2026-07-14] [refactor] [agent-runtime] 新增顶层 Agent Handoff 运行循环与会话 `active_agent_code` 持久化；主 Agent `task` 改为控制权切换，子 Agent 可跨消息持续接管并显式交还主 Agent
- [2026-07-10] [feat] [ts-role] 新增 `POST /sys/ts-roles/optimize-image-prompt` 角色形象提示词优化接口，读取数据库模板 `role_image_prompt_optimize::v1`，输出中文优化提示词与负面提示词，便于前端直接回填
- [2026-04-08] [docs] [api] 将 `docs/api/hardness-api-inventory.md` 拆分并并入 `docs/api/Index.md` + 业务明细文档，移除重复资产清单文件并清理引用
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

- [2026-04-02] [feat] [ts-role] 新增角色一键生成接口（setting/image/voice）与通用 Prompt 模板（role_core_fill/role_image_generate/role_voice_generate），并同步更新 ts-api 与 hardness 清单
- [2026-04-02] [refactor] [ts-role] 按 `docs/api/README.md` 规范瘦身 `TsRoleServiceImpl`，新增 `ITsRoleOneClickGenerateService` + `TsRoleOneClickGenerateServiceImpl`，并简化四核心字段变量透传逻辑，接口行为保持兼容
- [2026-04-02] [refactor] [ts-role] `oneClickGenerateSetting` 改为仅做模板变量代入与模型调用，移除四核心字段后端判空/保留/兜底逻辑，空值统一透传为 `null` 由 Prompt 决策
- [2026-04-02] [refactor] [ts-role] 一键生成服务提取 `PromptRuntimeUtil`/`RoleGenerateSnapshotUtil`/`VoiceProfileMatchUtil` 三个工具类（含中文注释）；图片地址提取逻辑改为在业务方法内联
- [2026-04-02] [refactor] [ts-role] 继续收敛 `TsRoleOneClickGenerateServiceImpl`：移除全部业务私有 helper，仅保留三个业务入口方法；其余能力统一下沉到 utils
- [2026-04-02] [feat] [ts-role] 新增 `POST /sys/ts-roles/generate-role` 随机完整角色编排接口（设定+形象+声音），`storySetting/storyBackground` 改为非必填，并新增模板 `role_generate_role_v1`
- [2026-04-14] [feat] [ts-voice] 新增“我的音色库”接口：`GET /sys/ts-user-voice-profiles`、`PUT /sys/ts-user-voice-profiles/{id}`、`DELETE /sys/ts-user-voice-profiles/{id}`，并同步 `docs/api/ts-api.md` 与 `db/ai-company.sql`
- [2026-05-25] [refactor] [airag-llm] 新增 `llm.adapter` 参数适配层（capability/normalizer/registry/adapter），并接入 `AIChatHandler` 的 `completions/chat` 调用链，优先支持 `DEEPSEEK/MINIMAX/GEMINI` 的参数裁剪与映射
- [2026-06-01] [feat] [ts-story] 新增 `POST /sys/ts-stories/story-full-generate`：随机选取 story 预设并读取绑定标签，先做 `{{ value }}` 模板替换与 toolcall 预编排，再串联现有故事设定/场景/大纲生成（chapter 模式支持跳过大纲）
- [2026-06-01] [feat] [ts-story] 新增 `POST /sys/ts-stories/story-full-generate-preset`：按预设绑定标签映射填充 5 字段模板并统一串联设定/场景/大纲；删除对外 `story-setting-generate`/`story--scene-generate`/`story--outline-generate`；toolcall 修复链路补充 `required_field_hints`
