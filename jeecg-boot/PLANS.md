# PLANS.md

## 使用说明
用于记录中大型任务的执行过程，强调“先记录、再实现、持续更新”。

建议在以下场景使用：
- 跨多个模块的改动
- 涉及鉴权、配置、数据结构、外部集成的改动
- 需要长期追踪的优化任务

---

## 模板

### 任务 ID
`YYYYMMDD-<短名称>`

### 背景
- 业务背景：
- 现状与约束：
- 关联模块/文件：

### 目标
- 主要目标：
- 验收标准：

### 范围
- 范围内：
- 范围外：

### 执行步骤
1. 现状分析（代码/配置/接口）。
2. 最小可行改动。
3. 验证（编译、测试、手工回归）。
4. 文档与变更记录同步。

### 进度
- [ ] 步骤 1：
- [ ] 步骤 2：
- [ ] 步骤 3：
- [ ] 步骤 4：

### 决策记录
- 决策：
- 备选方案：
- 选择原因：
- 影响面：

### 风险与回滚
- 风险：
- 监控/告警信号：
- 回滚步骤：

### 验证记录
- 构建命令：
- 测试命令：
- 手工验证：

### 结果
- 结果摘要：
- 后续事项：

---

## 当前任务记录

### 任务 ID
`20260714-core-fill-extra-info`

### 背景
- `role_core_fill` 与 `story_core_fill` Prompt 已增加 `{{extra_info}}`。
- 现有角色、故事普通核心生成接口尚未接收和传递该变量。

### 目标
- 两个普通核心生成接口增加可选 `extraInfo`，并兼容 `extra_info`。
- 未传或空白时向模板传字面量 `null`。
- 故事 Agent 普通生成工具同步声明并透传用户补充信息。

### 范围
- 范围内：角色/故事 DTO、Prompt 变量构建、故事 Agent 工具、测试与 API 文档。
- 范围外：响应 VO、数据库、`role_core_fill_preset`、`story_core_fill_preset`。

### 执行步骤
1. 增加请求字段、别名与归一化。
2. 接入两个普通模板变量。
3. 同步故事 Agent 工具与文档。
4. 执行定向测试和模块编译。

### 进度
- [x] 步骤 1：请求字段、别名与归一化
- [x] 步骤 2：普通模板变量映射
- [x] 步骤 3：故事 Agent 工具 inputSchema、透传与文档
- [x] 步骤 4：主代码编译完成；定向测试阻塞原因已记录

### 风险与回滚
- 风险：共享 DTO 使 preset 接口也可接收字段，但 preset 模板不会消费。
- 回滚步骤：移除两个 DTO 字段、变量映射、Agent 参数和对应文档。

### 验证记录
- 编译命令：`mvn -pl jeecg-module-system/jeecg-system-biz -am -DskipTests compile`。
- 编译结果：8 个 Reactor 模块 `BUILD SUCCESS`。
- 测试命令：`mvn -pl jeecg-module-system/jeecg-system-biz -am "-DskipTests=false" "-Dtest=CoreFillExtraInfoTest" test`。
- 测试结果：未进入本次测试；被仓库既有 `AiragPromptTemplateServiceTest` 类型不匹配阻塞。单模块隔离执行又受并行 Agent Event 源码与本地 AIRAG 依赖状态不一致影响。

### 结果
- 两个普通核心生成接口已支持可选额外信息并映射到 `extra_info`。
- 新增测试覆盖别名、空值和两个模板变量映射，待仓库既有测试/并行源码状态恢复后执行。

### 任务 ID
`20260714-agent-handoff-active-agent`

### 背景
- 当前 Agent 链路已支持主 Agent 通过 `task` 调用子 Agent，以及子 Agent 通过 `handoff_to_main` 返回控制信息。
- 每次用户消息仍固定从主 Agent 启动，子 Agent 的 `WAITING_USER` 状态无法跨消息保持当前控制者。
- 当前 `task` 在工具服务内部嵌套执行子 Agent，不具备同一顶层 Run 内持续切换 active Agent 的运行循环。

### 目标
- 新增顶层 Agent Run 循环，在同一 Run 内处理 `Main -> SubAgent -> Main` 控制权切换。
- 在 `ts_agent_chat_session` 持久化 `active_agent_code`，下一条用户消息从最后运行的 Agent 继续。
- 保持现有消息、事件、Skill、Tool 和 SSE 对外契约兼容。

### 范围
- 范围内：
  - `jeecg-boot-module-airag/agent` 运行时、task 工具、handoff 控制协议。
  - `jeecg-system-biz` Agent 会话、消息和回复编排。
  - 数据库迁移、ADR、架构说明、变更记录和自动化测试。
