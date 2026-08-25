# 变更记录（文档驱动）

## 记录格式
`[YYYY-MM-DD] [type] [module] 摘要 (PR/Issue)`

`type` 推荐值：`feat`、`fix`、`refactor`、`breaking`、`security`、`docs`

## 记录
- [2026-08-25] [perf] [ts-chat] 会话列表响应补充角色名称、头像和最后消息摘要，移除前端逐会话详情/消息请求
- [2026-08-24] [feat] [ts-story] 场景生图接口支持 `time/weather/mood` 的英文 `key + description` 结构，并注入数据库 Prompt 模板
- [2026-08-24] [feat] [ts-ad] 扩展运营内容管理为自有/外部图片、视频、卡片和深层动作，新增统一媒体预览、投放响应字段及兼容迁移
- [2026-08-24] [feat] [infra] 两套 Docker Compose 增加单节点 Kafka Broker、持久化目录、共享网络和后台连接配置，行为 Kafka 默认保持关闭
- [2026-08-23] [feat] [ts-ad-admin] 新增 Vue3 运营内容管理页面，覆盖广告位、广告内容、投放规则和曝光点击统计，并补充探拾管理员菜单
- [2026-08-23] [feat] [ts-feedback-admin] 新增 Vue3 反馈与评论统一审核页面，支持内容筛选、通过/驳回、反馈处理状态维护、官方回复及管理菜单按钮权限
- [2026-08-23] [feat] [ts-work-review-admin] 新增探拾管理端作品内容审核页面，以角色内容、角色图片、故事内容、故事图片四类视图展示审核材料，并支持详情、AI 重试、通过和驳回
- [2026-08-23] [security] [ai-safety] 抽取统一安全 Skill Prompt 提供器，并将 `ai_safety_guard` 扩展到 TS 角色/故事 Prompt Chat、JSON 修复、普通 MiniMax 聊天和图片生成入口
- [2026-08-23] [feat] [ts-behavior] 新增登录用户单条/批量推荐行为采集接口，事件异步投递 Kafka，并由独立消费者组写入 MySQL 明细和 Redis 实时特征
- [2026-08-23] [feat] [ts-ad] 新增海报与广告独立运营投放域：支持广告位、内容发布下线、平台/登录/会员/指定用户规则、匿名与登录投放、曝光点击幂等上报及汇总统计
- [2026-08-23] [security] [agent-runtime] 所有 Agent `LlmNode` 强制加载 `ai_safety_guard` Skill，并将安全规则固定置于 System Prompt 最前面；安全 Skill 缺失或为空时阻止模型调用
- [2026-08-23] [feat] [ts-feedback] 反馈、评论/回复和追加内容新增 `pending/approved/rejected` 审核状态、统一管理端审核队列与审核日志；公开查询和互动仅作用于审核通过内容，作者可查看本人审核结果
- [2026-08-23] [feat] [ts-work-review] 新增角色与故事独立作品审核域：保存后固化版本快照并执行 AI 初审、管理员终审；未通过当前版本审核的作品禁止公开
- [2026-08-23] [refactor] [ts-reward] 新增统一奖励事件中心及 Vue3 监控页面；签到和任务奖励同步处理，会员开通奖励改为支付事务提交后处理，支持事件分页、详情、状态汇总和失败重试
- [2026-08-21] [refactor] [ts-story] 故事场景图片提示词改为仅从 `airag_prompts` 按 `prompt_key + version` 读取，移除对应 classpath txt 模板
- [2026-08-21] [fix] [ts-story] 故事场景图片提示词优化模板改为 tool call，补齐场景专用规则、输出 schema 和 220/80 字段长度兜底
- [2026-08-21] [feat] [ts-points] 新增探拾后台积分管理与统一账单管理页面，补充管理员菜单迁移；支持积分账户、积分流水、充值订单、充值商品、会员赠送规则、平台账单汇总与详情
- [2026-08-20] [feat] [ts-activity] 新增签到、每日/每周任务、行为进度、统一奖励、会员奖励加成和活动奖励记录接口；星钻奖励统一调用积分服务并保证领取幂等
- [2026-08-20] [feat] [ts-story] 新增故事场景图片提示词润色接口 `/sys/ts-stories/optimize-scene-image-prompt`，使用模板 `story_scene_image_prompt_optimize::v1`
- [2026-08-19] [feat] [ts-points] 新增积分账户、积分流水、充值支付、后台调整、会员赠送积分和双视角统一账单；余额不足通过 `errorCode=POINTS_NOT_ENOUGH` 返回机器可识别错误
- [2026-08-13] [refactor] [ts-feedback] 反馈中心 `POST/PUT` 接口的 `feedbackId/commentId` 统一改为通过 JSON Body 传递，`GET` 查询接口继续使用查询参数
- [2026-08-13] [feat] [ts-feedback] 新增反馈中心五张业务表及 `/sys/ts-feedback`、`/sys/ts-comments`、`/sys/ts-admin-feedback` 接口，支持反馈发布、幂等点赞、两层评论、追加反馈、官方回复、状态管理与通知事件扩展
- [2026-08-12] [feat] [ts-browse-history] 新增角色与故事统一浏览记录表及 `/sys/ts-user-browse-history` 记录、分页、单条删除和清空接口；重复浏览累加次数并更新最近浏览时间
- [2026-08-12] [feat] [ts-favorite] 新增角色与故事统一收藏表及 `/sys/ts-user-favorites` 收藏、取消、状态和分页查询接口；所有操作按当前用户过滤，并隐藏已下架或删除资源
- [2026-08-04] [feat] [ts-member] 新增管理员支付流水分页与详情接口，并提供 `system/tanshi/payment/index` 只读管理页面；渠道原始响应在展示前二次脱敏
- [2026-08-04] [feat] [ts-image] 新增 `POST /sys/ts-images/download` 公网图片代理下载接口；限制协议、内网地址、重定向、图片类型和 30MB 文件大小，不执行图片入库或角色/故事关联
- [2026-07-30] [feat] [ts-draft] 新增角色与故事统一草稿表及 `/sys/ts-drafts` 增删改查；`draftType` 区分 `role/story`，列表返回摘要，详情返回完整 JSON 快照，所有操作按当前用户归属过滤
- [2026-07-29] [fix] [agent-runtime] 角色/故事确认选项映射为仅供模型判断的 `NONE/ACCEPTED/REVISION_REQUESTED` 隐藏状态，支持跨轮恢复并注入 Skill Prompt，不写入消息、SSE 或 Agent Event
- [2026-07-29] [fix] [agent-runtime] 角色/故事创建对话节点停止在 USER Prompt 重复注入主 Agent 初始委托；`task_description` 仅保留在 Skill/System 中，USER 只表达本轮最新输入
- [2026-07-29] [refactor] [agent-runtime] 角色形象与故事场景图片 Tool 统一为扁平媒体协议；SSE 顶层及事件 `output` 直接提供 `contentType/resourceType/imageUrl/promptCode/promptVersion`，不再嵌套 `result`
- [2026-07-29] [feat] [ts-story] 新增 `POST /sys/ts-stories/one-click-scene-image` 和 `story_generate_scene_image` Tool；使用 `story_scene_image_generate::v1` 生成临时故事背景原图，不自动入库或关联故事
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
- [2026-07-30] [feat] [ts-draft] 统一草稿列表直接返回完整 `content`，前端无需维护重复的 `cardData`
- [2026-08-23] [security] [ai-moderation] 新增统一 AI 文本输入输出审核契约、风险策略、上下文复审、输出安全重写和脱敏审核日志，并接入 Agent、公共 Prompt Chat、普通聊天及图片 Prompt
