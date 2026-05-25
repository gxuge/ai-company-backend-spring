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