- 范围外：
  - 前端 UI 与接口路径调整。
  - OpenAI Agents SDK 的直接依赖引入。
  - 与 Agent 会话无关的业务模块重构。

### 执行步骤
1. 新增 Agent 注册表、上下文准备器、Run step/outcome 和顶层循环。
2. 将主 Agent `task` 从嵌套执行改为通用 Handoff 结果。
3. 将子 Agent `handoff_to_main` 接入同一 Run 循环。
4. 新增并持久化 `active_agent_code`，消息记录最后实际回复 Agent。
5. 补充迁移脚本、ADR、架构文档、测试和编译验证。

### 进度
- [x] 步骤 1 前置：完成现状、约束、编码和数据库迁移规范检查。
- [x] 步骤 1：实现顶层 Run 循环。
- [x] 步骤 2：改造主 Agent task。
- [x] 步骤 3：改造反向 Handoff。
- [x] 步骤 4：接入跨消息 active Agent。
- [x] 步骤 5：完成文档、测试和验证。

### 决策记录
- 决策：保留 `AgentRuntimeService.execute` 作为单 Agent step 执行器，新增 `AgentRunLoopService` 负责顶层控制权切换。
- 决策：新增 `active_agent_code`，不复用表示会话主 Agent 配置的 `agent_code`。
- 决策：`WAITING_USER` 和 `SUCCESS` 均保留最后运行 Agent，只有显式 Handoff 才切换控制者。
- 决策：单次顶层 Run 最多执行 8 个 Agent step，达到上限时保留最后实际执行的 Agent。
- 决策：子 Agent 筛选后的历史 JSON 数组可直接恢复为提示词历史块。
- 备选方案：
  - 仅在业务 Service 中按状态直调子 Agent，无法覆盖同一 Run 内反向 Handoff。
  - 复用 `state_json` 保存 active Agent，查询和约束能力较弱且语义不清晰。

### 风险与回滚
- 风险：同一会话并发发送消息时，active Agent 采用最后完成写入策略。
- 风险：历史会话的 active Agent 为空或对应 Agent 已下线。
- 监控/告警信号：
  - Handoff 超过最大次数。
  - active Agent 编码无法从注册表解析。
  - 子 Agent 恢复后缺少定义、Skill 或历史上下文。
- 回滚步骤：
  - 回复编排恢复固定调用 `TsAgentChatAgent`。
  - 回退运行循环和 Handoff 目标字段。
  - 数据库字段可保留不使用；如需删除，执行 `ALTER TABLE ts_agent_chat_session DROP COLUMN active_agent_updated_at, DROP COLUMN active_agent_code;`。

### 验证记录
- 构建命令：`mvn -f pom.xml -pl jeecg-module-system/jeecg-system-biz -am -DskipTests compile`，执行成功。
- 测试命令：定向执行 `AgentRunLoopServiceTest`、`AgentContextPreparerTest`、`DeepAgentTaskToolServiceTest`，共 8 条测试全部通过；测试后已恢复父 POM 默认跳过测试配置。
- 手工验证：
  - 主 Agent Handoff 到子 Agent 后，`WAITING_USER` 保留子 Agent。
  - 下一轮可直接从持久化子 Agent 启动。
  - 子 Agent 可在同一 Run 内 Handoff 回主 Agent。
  - 非法目标和 8 step 循环上限返回失败，并记录最后实际执行 Agent。
  - 子 Agent 历史 JSON 数组可恢复为提示词历史块。

### 结果
- 已完成顶层 Handoff 运行循环、active Agent 跨消息持久化、实际回复 Agent 消息标记、数据库迁移、ADR、架构文档和自动化测试。
- 部署前需执行 `db/V3.9.1_26__add_agent_active_handoff_state.sql`。

### 任务 ID
`20260330-doc-context-v1`

### 背景
- 现有文档仅有通用模板，缺少与本仓库实际模块、接口、配置的绑定。
- 需要将文档统一改成中文，支持后续“上下文记录 + 策略记录 + 决策记录”。

### 目标
- 建立项目化文档体系（中文）。
- 覆盖模块架构、配置策略、核心 API、变更日志、ADR 模板。

### 范围
- 范围内：`AGENTS.md`、`docs/*`、本文件。
- 范围外：业务代码逻辑改动。

### 执行步骤
1. 采集真实上下文（模块、启动类、配置文件、核心 Controller）。
2. 重写文档为中文项目版。
3. 新增 API 与配置文档样例。
4. 更新变更记录与索引。

### 进度
- [x] 步骤 1：已完成（基于 `pom.xml`、启动类、`application*`、`LoginController`、`airag` 控制器）。
- [x] 步骤 2：已完成。
- [x] 步骤 3：已完成。
- [x] 步骤 4：已完成。

### 决策记录
- 决策：采用“轻量固定骨架 + 按模块增量补充”的文档策略。
- 备选方案：
  - 一次性补齐全量接口文档（成本高、容易过期）。
  - 仅保留模板不落地（价值低）。
- 选择原因：可快速落地且维护成本可控。

### 风险与回滚
- 风险：文档落地后无人持续更新导致失真。
- 监控/告警信号：接口变更但 `docs/api` 无对应更新。
- 回滚步骤：回退到上一版文档并按模块逐步补录。

### 验证记录
- 构建命令：本次未执行（仅文档变更）。
- 测试命令：本次未执行（仅文档变更）。
- 手工验证：已检查新增/修改文档路径与内容一致性。

### 结果
- 已完成项目化文档 v1 的基础搭建。
- 后续建议：按每次业务迭代补齐对应模块 API 明细与 ADR。

### 任务 ID
`20260401-airag-minimax-migration`

### 背景
- 仅迁移 MiniMax 相关接口与配置，避免影响其他 AIRAG 接口。

### 目标
- 将 MiniMax API 控制器迁移到 `jeecg-system-biz/openapi`。
- 将 MiniMax 运行配置迁移到 `jeecg-system-start`。

### 范围
- 范围内：`SpringAiMiniMaxDemoController`、`application-dev/prod.{yml,properties}`、文档同步。
- 范围外：其他 `airag` 业务接口与数据结构。

### 执行步骤
1. 迁移 Controller 到 `org.jeecg.modules.openapi.controller` 并保持路由不变。
2. 将 `spring.ai.minimax.*` 与 `jeecg.airag.minimax.*` 转移到 `system-start` 配置。
3. 清理 `airag` 模块中的 MiniMax 配置段，避免分散。
4. 更新 `docs/api`、`docs/config`、`docs/changelog`。

### 进度
- [x] 步骤 1
- [x] 步骤 2
- [x] 步骤 3
- [x] 步骤 4

### 任务 ID
`20260804-ts-image-download`

### 目标与边界
- 新增 `POST /sys/ts-images/download`，代理下载公网图片并以附件流返回。
- 下载不入库，不关联角色、故事或用户图片资产。
- 限制协议、内网地址、响应类型、重定向次数和最大文件大小。

### 执行步骤
- [x] 新增下载 DTO、Service 与 Controller 映射。
- [x] 同步 TS API 文档与变更记录。
- [x] 完成 system-biz 编译与差异检查。

### 风险与回滚
- 风险：第三方图片服务响应慢、返回非图片内容或重定向到内网地址。
- 缓解：连接/读取超时、逐跳地址校验、`image/*` 类型校验和 30MB 上限。
- 回滚：移除下载 DTO、Service 和 Controller 方法；不涉及数据库或存量数据。

### 验证记录
- 联动编译：`mvn -pl jeecg-module-system/jeecg-system-biz -am -DskipTests compile`，结果 `BUILD SUCCESS`。
- 定向测试源码：`TsImageServiceImplTest` 编译通过，覆盖内网地址与非 HTTP/HTTPS 协议拒绝。
- 测试限制：根 POM 将 Surefire `skipTests` 固定为 `true`，测试用例未实际执行；不影响主代码编译。
- 差异检查：本次后端文件 `git diff --check` 通过。

### 验证记录
- 编译验证：已执行 `mvn -pl jeecg-module-system/jeecg-system-start -am -DskipTests compile`，结果 `BUILD SUCCESS`。

### 结果
- MiniMax 接口已迁移到 `openapi` 包路径。
- MiniMax 配置已在 `system-start` 模块集中维护。
- MiniMax 下游 service/dto/vo/config 已迁移到 `openapi` 现有目录（config/dto/vo/service/impl）。
- `prompts` 资源目录已迁移到 `jeecg-system-biz/src/main/resources/prompts`。
## 任务 ID
`20260414-sound-edit-user-voice-profiles-api`

### 背景
- `pages/sound-edit` 的“我的音色库”在前端仍是降级提示，缺少真实后端接口（列表/重命名/删除）。
- 需要补齐接口能力并给出可联调的文档与验证证据。

### 目标
- 新增 `GET /sys/ts-user-voice-profiles`
- 新增 `PUT /sys/ts-user-voice-profiles/{id}`
- 新增 `DELETE /sys/ts-user-voice-profiles/{id}`
- 同步 `docs/api/ts-api.md` 与 SQL 基线。

### 范围
- 范围内：`TsVoiceProfileController`、`ITsVoiceProfileService`、`TsVoiceProfileServiceImpl`、`TsUserVoiceProfile*`（Entity/Mapper/PO/DTO）、`db/ai-company.sql`、`docs/api/ts-api.md`
- 范围外：前端 UI 布局改动、无关模块重构。

### 执行步骤
1. 落地 `ts_user_voice_profile` 表结构及用户归属字段。
2. 新增我的音色查询/重命名/删除接口，并统一做登录用户归属校验。
3. 保持 `TsUserVoiceConfigServiceImpl` 默认音色初始化兼容逻辑。
4. 更新 API 文档并完成编译验收。

### 进度
- [x] 步骤 1
- [x] 步骤 2
- [x] 步骤 3
- [x] 步骤 4

### 决策记录
- 决策：将“我的音色库”独立为 `ts_user_voice_profile`，避免与公共推荐音色耦合。
- 备选方案：
  - 在 `ts_voice_profile` 上直接增加用户字段（会污染公共库语义）。
  - 继续前端降级提示（无法满足重命名/删除真实能力）。
- 选择原因：用户隔离清晰、扩展性更好、与现有音色配置链路兼容。

### 风险与回滚
- 风险：历史用户首次进入时可能出现个人音色为空。
- 监控/告警信号：`GET /sys/ts-user-voice-profiles` 空列表比例异常。
- 回滚步骤：回退本次接口与表变更，前端保持降级提示模式。

### 验证记录
- 编译验证：`mvn -f D:\project_demo\ai-company-backend-spring\jeecg-boot\pom.xml -pl jeecg-module-system/jeecg-system-biz -am -DskipTests compile`
- 结果：`BUILD SUCCESS`
- 手工验证：接口路径、入参、权限校验与文档字段对齐。

### 结果
- 已完成 `pages/sound-edit` 对接所需后端接口基础能力，可进入前端真实联调。

### 任务 ID
`20260525-airag-llm-provider-adapter-v1`

### 背景
- 现有 `AIChatHandler` 在多供应商场景下存在参数处理分散、供应商差异硬编码、思考参数控制不统一的问题。
- 需要在 `jeecg-boot-module-airag` 内构建可扩展的参数适配层，优先支持 `DEEPSEEK / MINIMAX / GEMINI`。

### 目标
- 在 `org.jeecg.modules.airag.llm` 下新增可配置化的 provider 参数适配骨架（capability + normalizer + adapter + registry）。
- 将 `AIChatHandler` 接入该适配层，实现调用前统一裁剪、映射与 warning 留痕。
- 优先完成 `deepseek / minimax / gemini` 三类 provider 的可运行适配。

### 范围
- 范围内：
  - `jeecg-boot-module-airag/src/main/java/org/jeecg/modules/airag/llm/**`
  - 适配层新增类与 `AIChatHandler` 接入改造
- 范围外：
  - 前端改造
  - 非 airag 模块接口契约调整

### 执行步骤
1. 设计并落地适配层核心类型（capability、context、adapter、registry、normalizer）。
2. 实现 `deepseek / minimax / gemini` provider 规则。
3. 接入 `AIChatHandler`（completions/chat/image 链路）并保留兼容能力。
4. 编译验证并记录风险、后续优化点。

### 进度
- [x] 步骤 1
- [x] 步骤 2
- [x] 步骤 3
- [x] 步骤 4

### 风险与回滚
- 风险：
  - 适配层引入后可能影响现有模型默认行为（如采样参数、thinking 开关）。
  - 依赖 `LLMHandler` 缓存机制的模型注入逻辑存在版本耦合风险。
- 监控/告警信号：
  - 模型测试激活失败率异常升高。
  - DeepSeek 二轮调用出现 `reasoning_content must be passed back` 错误回升。
- 回滚步骤：
  - 回退 `AIChatHandler` 对适配层的接入提交。
  - 保留数据库模型配置不变，恢复旧调用链。

### 验证记录
- 编译验证：`D:\maven\bin\mvn -pl jeecg-boot-module/jeecg-boot-module-airag -am -DskipTests compile`
- 结果：`BUILD SUCCESS`
- 手工验证：
  - `DEEPSEEK` 适配器会注入 `thinking.type` 自定义参数，并通过缓存键同时覆盖非流式与流式模型。
  - `MINIMAX/GEMINI` 文本模型调用统一归一为 `OPENAI-compatible` provider 与 baseUrl 规范化。
  - 能力不支持的参数（如 `temperature/topP/presencePenalty/frequencyPenalty/tools/search`）会按 capability 自动裁剪并记录 warning 日志。

### 结果
- 已在 `llm.adapter` 目录落地 provider 参数适配骨架，包含 capability/normalizer/adapter/registry/service。
- 已接入 `AIChatHandler` 的 `completions/chat` 主链路，实现调用前统一适配与 warning 留痕。
- 已优先实现 `deepseek/minimax/gemini` 三类 provider 规则，后续新增 provider 仅需新增 adapter。

### 任务 ID
`20260714-agent-message-event-controller-split`

### 背景
- Agent Runtime 当前把 Task/Tool 事件写入通用表 `ts_chat_message_events`，与 `ts_agent_chat_message` 的业务边界不一致。
- `TsAgentChatSessionController` 同时承载会话、消息与回复接口，职责需要拆分。
- 前端需要按当前登录用户查询 Agent 消息对应的完整 Task/Tool 事件。

### 目标
- 新增 `ts_agent_chat_message_event` 表及 AIRAG Entity/Mapper/Service，Agent Runtime 只写入新表。
- 新增独立 `TsAgentChatMessageEventController`，提供事件分页与详情接口，并强制用户归属过滤。
- 将 `TsAgentChatSessionController` 拆为 Session 与 Message Controller，保持已有路由不变。

### 范围
- 范围内：Agent 事件实体、Mapper/XML、Service、Publisher、数据库迁移、三个 Controller、查询 DTO、测试与 API 文档。
- 范围外：旧 `ts_chat_message_events` 历史数据迁移、前端改造、SSE 事件格式和正式消息落库时序。

### 执行步骤
1. 新增 Agent 消息事件表、持久化模型和带用户归属过滤的查询能力。
2. 将 AIRAG 事件发布器切换到新事件 Service，并更新定向测试。
3. 拆分 Session/Message Controller，新增 Event Controller，保持旧消息接口路径兼容。
4. 更新 API、ADR、变更记录并执行跨模块编译与定向测试。

### 进度
- [x] 步骤 1：依赖方向、现有表结构与 Controller 边界分析
- [x] 步骤 2：事件表与 AIRAG 持久化实现
- [x] 步骤 3：Controller 拆分与事件查询接口
- [x] 步骤 4：测试、编译与文档证据回写

### 决策记录
- 决策：事件 Entity/Mapper/Service 放在 AIRAG，system-biz Controller 依赖 AIRAG 查询服务。
- 决策：`message_id` 关联触发当前 Run 的用户消息；助手消息在 Run 结束后才落库，不作为事件归属主键。
- 决策：旧表保留历史数据，不自动复制到新表，避免重复事件和迁移锁表风险。

### 风险与回滚
- 风险：部署代码早于迁移脚本时，新事件写入会失败。
- 风险：调用方若按助手消息 ID 查询事件将得到空结果，需要遵循“触发消息 ID”语义。
- 监控信号：`ts_agent_chat_message_event` 插入异常、事件分页接口空结果比例异常。
- 回滚步骤：回退 Publisher/Controller/Service 改动，恢复旧 `TsChatMessageEventService`；新表可保留或执行 `DROP TABLE ts_agent_chat_message_event`。

### 验证记录
- AIRAG 编译：`mvn -pl jeecg-boot-module/jeecg-boot-module-airag -am -DskipTests compile`，结果 `BUILD SUCCESS`。
- system-biz 编译：`mvn -pl jeecg-module-system/jeecg-system-biz -am -DskipTests compile`，结果 `BUILD SUCCESS`。
- 定向测试：通过 JUnit Platform Launcher 执行 `AgentEventPublisherTest` 与 `TsAgentChatMessageEventServiceTest`，共 6 条测试，失败数 0。
- 测试限制：AIRAG 标准 Maven `test` 仍被仓库既有 `AiragPromptTemplateServiceTest.java:77` 类型错误阻塞，与本次改动无关。

### 结果
- Agent Runtime 的 SubAgent/Tool 完整事件已切换到 `ts_agent_chat_message_event`。
- Session、Message、Event Controller 已拆分，原 Session/Message 路由保持兼容。
- 新事件分页与详情接口按当前登录用户、会话和消息归属过滤。

### 任务 ID
`20260730-unified-draft-crud`

### 背景
- 角色草稿与故事草稿当前依赖正式业务表的状态字段，草稿保存会污染正式角色、故事数据。
- 草稿箱只需要按类型展示摘要并恢复完整页面快照，角色与故事可以共用统一资源。

### 目标
- 新增统一草稿表 `ts_draft`，通过 `draft_type=role/story` 区分业务类型。
- 新增 `/sys/ts-drafts` 标准增删改查，列表和详情均返回完整 `content` 对象。
- 所有接口必须按当前登录用户进行归属过滤，删除采用软删除。

### 范围
- 范围内：Entity、DTO/PO/VO、Mapper/XML、Service、Controller、归属 AOP、数据库迁移、测试和文档。
- 范围外：本轮不修改角色/故事正式保存接口，不接入前端页面，不实现草稿发布为正式数据的聚合接口。

### 执行步骤
1. 新增统一草稿模型、数据库表和类型约束。
2. 实现分页、详情、新增、编辑、删除以及用户归属校验。
3. 补充 JSON 快照转换测试、Mapper XML 校验和模块编译。
4. 更新 API 文档、ADR、变更记录和验证证据。

### 进度
- [x] 步骤 1：规则、现有 CRUD 分层和数据库风格分析
- [x] 步骤 2：草稿模型与 CRUD 实现
- [x] 步骤 3：数据库迁移与测试
- [x] 步骤 4：文档和验证证据回写

### 风险与回滚
- 风险：`content_json` 可能较大，当前前端固定按 20 条分页读取；草稿量增长后再评估摘要列。
- 风险：旧前端仍调用角色/故事正式保存接口，需要后续单独切换到草稿接口。
- 回滚：回退草稿模块代码并执行 `DROP TABLE ts_draft`；草稿表与正式业务表无外键，不影响角色、故事数据。

### 验证记录
- 跨模块编译：`mvn -pl jeecg-module-system/jeecg-system-biz -am -DskipTests compile`，结果 `BUILD SUCCESS`。
- 定向测试：JUnit Platform Launcher 执行 `TsDraftModelTest`，3 条成功、0 条失败。
- Mapper XML：PowerShell XML 解析通过；归属 SQL、软删除条件和显式排序检查通过。
- 格式检查：目标文件 `git diff --check` 通过，API 文档和 Mapper XML 已按原文件规范保持 UTF-8 BOM 与 CRLF。
- 测试限制：完整 Maven `test` 被仓库既有 `AiragPromptTemplateServiceTest.java:77` 类型错误阻塞，与本次草稿接口无关。

### 结果
- 新增统一草稿表和 `/sys/ts-drafts` 增删改查接口，`role/story` 共用同一资源。
- 列表、详情及写接口均返回完整 `content` JSON 对象，不重复维护 `cardData`。
- 查询、详情、更新和删除均按当前登录用户过滤，删除采用软删除。

### 节点来源扩展（2026-07-14）
- 目标：事件表增加 `node_name/node_type`，正式助手消息增加 `source_node_name/source_event_id`。
- 规则：最后一个成功且返回非空正文的节点作为助手消息来源；Gate 返回追问时 Gate 即为来源节点。
- 规则：SubAgent 完整事件记录最终产出节点，Tool 完整事件记录实际调用节点。
- 规则：SubAgent 事件落库后将事件 ID 回写运行上下文，助手消息通过 `source_event_id` 关联该完整 Task。
- 范围外：不恢复 LLM start/delta/end 分段落库，不增加额外过程事件记录。

### 节点来源扩展步骤
1. 新增 `V3.9.1_28`，扩展消息表和事件表字段及索引。
2. 在 `NodeRunner/AgentContext` 维护当前节点和最终结果节点。
3. 扩展事件 Service/Mapper/API 查询条件，并将 SubAgent 事件 ID 回写上下文。
4. 扩展助手消息落库字段，补充测试、API 文档与编译证据。

### 节点来源扩展进度
- [x] 步骤 1：节点执行入口、结果汇聚点和消息落库链路分析
- [x] 步骤 2：数据库与持久化模型改造
- [x] 步骤 3：运行时节点来源传播
- [x] 步骤 4：测试、编译与文档回写

### 节点来源扩展验证
- 数据库迁移：新增 `db/V3.9.1_28__add_agent_node_source_fields.sql`，需在 `V3.9.1_27` 后执行。
- 跨模块编译：`mvn -pl jeecg-module-system/jeecg-system-biz -am -DskipTests compile`，结果 `BUILD SUCCESS`。
- AIRAG 定向测试：JUnit Platform Launcher 执行 `AgentEventPublisherTest`、`TsAgentChatMessageEventServiceTest`、`AgentContextNodeSourceTest`、`AgentContextPreparerTest`，11 条成功、0 条失败。
- system-biz 定向测试：独立编译并执行 `TsAgentChatMessageServiceImplTest`，1 条成功、0 条失败。
- 测试限制：AIRAG 标准 Maven `test` 仍被仓库既有 `AiragPromptTemplateServiceTest.java:77` 类型错误阻塞，与本次改动无关。

### 节点来源扩展结果
- SubAgent/Tool 完整事件均可记录实际执行节点。
- 子 Agent 助手消息可通过 `source_event_id` 关联本轮完整 SubAgent 事件。
- 主 Agent 助手消息保留 `source_node_name`，`source_event_id` 为空。
- 失败结果优先记录当前失败节点，避免关联到前一个成功节点。
- Handoff 切换活动 Agent 时清空上一 Agent 的节点来源和事件关联，避免跨 Agent 串联。

### 任务 ID
`20260714-agent-complete-task-tool-events`

### 背景
- `ts_chat_message_events` 当前按 `start/error/end` 保存 LLM、SubAgent 和 Tool 过程事件，记录碎片较多。
- 业务只需要回放完整的 SubAgent Task 和 Tool 调用，不需要持久化 LLM 与 Agent 控制流事件。

### 目标
- 每次 SubAgent Task 只在 `subagent.end` 保存一条完整记录。
- 每次 Tool 调用只在 `tool.end` 保存一条完整记录。
- 保持现有 SSE 事件兼容，不改变前端消费顺序。

### 范围
- 范围内：`AgentEventPublisher`、`TsChatMessageEventService`、`ToolNode`、相关单元测试和事件存储文档。
- 范围外：数据库表结构、正式聊天消息落库、前端 SSE 处理。

### 执行步骤
1. 将 SubAgent/Tool 的 start 与 error 改为只收集执行信息并发送 SSE。
2. 在 end 节点组装固定的 `input/output/error/metrics` JSON，并只落一条记录。
3. 停止持久化 LLM 与 Agent 控制流事件，排除内部 `task` Handoff Tool 的重复记录。
4. 补充单元测试、编译验证、ADR 与变更记录。

### 进度
- [x] 步骤 1：调用链与异常路径确认
- [x] 步骤 2：完整事件聚合实现
- [x] 步骤 3：新增定向测试并完成模块编译验证
- [x] 步骤 4：文档与证据回写

### 风险与回滚
- 风险：进程在 end 节点前异常退出时，本次未完成执行不会生成事件记录。
- 监控信号：Task/Tool 执行日志存在但对应完整事件缺失。
- 回滚步骤：回退事件发布器与事件写入服务改动，恢复 start/error/end 分段落库。

### 验证记录
- 编译命令：`mvn -pl jeecg-boot-module/jeecg-boot-module-airag -am -DskipTests compile`
- 编译结果：`BUILD SUCCESS`
- 定向测试：隔离编译并执行 `AgentEventPublisherTest`、`TsChatMessageEventServiceTest`，共 5 条测试全部通过。
- 测试覆盖：SubAgent 成功、Tool 失败、LLM 不落库、内部 task Tool 不落库、固定 JSON 字段与显式空值。
- 测试限制：仓库既有 `AiragPromptTemplateServiceTest` 存在与本次无关的测试编译错误，导致 AIRAG 全量 `test` 生命周期无法完成。

### 结果
- `ts_chat_message_events` 新增记录仅来自 `subagent.end` 与非内部 `tool.end`。
- 每次执行只保存一条完整记录，JSON 固定为 `input/output/error/metrics`。
- Tool 通过 `parent_event_id` 关联所属 SubAgent Task。

### 任务 ID
`20260714-agent-node-resume-state`

### 背景
- 会话当前只保存 `active_agent_code`，下一轮能够直接进入子 Agent，但无法恢复到角色/故事流程的具体阶段。
- 每次请求都会新建 `AgentContext`，角色和故事子 Agent 又固定从 `dialog` 开始，导致已有核心数据、确认阶段和后续生成节点无法稳定续跑。

### 目标
- 会话新增恢复节点、恢复阶段和白名单流程状态，下一条用户消息可从具体子 Agent 节点继续。
- 角色与故事子 Agent 按恢复阶段显式路由，不再无条件重置为 `dialog`。
- 子 Agent 完成后显式 Handoff 回主 Agent，并清空会话恢复状态。

### 范围
- 范围内：`ts_agent_chat_session`、`AgentContext`、会话回复服务、角色/故事子 Agent、运行时测试与文档。
- 范围外：前端协议、SSE 事件格式、通用工作流引擎、任意上下文字段的全量序列化。

### 执行步骤
1. 新增 `V3.9.1_29` 与 Session 恢复字段，建立流程状态白名单快照/恢复能力。
2. 在创建 `AgentContext` 时恢复节点、阶段和业务字段，在 Run 结束后按结果状态更新或清空。
3. 改造角色/故事子 Agent，根据恢复阶段进入对话、确认、图片、声音或背景节点，完成后显式交还主 Agent。
4. 补充定向测试、跨模块编译、ADR、架构与变更记录。

### 进度
- [x] 步骤 1：现有阶段、工具字段、Handoff 和测试边界分析
- [x] 步骤 2：数据库与流程状态模型改造
- [x] 步骤 3：角色/故事节点恢复路由与完成 Handoff
- [x] 步骤 4：测试、编译与文档证据回写

### 风险与回滚
- 风险：状态 JSON 损坏或包含旧字段时恢复失败。
- 缓解：仅恢复当前 Agent 的白名单字段，解析异常时回退到阶段默认节点。
- 风险：完成 Handoff 后主 Agent 再次委托相同任务。
- 缓解：交还报告明确标记 `completed=true` 和完成摘要，主 Agent直接总结，不再次派活。
- 回滚步骤：回退运行时与 Session Entity 改动；新增字段均可空，旧代码可继续运行，必要时执行 `ALTER TABLE ts_agent_chat_session DROP COLUMN ...`。

### 验证记录
- AIRAG 编译：`mvn -pl jeecg-boot-module/jeecg-boot-module-airag -am -DskipTests compile`，结果 `BUILD SUCCESS`。
- system-biz 跨模块编译：`mvn -pl jeecg-module-system/jeecg-system-biz -am -DskipTests compile`，结果 `BUILD SUCCESS`。
- AIRAG 定向测试：流程状态、上下文准备、运行循环、角色恢复、故事恢复共 13 条成功，0 条失败。
- system-biz 定向测试：会话流程状态保存/清理 1 条成功，0 条失败。
- 测试限制：标准 AIRAG `test-compile` 仍被既有 `AiragPromptTemplateServiceTest.java:77` 类型错误阻塞，与本次改动无关。

### 结果
- 子 Agent 跨消息续接从 Agent 级提升为节点和阶段级。
- 角色恢复到 `voice` 时不会重复执行对话、门禁和图片节点；故事恢复到 `background` 时不会重复执行前置节点。
- 等待用户或可重试失败时保存白名单状态，完成 Handoff 回主 Agent 后清空。

### 任务 ID
`20260717-agent-confirm-summary-removal`

### 目标与边界
- 从角色和故事确认 Tool Schema、待交互状态、AgentResult 与 SSE 中移除 `summary`。
- 保留 `transferData` 的后端节点传递能力，以及普通工具结果的通用摘要机制。
- backup 前端确认区域只消费 `question` 和 `options`，不保存或渲染确认摘要。

### 执行步骤
- [x] 移除角色/故事确认工具的 `summary` 参数和校验。
- [x] 移除待交互、等待结果及确认型 `tool.end` 的 `summary` 字段。
- [x] 同步 backup 前端状态机和测试。
- [x] 完成后端定向测试、前端验证与证据回写。

### 风险与回滚
- 风险：仍按旧 schema 调用的模型可能多传 `summary`，将因 `additionalProperties=false` 触发参数校验失败。
- 缓解：模型侧 ToolSpecification 与注册中心 JSON Schema 已同步更新，下一次模型调用会使用新 schema。
- 回滚：恢复两个确认工具契约中的 `summary` 属性及 SSE 映射，不涉及数据库变更。

### 验证记录
- 后端编译：`mvn -pl jeecg-boot-module/jeecg-boot-module-airag -am -DskipTests compile`，结果 `BUILD SUCCESS`。
- 后端定向测试：`AgentEventPublisherTest`、角色/故事确认工具与恢复测试共 24 条，0 失败。
- 前端状态机测试：6 条通过，覆盖新协议及旧 `data.summary` 不进入状态、不渲染。
- 前端 TypeScript 定向诊断为零，Babel Web 转译通过。
- 标准 Maven `test` 生命周期仍被既有 `AiragPromptTemplateServiceTest.java:77` 类型错误阻塞，定向测试通过临时 Surefire 覆盖执行，覆盖已删除且未留在 POM。

### 任务 ID
`20260601-ts-story-full-generate-v1`

### 背景
- 前端需要“故事全量生成”能力：随机选中 story 预设并读取绑定标签，先完成模板变量替换，再串联现有故事设定/场景/大纲生成链路。
- 章节模式下，按产品规则允许先跳过大纲自动填充。

### 目标
- 新增聚合接口 `POST /sys/ts-stories/story-full-generate`。
- 出参与现有单字段生成保持一致（设定/场景/大纲 VO 不变，聚合返回）。
- 新增一个前置编排 Prompt 模板（toolcall 输出结构化字段）。

### 范围
- 范围内：
  - `TsStoryController`、`ITsStoryService`、`TsStoryServiceImpl`
  - `ITsStoryGenerateService`、`TsStoryGenerateServiceImpl`
  - `dto/tsstory`、`vo/tsstory`
  - `resources/prompts/story/story_full_generate_v1.txt`
  - `docs/api/ts-api.md`、`docs/changelog.md`
- 范围外：
  - 前端 UI 逻辑调整
  - 数据库表结构变更

### 执行步骤
1. 新增 full-generate DTO/VO 与 Controller/Service 接口。
2. 实现随机 story 预设 + 绑定标签查询 + `{{ value }}` 替换。
3. 使用新模板做 toolcall 预编排，再复用现有设定/场景/大纲生成。
4. 同步 API 文档与变更记录，并做编译验证。

### 进度
- [x] 步骤 1
- [x] 步骤 2
- [x] 步骤 3
- [x] 步骤 4
