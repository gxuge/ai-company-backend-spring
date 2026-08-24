# PLANS.md

## 使用说明
用于记录中大型任务的执行过程，强调“先记录、再实现、持续更新”。

建议在以下场景使用：
- 跨多个模块的改动
- 涉及鉴权、配置、数据结构、外部集成的改动
- 需要长期追踪的优化任务

---

### 任务 ID
`20260821-ts-points-billing-admin-ui`

### 背景
- 后端已提供积分账户、积分流水、充值订单、积分商品、会员赠送规则和统一账单管理接口。
- 前端 `tanshi` 目录已有支付、会员配置和活动管理页面，需要补充积分运营后台。
- 菜单由后端 `sys_permission` 和 `sys_role_permission` 驱动，页面组件路径必须与动态路由约定一致。

### 目标
- 新增积分管理页面，覆盖账户查询、流水查询、充值订单、充值商品和会员赠送规则。
- 新增统一账单页面，覆盖现金/积分收支汇总、账单查询和账单详情。
- 增加管理员菜单迁移，并保持 SQL 可重复执行。

### 执行步骤
1. 分析积分和账单后台接口、返回字段及动态菜单约束。
2. 新增前端 API、表格、筛选、编辑弹窗和详情抽屉。
3. 增加 Tanshi 子菜单和管理员角色授权。
4. 执行编码、差异、字段映射和前端工具链验证。

### 进度
- [x] 步骤 1：完成接口、页面模式和菜单授权分析
- [x] 步骤 2：完成积分管理页面
- [x] 步骤 3：完成统一账单页面
- [x] 步骤 4：完成菜单迁移、编码和静态检查

### 决策记录
- 选择复用 `useListPage`、`BasicTable`、`BasicForm`、`BasicModal` 和 `BasicDrawer`，保持 Tanshi 现有页面交互一致。
- 后台调整积分时前端生成幂等 Key，避免重复点击导致重复记账。
- 会员赠送规则通过现有会员配置接口映射等级/套餐名称，后端规则接口保持不变。

### 风险与回滚
- 风险：真实环境未执行 SQL 迁移时，页面文件虽然存在，但不会出现在动态菜单中。
- 风险：账单详情接口当前返回结构没有独立币种字段，因此页面只显示金额数值，不猜测币种。
- 回滚：移除前端 `points`、`billing` 目录和 `V3.9.1_39__add_tanshi_points_billing_menu.sql`；按 SQL 文件末尾回滚语句删除菜单授权和菜单记录。

### 验证记录
- `git diff --check`：已通过目标文档、SQL 和前端文件检查。
- 编码检查：新增源文件与 SQL 为 UTF-8 无 BOM、LF；既有 `docs/api/ts-api.md` 的 UTF-8 BOM/CRLF 未改动。
- 前端 ESLint：`corepack pnpm exec eslint src/views/system/tanshi/points src/views/system/tanshi/billing`，通过。
- 前端 Prettier：`corepack pnpm exec prettier --check src/views/system/tanshi/points src/views/system/tanshi/billing`，通过。
- 前端生产构建：`corepack pnpm exec vite build`，通过；日志仅包含仓库既有 CSS、动态导入和旧页面重复字段警告。
- 前端类型检查：仓库现有 `vue-tsc@1.8.27` 与 `typescript@5.9.3` 启动不兼容，在项目诊断前报 `Search string not found`，未能执行类型诊断。

### 结果
- 已完成积分与账单管理页面代码及菜单迁移；ESLint、Prettier 和生产构建均通过。

---

### 任务 ID
`20260820-ts-activity-center`

### 背景
- 在现有会员、会员权益和积分账本上新增签到、周期任务和奖励中心。
- 所有奖励必须经统一奖励服务发放，不允许直接修改积分余额。

### 目标
- 提供用户、管理员、内部行为三组 `ts` 前缀接口。
- 通过周期键和唯一索引保证签到、进度、领取与积分发放幂等。
- 支持 NORMAL/VIP/SVIP 奖励加成，并保持会员接口不变。

### 执行步骤
1. 建立活动任务、进度、签到、奖励、会员加成和行为去重模型。
2. 实现统一奖励、签到、行为进度和任务领取服务。
3. 实现用户、后台和内部接口，所有业务 ID 使用查询参数或 JSON Body。
4. 更新 API、ADR、变更记录并完成测试与编译。

### 进度
- [x] 步骤 0：完成规范、积分、会员和路由冲突分析。
- [x] 步骤 1：完成迁移、计划、Hardness 与 ADR 初稿。
- [x] 步骤 2：活动模型与服务。
- [x] 步骤 3：Controller 与 API 文档。
- [x] 步骤 4：测试、编译与验收。

### 验证记录
- 跨模块编译：`mvn -pl jeecg-module-system/jeecg-system-biz -am -DskipTests compile`，
  8 个 Reactor 模块 `BUILD SUCCESS`。
- 测试源码：`mvn -pl jeecg-module-system/jeecg-system-biz -DskipTests test-compile`，
  结果 `BUILD SUCCESS`。
- 定向测试：JUnit Platform Launcher 执行统一奖励、活动服务和周期键
  3 个测试类，共 8 条测试成功、0 失败。
- Mapper XML：`TsActivityQueryMapper.xml` 通过 XML 解析。
- 静态检查：活动代码仅通过 `ITsPointsService.add()` 发放星钻；
  三组 Controller 均未使用 `@PathVariable`。

### 未完成项
- 未执行真实数据库迁移和登录态 HTTP 冒烟测试。

### 风险与回滚
- 每日/每周任务必须使用 `cycle_key`，否则无法重复参与。
- 行为上报必须使用 `bizId` 去重，避免消息重试重复累计。
- 回滚时逆序删除六张活动表，并移除活动 Java/XML；会员和积分数据不变。

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
`20260823-ts-unified-reward-event`

### 背景
- 活动签到、任务领取和会员开通赠送都已通过积分服务记账，但奖励触发入口、规则读取和幂等键分散在不同业务 Service。
- 后续奖励规则扩展时，不应修改多个 Controller 或通过 AOP 承载奖励计算。

### 目标
- 新增统一奖励事件模型与策略分发器，现有对外接口和请求参数保持不变。
- 活动奖励同步执行并保留原事务；会员开通奖励在支付事务提交后执行。
- 奖励最终仍统一调用 `ITsPointsService`，任何模块不得直接修改积分余额。
- 提供管理员事件监控、详情和受控失败重试页面。

### 范围
- 范围内：奖励事件表、事件执行器、活动/会员处理器、支付与活动内部调用衔接、管理接口、Vue3 页面、菜单、测试和文档。
- 范围外：新增统一规则编辑接口、迁移现有规则表、积分充值入账改造、使用 AOP 计算奖励。

### 执行步骤
1. 新增奖励事件迁移、Entity、DTO、VO、枚举和 Mapper。
2. 实现事件协调器、幂等执行器和活动/会员策略处理器。
3. 将签到、任务领取和会员支付成功衔接到事件中心。
4. 新增后台事件分页、汇总、详情、重试接口及 Vue3 管理页面。
5. 更新 ADR、Hardness、变更记录并执行编译和前端检查。

### 进度
- [x] 步骤 0：完成现有奖励、会员支付和积分记账链路分析。
- [x] 步骤 1：奖励事件数据模型与迁移。
- [x] 步骤 2：统一事件执行与策略分发。
- [x] 步骤 3：活动和会员链路接入。
- [x] 步骤 4：后台接口、Vue3 页面与菜单。
- [ ] 步骤 5：测试、编译与文档证据。

### 决策记录
- 事件中心只统一触发、幂等、状态和策略分发；具体积分记账继续由 `ITsPointsService` 负责。
- 现有 `activity_task_reward_rule` 和 `member_points_gift_rule` 暂不迁移，处理器继续读取原规则，避免破坏后台配置接口。
- AOP 不参与奖励金额计算；显式事件调用便于事务控制、排错和重试。
- 管理后台只允许查看和重试失败事件，不允许编辑奖励金额或删除成功事件。

### 风险与回滚
- 风险：支付成功后的奖励改为提交后执行，奖励短暂失败时会员已开通但积分尚未到账。
- 缓解：失败事件保存在 `reward_event`，积分幂等键保证重试不重复入账。
- 回滚：恢复活动和支付 Service 的原直接调用，删除奖励事件 Java 文件；`reward_event` 可保留审计，必要时执行 `DROP TABLE IF EXISTS reward_event`。

### 验证记录
- 待执行：奖励事件定向测试、活动服务测试、会员赠送测试、Maven 编译和 `git diff --check`。

### 未完成项
- 实现与验证进行中。

### 任务 ID
`20260823-ts-feedback-audit`

### 背景
- 现有反馈、评论/回复和追加内容发布后立即公开，缺少内容审核状态与审核留痕。
- 反馈处理状态 `received/processing/completed` 表示业务处理进度，不能复用为内容审核状态。

### 目标
- 为反馈、评论/回复、追加内容增加独立审核状态和审核信息。
- 新增统一管理端审核队列与审核操作接口，审核操作写入历史日志。
- 公开查询、点赞和回复仅作用于审核通过内容；作者可查看自己的待审核或驳回内容及原因。
- 评论总数只统计审核通过的评论和回复。

### 范围
- 范围内：数据库迁移、Entity/DTO/PO/VO、审核 Service/Mapper/Controller、公开查询和互动规则、测试与文档。
- 范围外：前端审核管理页面、自动 AI 审核、评论删除后的计数修正。

### 执行步骤
1. 新增审核字段、审核日志表和领域模型。
2. 实现统一审核队列及通过/驳回事务。
3. 调整发布默认状态、官方回复规则和评论计数。
4. 调整公开可见性、详情、点赞和回复边界。
5. 更新 API、ADR、变更记录并执行编译与测试。

### 进度
- [x] 步骤 0：完成现有反馈中心结构与计数边界分析。
- [x] 步骤 1：审核数据模型与迁移。
- [x] 步骤 2：管理端审核接口。
- [x] 步骤 3：用户端可见性与互动规则。
- [x] 步骤 4：文档、测试与验收。

### 决策记录
- 审核状态使用 `pending/approved/rejected`，与反馈业务处理状态完全分离。
- 用户发布内容默认 `pending`；管理端官方回复默认 `approved`。
- 采用统一审核队列，以 `targetType=feedback/comment/append` 区分目标。
- 评论总数在审核状态进入或离开 `approved` 时增减，避免待审核内容污染公开计数。

### 风险与回滚
- 风险：应用代码上线但迁移未执行时会因缺少审核字段导致 SQL 失败。
- 风险：历史评论计数若已存在人工修正，迁移后的审核状态切换仍以当前冗余计数为基准。
- 回滚：先回退 Java/XML，再删除审核日志表、审核索引和三张业务表的审核字段；历史业务内容不删除。

### 验证记录
- 主代码编译：`mvn -pl jeecg-module-system/jeecg-system-biz -DskipTests "-Dmaven.resources.skip=true" compile`，结果 `BUILD SUCCESS`。
- 定向测试：独立编译并通过 JUnit Launcher 执行反馈审核、评论和点赞 3 个测试类，共 7 条测试成功、0 失败。
- Mapper XML：`TsFeedbackMapper.xml`、`TsFeedbackCommentMapper.xml`、`TsFeedbackAuditMapper.xml` 均通过 XML 解析。
- 路由检查：新增审核接口使用 `GET/PUT /sys/ts-admin-feedback/audit`，权限均为 `feedback:admin:audit`，未新增路径变量。
- 编码检查：新增 SQL、Java、XML 和文档为 UTF-8 无 BOM + LF；`docs/api/ts-api.md` 保持 UTF-8 BOM + CRLF。

### 未完成项
- 未执行真实数据库迁移和登录态 HTTP 冒烟。

### 结果
- 已完成反馈、评论/回复、追加内容审核状态、统一审核队列、审核日志、公开可见性和互动限制。
- 部署前需执行 `db/V3.9.1_41__add_ts_feedback_audit.sql`。

### 任务 ID
`20260820-story-scene-prompt-optimize`

### 背景
- `/pages/create-scene` 复用角色编辑器，AI 润色按钮当前调用角色图片提示词接口。
- 后端已有故事场景背景图生成链路，需要补充故事专属场景提示词润色接口。

### 目标
- 新增 `/sys/ts-stories/optimize-scene-image-prompt`。
- 保持模板标识 `story_scene_image_prompt_optimize::v1` 不变，补齐场景专用 tool call 结构、开发提示词和字段长度约束。
- 前端场景页切换到故事接口，角色页行为保持不变。

### 范围
- 范围内：故事 DTO/VO、Controller/Service、数据库模板迁移、前端 API 与场景编辑器适配、接口文档和变更记录。
- 范围外：故事图片生成接口改造、前端接口契约改名。

### 执行步骤
1. 新增后端故事提示词润色链路。
2. 将故事模板切换为 tool call，补齐正式场景规则和 schema。
3. 同步数据库模板迁移、接口文档和变更记录。
4. 前端注入故事润色 API。
5. 执行编译、类型检查和差异检查。

### 进度
- [x] 步骤 1：后端接口与模板
- [x] 步骤 2：tool call 模板、schema 修复和长度约束
- [x] 步骤 3：数据库迁移、文档同步
- [x] 步骤 4：前端接入
- [x] 步骤 5：验证

### 风险与回滚
- 风险：数据库未执行迁移或模板记录被删除时，接口将无法按 `prompt_key + version` 找到模板。
- 回滚步骤：回退 Java 的工具调用切换和长度兜底，数据库回滚为恢复旧 `content` 或删除本次新增模板记录。

### 验证记录
- 后端编译：`D:\maven\bin\mvn.cmd -pl jeecg-module-system/jeecg-system-biz -am -DskipTests compile`，结果 `BUILD SUCCESS`。
- 前端类型检查：沿用前序任务结果 `bun run check:types` 通过，本轮未改前端。
- 差异检查：目标 Java、SQL 和文档通过 `git diff --check`；无新增空白错误。
- 模板检查：数据库迁移中的 `tool_schema` 可解析，工具名、required 字段和 220/80 长度约束均通过。
- 编码检查：`TsStoryGenerateServiceImpl.java` 保持纯 CRLF；`docs/api/ts-api.md` 保持 UTF-8 BOM + CRLF；数据库迁移脚本为 UTF-8 无 BOM。

### 结果
- 新增故事场景图片提示词润色接口 `/sys/ts-stories/optimize-scene-image-prompt`。
- `story_scene_image_prompt_optimize::v1` 已改为数据库唯一运行来源，通过 `prompt_key + version` 读取 `airag_prompts`；已删除对应 classpath txt 文件。
- 数据库迁移 `db/V3.9.1_40__add_story_scene_image_prompt_optimize_prompt.sql` 已由用户执行。
- `/pages/create-scene` 已切换到故事专属润色 API，角色页面继续使用角色接口。

### 未完成项
- 未执行 AI 供应商接口冒烟。


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
`20260819-ts-points-billing`

### 背景
- 现有会员、会员权益、会员订单和 Stripe/PayPal 支付链路已经完成。
- 需要新增独立积分账户、积分流水、积分充值、后台调整和统一账单能力。
- 会员核心接口和数据结构保持兼容；积分通过支付编排边界与会员订单衔接。

### 目标
- 建立统一 `ITsPointsService`，所有积分增减、退款和后台调整均通过同一事务入口。
- 支持用户积分查询、内部业务扣费、积分充值、后台管理和双视角统一账单。
- 余额不足返回机器可识别错误码 `POINTS_NOT_ENOUGH`。
- 通过原子更新、唯一索引和业务锁保证余额非负、消费/充值/退款幂等。

### 范围
- 范围内：
  - `user_points_account`、`points_transaction`、`points_recharge_product`、
    `points_recharge_order`、`points_recharge_payment`。
  - 用户、内部业务、管理后台、用户账单和平台账单接口。
  - 会员支付成功后的幂等赠送积分衔接。
  - SQL、API 文档、ADR、变更记录、测试与编译验证。
- 范围外：
  - 修改既有会员等级、权益额度、会员订单对外契约。
  - 前端页面和真实支付渠道账号配置。
  - 会员订单退款业务。

### 执行步骤
1. 建立积分数据模型、索引、枚举和机器错误码。
2. 实现积分账户、流水、增加、消费、退款和后台调整。
3. 实现积分商品、充值订单、支付创建/查询和回调结算。
4. 实现用户视角与平台视角账单分页、详情和平台汇总。
5. 在会员支付成功编排边界接入幂等会员赠送积分。
6. 更新文档并完成 Mapper XML、测试、编译和差异检查。

### 进度
- [x] 步骤 0：读取开发规范、会员/支付现状和数据库迁移状态。
- [x] 步骤 1：积分模型与核心账本。
- [x] 步骤 2：积分充值与支付。
- [x] 步骤 3：后台管理和双视角账单。
- [x] 步骤 4：会员赠送积分衔接。
- [x] 步骤 5：文档、测试与验收。

### 决策记录
- 决策：积分表 `user_id` 使用 `VARCHAR(32)`，与 `sys_user.id` 和会员表一致。
- 决策：账单不使用单一 `direction`，改为 `money_direction` 与
  `points_direction`，并分别提供用户视角和平台视角接口。
- 决策：积分充值使用独立支付流水，复用现有 `PaymentProvider`，不改变
  `payment_transaction` 绑定会员订单的语义。
- 决策：充值订单作为复合账单展示，统一账单排除其关联的 `RECHARGE`
  积分流水，避免重复展示。
- 决策：新增积分域异常和异常处理器，复用 `Result.errorCode` 字段。

### 风险与回滚
- 风险：积分支付与会员支付共享相同 webhook 路径，需要按支付意图 ID
  在两类支付流水中准确路由。
- 风险：会员赠送积分规则未配置时只能跳过发放并记录日志。
- 风险：统一账单跨三类数据源分页查询复杂，必须使用数据库 `UNION ALL`
  和稳定排序。
- 回滚步骤：
  - 回退积分 Java/XML/文档改动。
  - 移除会员支付编排中的积分赠送调用。
  - 按迁移脚本中的逆序语句删除六张新增表；既有会员表不受影响。

### 验证记录
- 构建命令：`mvn -pl jeecg-module-system/jeecg-system-biz -am -DskipTests compile`，
  结果 `BUILD SUCCESS`。
- 测试源码：`mvn -pl jeecg-module-system/jeecg-system-biz -DskipTests test-compile`，
  结果 `BUILD SUCCESS`。
- 定向测试：JUnit Platform Launcher 执行积分账本、错误码和双视角账单
  3 个测试类，共 7 条测试成功、0 失败。
- Mapper XML：三个新增 XML 均通过 XML 解析。
- 手工验证：用户接口从登录态取用户 ID；管理接口要求管理员角色；内部
  HTTP 接口要求 `ts:points:internal` 权限。

### 结果
- 已完成积分核心、充值支付、后台管理、会员赠送积分、双视角账单和文档。
- 部署前必须执行 `db/V3.9.1_37__create_ts_points_billing.sql`，并配置积分
  充值商品；会员赠送积分需配置对应赠送规则。

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
`20260812-ts-user-favorite`

### 目标与边界
- 新增当前登录用户的角色、故事统一收藏能力。
- 支持收藏、取消、状态查询和分页列表，所有数据按用户归属过滤。
- 仅允许收藏在线公开资源，列表隐藏已删除或下架内容。
- 范围外：前端接入、浏览历史、收藏数量统计和后台管理。

### 执行步骤
1. 新增收藏 Entity、DTO、PO、VO、Mapper、Service 与 Controller。
2. 新增 `ts_user_favorite` 表和用户资源唯一索引，保证收藏幂等。
3. 同步 API 文档与变更记录，执行 XML、编译和权限边界检查。

### 进度
- [x] 步骤 1：接口与业务分层实现
- [x] 步骤 2：数据库迁移与唯一索引实现
- [x] 步骤 3：编译和验证证据回写

### 验证记录
- Mapper XML：PowerShell XML 解析成功。
- 后端编译：`mvn -pl jeecg-module-system/jeecg-system-biz -am -DskipTests compile`，结果 `BUILD SUCCESS`。
- 权限边界：Controller 强制认证，用户 ID 仅从登录态读取；Mapper 的列表、状态和取消 SQL 均显式包含 `user_id` 条件。
- 数据边界：新增收藏前校验在线公开记录；分页查询过滤角色停用、故事删除和公开记录下架状态。

### 风险与回滚
- 风险：同一资源存在多个在线公开渠道时，收藏列表只展示排序最靠前的一条公开记录。
- 缓解：收藏关系绑定角色或故事主资源，不绑定渠道；后续可按产品需要扩展 `public_id`。
- 回滚：移除收藏业务新增文件，并执行 `DROP TABLE IF EXISTS ts_user_favorite`。

### 任务 ID
`20260812-ts-user-browse-history`

### 目标与边界
- 新增当前登录用户的角色、故事统一浏览记录能力。
- 重复浏览累加次数并刷新最近浏览时间，列表按最近浏览时间倒序。
- 支持分页查询、单条删除和全部清空，所有数据按用户归属过滤。
- 范围外：前端自动上报、短时间防抖、浏览量公共统计和后台管理。

### 执行步骤
1. 新增浏览记录 Entity、DTO、PO、VO、Mapper、Service 与 Controller。
2. 新增 `ts_user_browse_history` 表和用户资源唯一索引，实现并发幂等写入。
3. 同步 API 文档与变更记录，执行 XML、编译和权限边界检查。

### 进度
- [x] 步骤 1：接口与业务分层实现
- [x] 步骤 2：数据库迁移与唯一索引实现
- [x] 步骤 3：编译和验证证据回写

### 验证记录
- Mapper XML：PowerShell XML 解析成功。
- 后端编译：`mvn -pl jeecg-module-system/jeecg-system-biz -am -DskipTests compile`，结果 `BUILD SUCCESS`。
- 权限边界：Controller 强制认证，用户 ID 仅从登录态读取；分页、单条删除和清空 SQL 均显式包含 `user_id` 条件。
- 数据边界：新增记录前校验在线公开资源；分页查询过滤角色停用、故事删除和公开记录下架状态。

### 风险与回滚
- 风险：前端在页面刷新或重复挂载时多次上报，会正常累加 `view_count`。
- 缓解：当前接口按一次调用视为一次浏览；需要防抖时可后续增加时间窗口。
- 回滚：移除浏览记录业务新增文件，并执行 `DROP TABLE IF EXISTS ts_user_browse_history`。

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
# 20260813-ts-feedback-center-v1

## 元信息
- 任务名称：反馈中心后端接口
- 分级：H2
- 负责人：Codex
- 开始时间：2026-08-13

## 目标与非目标
- 目标：实现反馈发布、分页、详情、点赞、评论、两层回复、追加、官方回复和状态管理。
- 目标：所有新接口使用 `ts` 前缀；`GET` 的资源 ID 使用查询参数，`POST/PUT` 的资源 ID 使用 JSON Body。
- 目标：分页查询无 N+1，计数更新支持并发，通知事件可扩展。
- 非目标：本轮不实现前端页面、不实现附件二进制上传、不新增评论删除接口。

## 任务分解
- [x] T1：完成迁移 SQL、Entity、DTO、PO、VO。
- [x] T2：完成反馈分页、详情、一级评论和回复预览 Mapper XML。
- [x] T3：完成事务、归属校验、两层评论、点赞幂等和通知事件。
- [x] T4：完成用户端与管理端 Controller、API 文档和变更记录。
- [x] T5：完成编译、XML、编码、边界与权限验证。

## 验收标准
- 接口均位于 `/sys/ts-*`，ID 参数不使用路径变量；写接口 ID 统一放在 JSON Body。
- 点赞唯一索引与 `INSERT IGNORE` 保证并发幂等，只有首次点赞增加计数。
- 二级回复的 `parent_id` 始终指向一级评论。
- 一级评论分页只附带前 2 条回复，全部回复通过独立分页接口加载。
- 追加反馈仅允许反馈发起人操作，管理接口必须有权限注解。
- system-biz 及依赖模块 Maven 编译成功。

## 风险与回退
- 风险：回复预览使用 MySQL 8 窗口函数；低版本 MySQL 需改为批量查询后分组截取。
- 风险：冗余计数依赖所有写入口统一维护；后续删除接口必须同步扣减或执行校正。
- 回退：回退本任务 Java/XML/文档文件，并删除 `V3.9.1_36__create_ts_feedback_center.sql` 创建的五张表。

## 验证记录
- 主代码编译：`D:\maven\bin\mvn.cmd -pl jeecg-module-system/jeecg-system-biz -DskipTests compile`，结果 `BUILD SUCCESS`。
- 测试源码编译：`D:\maven\bin\mvn.cmd -pl jeecg-module-system/jeecg-system-biz -DskipTests test-compile`，结果 `BUILD SUCCESS`。
- 定向测试：通过 JUnit Launcher 执行 `TsFeedbackLikeServiceImplTest` 与 `TsFeedbackCommentServiceImplTest`，共 3 条测试成功、0 失败。
- Mapper XML：`TsFeedbackMapper.xml`、`TsFeedbackCommentMapper.xml`、`TsFeedbackLikeMapper.xml` 均通过 XML 解析。
- 路由检查：反馈中心 13 个接口全部使用 `/sys/ts-*`，无 `@PathVariable`；`GET` 资源 ID 使用 `@RequestParam`，`POST/PUT` 资源 ID 使用 `@RequestBody` DTO。
- 代码检查：`git diff --check` 无空白错误；新增 SQL 与源码保持 UTF-8 无 BOM。

## 未完成项
- 未执行真实数据库迁移与接口冒烟；本轮按代码级验证完成。

# 20260823-ts-ad-center-v1

## 元信息
- 任务名称：海报与广告运营内容管理
- 分级：H2
- 负责人：Codex
- 开始时间：2026-08-23

## 目标与非目标
- 目标：新增广告位、广告内容、投放规则、曝光/点击事件四类数据模型。
- 目标：提供管理员配置接口和前端按广告位读取、事件上报接口。
- 目标：投放身份由后端登录态和会员服务判定，客户端仅提供广告位和平台。
- 非目标：本轮不实现管理端 Vue 页面、不写菜单迁移、不接入第三方广告平台。

## 任务分解
- [x] T1：新增 V43 数据库迁移、Entity、DTO、VO。
- [x] T2：实现后台广告位、内容、规则、统计服务和接口。
- [x] T3：实现前端投放读取、受众过滤与事件幂等上报。
- [x] T4：补充 API、ADR、变更记录和定向测试。
- [x] T5：完成编译、XML、编码和差异检查。

## 验收标准
- 管理接口仅允许 `admin` 角色访问，所有写接口的业务 ID 均位于 JSON Body。
- 公开读取仅返回启用广告位中已发布且处于有效时间窗的内容。
- 匿名、登录、会员等级和指定用户规则均由后端过滤。
- 事件使用 `event_id` 唯一索引保证重复上报不重复计数。
- system-biz 模块可编译，投放过滤与事件幂等具有定向测试。

## 风险与回退
- 风险：JSON 规则字段被人工写入非法值；服务保存时统一校验，读取异常时按不投放处理。
- 风险：事件表持续增长；本期建立统计索引，后续按数据量增加归档或分区。
- 回退：回退 Java/XML/文档改动，依次删除 `ts_ad_event`、`ts_ad_delivery_rule`、`ts_ad_content`、`ts_ad_slot`。

## 验证记录
- 模块编译：`mvn -pl jeecg-module-system/jeecg-system-biz -DskipTests -Dmaven.resources.skip=true compile`，结果 `BUILD SUCCESS`。
- 测试源码编译：`mvn ... test-compile`，结果 `BUILD SUCCESS`。
- 定向测试：`TsAdDeliveryServiceImplTest` 共 3 条成功、0 失败。
- Mapper XML：`TsAdQueryMapper.xml` 通过 XML 解析；V43 四张表及回滚顺序已检查。
- 编码检查：既有 `ts-api.md` 保持 UTF-8 BOM/CRLF，`ShiroConfig.java` 保持无 BOM/CRLF；新增文件为 UTF-8 无 BOM/LF。
- 差异检查：本任务范围 `git diff --check` 无空白错误。

## 未完成项
- 未执行真实 MySQL 迁移、登录态 HTTP 冒烟和管理端 Vue 页面开发。

# 20260823-ts-ad-admin-vue

## 元信息
- 任务名称：海报与广告运营管理 Vue 页面及菜单
- 分级：H2
- 负责人：Codex
- 开始时间：2026-08-23

## 目标与非目标
- 目标：在 `jeecgboot-vue3/src/views/system/tanshi/adCenter` 新增广告位、广告内容和投放数据管理页面。
- 目标：完整对接 `/sys/ts-ad-admin/**` 管理接口，并增加探拾后台管理员菜单。
- 目标：页面使用主题变量，白天和夜间模式均不出现硬编码白底。
- 非目标：不修改前端用户侧广告展示页面，不修改后端广告业务接口。

## 任务分解
- [x] T1：新增 TypeScript API 类型和接口封装。
- [x] T2：新增广告位、广告内容、投放规则编辑组件。
- [x] T3：新增三页签管理页面及曝光点击统计。
- [x] T4：新增 V49 菜单迁移并授权 admin。
- [x] T5：更新文档并完成类型、编码和差异检查。

## 验收标准
- 广告位和广告内容支持分页查询、增改删、启停或发布下线。
- 内容编辑支持单图上传、图片预览、跳转目标和时间窗。
- 投放规则支持平台、受众、会员等级和指定用户。
- 统计页可按广告位、内容、时间查询曝光、点击和点击率。
- 页面背景使用 `@component-background`，不得新增 `background: #fff`。
- 菜单组件路径与 `defineOptions` 名称一致，且默认授权 admin。

## 风险与回退
- 风险：上传组件返回相对地址；表格预览统一使用现有文件访问地址转换。
- 风险：指定用户输入量较大；第一版使用逐行或逗号分隔文本，提交前去重。
- 回退：删除 `adCenter` 页面目录和 V49 菜单迁移，不影响后端广告接口及业务数据。

## 验证记录
- 定向 ESLint：`corepack pnpm exec eslint src/views/system/tanshi/adCenter --ext .ts,.vue`，通过。
- Prettier：`corepack pnpm exec prettier --check src/views/system/tanshi/adCenter`，通过。
- Vue SFC 解析：主页和三个交互组件均通过 `@vue/compiler-sfc` 解析。
- 生产构建：`corepack pnpm exec vite build`，9363 个模块完成转换并成功生成 PWA 产物；警告均来自仓库既有页面或构建配置。
- 类型检查限制：仓库现有 `vue-tsc@1.8.27` 与 `TypeScript 5.9.3 / Node 24.19.0` 不兼容，启动阶段报 `Search string not found`，未进入项目代码检查。
- 静态扫描：页面无硬编码白色背景，管理接口 URL 仅存在于 `adCenter.api.ts`。

## 未完成项
- 未执行真实 MySQL 菜单迁移和登录态 HTTP 冒烟。

## 结果
- 已完成运营内容管理页面、广告位与广告内容操作、投放规则配置、投放统计和管理员菜单迁移。

# 20260823-ts-behavior-tracking-v1

## 元信息
- 任务名称：推荐行为埋点第一阶段
- 分级：H3
- 负责人：Codex
- 开始时间：2026-08-23

## 目标与非目标
- 目标：提供登录用户单条与批量行为采集接口，请求线程仅校验并异步投递 Kafka。
- 目标：使用独立消费者组将同一事件分别写入 MySQL 明细表和 Redis 实时特征。
- 目标：事件按 `event_id` 幂等，Kafka 消费失败重试后进入死信 Topic。
- 非目标：本轮不开放匿名采集、不实现推荐排序接口、不迁移 ClickHouse。

## 任务分解
- [x] T1：新增行为事件迁移、Entity、DTO、VO 与幂等 Mapper。
- [x] T2：新增 Kafka 配置、Producer、单条/批量采集接口。
- [x] T3：新增 MySQL 明细消费者与 Redis 实时特征消费者。
- [x] T4：补齐 API、配置、ADR、变更记录与测试。
- [x] T5：完成编译、XML、编码和差异检查。

## 验收标准
- `/sys/ts-events/collect` 与 `/sys/ts-events/collect/batch` 均要求登录，用户 ID 只取后端登录态。
- 单批最多 100 条，事件扩展 JSON 单条不超过 8KB，事件时间仅允许最近 7 天至未来 5 分钟。
- Producer 按用户 ID 分区，接口线程不直接写 MySQL 或 Redis。
- 明细消费者使用 `event_id` 唯一索引去重；特征消费者按用户维度写 Redis 并设置 30 天 TTL。
- Kafka 未启用时接口明确失败，不伪造接收成功。

## 风险与回退
- 风险：Kafka 不可用时异步发送可能在接口响应后失败；通过 Producer 错误日志、重试和 Broker 监控发现。
- 风险：MySQL 明细表持续增长；第一阶段保留索引，后续按数据量增加归档或迁移 ClickHouse。
- 风险：重复消费导致特征重复累计；Redis 特征消费者使用事件去重键降低重复影响。
- 回退：关闭 `TS_BEHAVIOR_KAFKA_ENABLED`，回退新增 Java/XML/配置/文档，并删除 `ts_user_behavior_event`。

## 验证记录
- 完整依赖链编译：`mvn -pl jeecg-module-system/jeecg-system-biz -am -DskipTests -Dmaven.resources.skip=true compile`，结果 `BUILD SUCCESS`。
- 测试源码编译：`mvn -pl jeecg-module-system/jeecg-system-biz -DskipTests -Dmaven.resources.skip=true test-compile`，结果 `BUILD SUCCESS`。
- 定向测试：通过 JUnit Platform Launcher 执行采集服务、MySQL 消费者和 Redis 特征消费者测试，共 6 条成功、0 失败。
- Mapper XML、Kafka 配置键、编码换行和 `git diff --check` 均完成检查。

## 未完成项
- 未执行真实 Kafka、MySQL、Redis 联调和接口冒烟；需部署基础设施并执行 V44 迁移后验证。

# 20260823-agent-global-safety-skill

## 元信息
- 任务名称：Agent LLM 节点全局安全 Skill
- 分级：H2
- 负责人：Codex
- 开始时间：2026-08-23

## 目标与非目标
- 目标：所有 `LlmNode` 在执行前强制加载统一安全 Skill。
- 目标：安全 Skill 始终位于完整 System Prompt 最前面，不受角色、故事、DeepAgents 或用户指令覆盖。
- 目标：安全 Skill 缺失或为空时阻止 LLM 节点执行，避免无安全规则降级运行。
- 非目标：本轮不实现输入/输出 Moderation API，不调整非 Agent 的 `IPromptChatService` 调用链。

## 任务分解
- [x] T1：新增 `ai_safety_guard` Skill 资源。
- [x] T2：在 `NodeRunner` 中强制加载并独立保存安全 Skill 正文。
- [x] T3：在 `LlmNode` 中将安全 Skill 前置到完整 System Prompt。
- [x] T4：补充加载失败和 Prompt 优先级测试。
- [x] T5：补充 ADR、变更记录并完成编译与编码检查。

## 验收标准
- 每个经过 `NodeRunner` 执行的 `LlmNode` 均加载 `ai_safety_guard`。
- 最终第一条 `SystemMessage` 以安全 Skill 正文开头，业务和节点 Skill 位于其后。
- 安全 Skill 不存在、读取失败或正文为空时，LLM 不得被调用。
- AIRAG 模块定向测试和编译通过，修改文件保持原编码与换行符。

## 风险与回退
- 风险：外部 Skill 根目录未同步提供 `ai_safety_guard` 时，Agent LLM 节点会失败关闭。
- 缓解：默认 classpath 内置安全 Skill；使用外部 Skill 根目录时必须同步部署该 Skill。
- 回退：删除安全 Skill 资源，并回退 `NodeRunner`、`LlmNode`、测试和文档改动。

## 验证记录
- 主代码编译：`D:\maven\bin\mvn.cmd -pl jeecg-boot-module/jeecg-boot-module-airag -DskipTests compile`，结果 `BUILD SUCCESS`。
- 定向测试：单独编译并执行 `LlmNodeConversationHistoryTest`、`NodeRunnerSafetySkillTest`，共 11 条测试成功、0 失败、0 错误。
- 测试覆盖：安全 Skill 强制加载、frontmatter 去除、独立上下文保存、System Prompt 最高优先级、缺失安全 Skill 和空运行上下文时失败关闭。
- 仓库既有限制：AIRAG 全量 `testCompile` 被既有 `AiragPromptTemplateServiceTest` 构造参数类型错误阻塞，本次测试通过定向编译执行，不修改无关测试。
- 差异与编码检查：本任务文件 `git diff --check` 无空白错误，BOM 与换行符保持原格式。

## 未完成项
- 未执行真实模型调用冒烟；代码级加载、顺序与失败路径已验证。

# 20260823-ts-work-review-admin

## 元信息
- 任务名称：探拾作品内容审核管理端
- 分级：H2
- 负责人：Codex
- 开始时间：2026-08-23

## 目标与非目标
- 目标：在 Vue3 管理端 `system/tanshi/workReview` 中提供角色内容、角色图片、故事内容、故事图片四类审核视图。
- 目标：复用统一作品审核接口，支持分页、详情、AI 初审信息、审核日志、AI 重试、管理员通过和驳回。
- 目标：补充探拾应用管理菜单及 admin 角色授权迁移。
- 非目标：不改变后端作品审核状态机，不将文本与图片拆成独立审核结论。

## 任务分解
- [x] T1：核对作品审核分页、详情和操作接口及审核材料字段。
- [x] T2：实现四类审核标签页、共享列表和详情抽屉。
- [x] T3：接入 AI 重试、管理员通过和驳回操作。
- [x] T4：补充动态菜单迁移、变更记录和代码级验证。

## 验收标准
- 四类标签页均按角色/故事和文本/图片正确筛选展示。
- 只有 `PENDING_ADMIN` 任务允许管理员通过或驳回，驳回原因必填。
- `PENDING_AI` 任务可重新提交 AI 初审。
- 菜单迁移可重复执行，并提供明确回滚 SQL。

## 风险与回退
- 风险：后端审核结论按完整作品版本生效，管理员在单一材料标签页操作时可能误认为只审核当前材料。
- 缓解：详情抽屉固定提示审核结论覆盖当前版本全部文本与图片。
- 回退：删除 `system/tanshi/workReview` 页面目录并执行菜单迁移内的回滚 SQL。

## 验证记录
- 定向格式检查：本地 Prettier 检查通过，5 个新增 Vue/TypeScript 文件均符合项目格式。
- 定向静态检查：本地 ESLint 检查通过，0 个错误和警告。
- SFC 与语法检查：使用 `@vue/compiler-sfc` 编译模板和脚本，并用 TypeScript 转译新增 `.ts` 文件，5 个文件全部通过。
- 差异检查：`git diff --check` 通过；新增前端文件和 SQL 均为 UTF-8 无 BOM、LF。
- 验证限制：全仓 `vue-tsc` 因仓库当前 `vue-tsc 1.8.27` 与 `TypeScript 5.9.3` 不兼容而启动失败，未产生指向本次文件的类型错误。

## 未完成项
- 未执行真实管理员账号接口联调和数据库菜单迁移。

# 20260823-ai-safety-prompt-expansion

## 元信息
- 任务名称：AI 安全 Skill 扩展到非 Agent 模型入口
- 分级：H2
- 负责人：Codex
- 开始时间：2026-08-23

## 目标与非目标
- 目标：抽取统一安全 Skill Prompt 提供器，消除各模型入口重复读取和拼装逻辑。
- 目标：覆盖 `IPromptChatService` 的普通文本与 Tool Call，统一保护 TS 角色、故事和 JSON 修复。
- 目标：覆盖普通 MiniMax 聊天和图片生成 Prompt。
- 非目标：本轮不实现线上 Moderation API，不审核模型生成后的图片内容。

## 任务分解
- [x] T1：抽取 `GlobalSafetySkillPromptProvider` 并迁移 `NodeRunner`。
- [x] T2：接入 `MiniMaxPromptChatServiceImpl` 的普通文本和 Tool Call。
- [x] T3：接入 `MiniMaxDemoServiceImpl` 的普通聊天和图片生成。
- [x] T4：补充公共提供器、消息顺序和入口拼装测试。
- [x] T5：更新 ADR、变更记录并完成编译、测试和编码检查。

## 验收标准
- 所有目标入口均从 `ai_safety_guard` 读取同一份规则。
- 文本模型的第一条 System Message 以安全规则开头。
- 图片供应商实际收到的 Prompt 以安全规则和图片任务边界开头。
- 安全 Skill 缺失或为空时失败关闭。
- AIRAG 与 system-biz 相关代码可编译，定向测试通过。

## 风险与回退
- 风险：图片 Prompt 增加安全规则后输入长度增加。
- 缓解：长度限制仍针对用户原始 Prompt，系统安全规则作为固定系统开销管理。
- 风险：外部 Skill 根目录缺少安全 Skill 时目标入口不可用。
- 缓解：默认 classpath 内置；外部部署必须同步该 Skill，缺失时明确失败。
- 回退：回退公共提供器和三个入口改动，恢复仅 Agent 注入。

## 验证记录
- AIRAG 与 system-biz 主代码编译通过。
- AIRAG 安全 Skill、加载顺序和公共提供器定向测试 14 条通过。
- Prompt Chat、普通聊天和图片 Prompt 入口定向测试 2 条通过。

## 未完成项
- 未执行真实供应商接口冒烟；进入下一阶段实现输入输出 Moderation。

# 20260823-ai-moderation-pipeline

## 元信息
- 任务名称：AI 文本输入输出统一审核流程
- 分级：H2
- 负责人：Codex
- 开始时间：2026-08-23

## 目标与非目标
- 目标：新增统一 `ModerationService`、`ModerationResult`、风险策略与厂商响应 Adapter。
- 目标：覆盖 Agent LLM、公共 Prompt Chat、普通聊天和图片 Prompt 的输入审核，并覆盖文本模型输出审核。
- 目标：不明确内容带最近上下文复审；中风险输出安全重写一次后再次审核。
- 非目标：本轮不审核生成图片像素，不新增数据库审核日志表，不改变现有 Controller/API 返回结构。

## 任务分解
- [x] T1：实现统一审核请求、结果、类别、动作、阶段和风险策略。
- [x] T2：实现 LLM JSON 审核 Adapter、线上审核 Service 和脱敏审核日志。
- [x] T3：接入 `LlmNode` 输入审核、输出缓冲审核及安全重写。
- [x] T4：接入 `MiniMaxPromptChatServiceImpl`、`MiniMaxDemoServiceImpl` 和图片 Prompt。
- [x] T5：补充测试并完成模块编译、编码和差异检查。

## 验收标准
- 业务代码只依赖 `ModerationService`/统一编排组件，不解析厂商字段。
- 统一结果至少包含 `safe/category/score/action/reason`。
- 输入非 `ALLOW` 时不得调用主 LLM。
- Agent 输出在审核完成前不得通过 SSE 发给用户。
- 中风险输出最多安全重写一次，重写结果必须再次审核。
- 审核日志只记录阶段、类别、分数、动作、服务、时间、长度和内容摘要，不记录完整原文。
- 第一版类别覆盖 `sexual/sexual_minor/violence/self_harm/hate/harassment/illegal/privacy`。

## 风险与回退
- 风险：审核模型不可用会增加失败率。
- 缓解：审核失败统一失败关闭，输入阻断、输出返回安全回复，不放行未审核内容。
- 风险：同步输入与输出审核增加模型调用次数和延迟。
- 缓解：仅不明确内容追加上下文复审；输出只允许一次重写。
- 风险：Agent 原有流式体验变为审核后整段发送。
- 缓解：安全优先，后续若厂商支持流式审核再恢复受控流式输出。
- 回退：移除统一审核编排接入，保留上一阶段全局安全 Skill Prompt。

## 验证记录
- AIRAG 主代码编译通过，新增审核核心、Agent 输入输出门禁和 SSE 缓冲逻辑可编译。
- system-biz 主代码编译通过，Prompt Chat、普通聊天和图片 Prompt 审核入口可编译。
- AIRAG 定向测试 21 条通过，覆盖 Adapter、分数策略、上下文复审、失败关闭、中风险重写、高风险丢弃、审核服务缺失和 Agent 审核后发送。
- system-biz 定向测试 4 条通过，覆盖安全 Prompt、Prompt Chat 输入阻断不调用主模型、图片 Prompt 阻断不调用供应商。
- 根 POM Surefire 跳过开关在测试后按原始字节恢复。
- AIRAG 全量测试编译仍被既有 `AiragPromptTemplateServiceTest` 构造参数类型错误阻塞，本次使用定向测试验证且未修改无关测试。

## 未完成项
- 未执行真实线上审核模型和主模型接口冒烟。
- 第一版只审核图片文本 Prompt，不审核生成图片像素内容。

# 20260823-ts-work-review-v1

## 元信息
- 任务名称：角色与故事作品审核
- 分级：H2
- 负责人：Codex
- 开始时间：2026-08-23

## 目标与非目标
- 目标：角色、故事保存到个人作品集时固化不可变快照，并创建独立作品审核任务。
- 目标：审核流程为 AI 初审后管理员终审，内容编辑后旧任务失效并按新版本重新审核。
- 目标：未通过当前版本作品审核的角色、故事不得进入公开浏览或被公开记录上架。
- 目标：审核快照由后端从已落库作品、角色绑定和正式图片地址重建，不直接信任前端提交 JSON。
- 非目标：本轮不合并反馈、评论审核；不复用其业务表、状态机或审核日志。
- 非目标：本轮不实现管理端前端页面，不改变角色、故事生成接口的临时素材协议。

## 任务分解
- [x] T1：核对角色、故事保存链路、角色图片永久化时点和公开记录查询链路。
- [x] T2：新增作品版本字段、审核任务、审核项、审核日志和 AI 审核 Prompt。
- [x] T3：实现快照构建、SHA-256 摘要、旧任务失效、AI 初审与管理员终审。
- [x] T4：接入角色/故事新增、编辑、完整角色生成，并增加公开门禁。
- [x] T5：新增用户状态查询、管理员分页/详情/通过/驳回/AI 重试接口。
- [x] T6：补充 API 文档、ADR、变更记录、定向测试和编译验证。

## 验收标准
- 每次作品内容保存生成递增 `content_version`，审核任务唯一对应 `work_type + work_id + work_version`。
- `snapshot_json` 为后端重建的稳定 JSON，`snapshot_hash` 为其 UTF-8 字节 SHA-256。
- AI 审核异常时作品保持不可公开，任务可由管理员重试，不以失败降级为通过。
- 管理员操作只允许处理 `PENDING_ADMIN` 且仍为作品当前版本的任务。
- 公开记录创建、提交、审核通过、上架以及公开浏览均校验当前作品审核状态为 `APPROVED`。
- system-biz 与依赖模块可编译，核心状态流转和快照稳定性有定向测试。

## 风险与回退
- 风险：AI 模型输出不符合 JSON Schema，导致任务停留在 AI 待处理状态。
- 缓解：复用现有 Tool Call Schema 修复能力；失败记录原因并提供管理员重试接口。
- 风险：历史已公开作品没有审核记录，迁移后会从公开浏览中隐藏。
- 缓解：迁移将历史有效作品初始化为 `APPROVED`、`content_version=1`，新保存内容才进入新流程。
- 风险：作品保存事务提交后 AI 调用失败。
- 缓解：任务先持久化为 `PENDING_AI`，失败不放行且可重试。
- 回退：回退 Java/XML/文档改动；删除三张审核表及 Prompt，移除角色/故事审核字段和公开查询过滤。

## 验证记录
- Java 编译：`mvn -pl jeecg-module-system/jeecg-system-biz -DskipTests -Dmaven.resources.skip=true compile`，结果 `BUILD SUCCESS`。
- 定向测试：临时关闭根 POM 的 Surefire 跳过开关后执行 `TsWorkReviewServiceImplTest`，1 条成功、0 失败；开关已恢复，不保留 POM 行为变更。
- 测试覆盖：首次保存生成版本 1、快照 SHA-256 可复算、作品转私有待审、当前审核任务绑定、提交事件发布。
- 差异检查：本任务文件 `git diff --check` 无空白错误；新增 SQL、Java、Markdown 均为 UTF-8 无 BOM。
- 验证限制：未执行真实 MySQL 迁移和 AI 供应商接口冒烟；第一次全依赖编译被并行反馈模块的 `target/classes/TsFeedbackCommentMapper.xml` 文件锁阻塞，跳过资源复制后的 Java 编译通过。

# 20260823-ts-feedback-audit-admin-ui

## 元信息
- 任务名称：反馈与评论审核管理端接入
- 分级：H2
- 负责人：Codex
- 开始时间：2026-08-23

## 目标与非目标
- 目标：在 Vue3 管理后台提供反馈、评论/回复和追加内容的统一审核队列。
- 目标：支持审核通过、驳回、反馈处理状态维护和官方回复。
- 目标：新增探拾管理菜单及 `feedback:admin:audit/status/reply` 权限并默认授权管理员。
- 非目标：不修改现有审核状态机、审核接口路径和用户端反馈页面。

## 任务分解
- [x] T1：新增反馈审核 API、列表字段和筛选配置。
- [x] T2：实现统一审核列表、审核弹窗和反馈详情抽屉。
- [x] T3：新增菜单、按钮权限与管理员授权迁移。
- [x] T4：同步变更记录并执行前端类型、构建、差异和编码检查。

## 验收标准
- 审核队列默认查询 `pending`，支持按内容类型、审核状态和关键词筛选。
- 待审核内容可通过或驳回，驳回原因必填且不超过 500 字。
- 反馈详情可查看附件和追加内容，并可按权限更新处理状态、发布官方回复。
- 无相应权限时不展示审核、状态更新或官方回复操作。
- 菜单迁移可重复执行，管理员默认获得菜单和三个按钮权限。

## 风险与回退
- 风险：数据库菜单迁移未执行时页面无法从动态菜单进入。
- 风险：管理员缺少按钮权限时接口会返回无权限。
- 缓解：迁移同时创建菜单、按钮权限和管理员授权，并在页面二次校验按钮权限。
- 回退：删除新增前端目录并回滚菜单迁移中的权限和角色授权记录。

## 验证记录
- 定向 ESLint：新增 `feedbackAudit` 目录全部通过，0 条错误。
- 生产构建：执行本地 `vite build` 成功，9351 个模块完成转换并生成反馈审核页面资源。
- 类型检查限制：仓库锁定的 `vue-tsc 1.8.27` 与 `TypeScript 5.9.3 / Node 24.19.0` 不兼容，启动阶段报 `Search string not found`，未进入项目代码检查。
- 构建既有警告：`MembershipModal.vue` 重复键、历史 CSS 语法和动态导入警告仍存在，本次新增页面无编译错误。
- 静态检查：三个管理端接口路径、三个按钮权限码、四条权限记录及管理员授权迁移均已定位。
- 差异与编码检查：本任务新增文件 `git diff --check` 无空白错误，均为 UTF-8 无 BOM + LF。

## 未完成项
- 未执行真实 MySQL 菜单迁移和登录态接口冒烟。

# 20260823-ts-backend-behavior-aop

## 元信息
- 任务名称：后端业务行为自动埋点
- 分级：H2
- 负责人：Codex
- 开始时间：2026-08-23

## 目标与非目标
- 目标：点赞、收藏、反馈评论、角色/故事发布与生成成功后自动构造可信行为事件。
- 目标：事务型业务仅在提交成功后发布 Kafka，埋点失败不影响原业务。
- 目标：通过注解和 AOP 统一提取用户、资源和操作信息，特殊场景保留手动调用能力。
- 非目标：不新增关注业务接口；不启用 Kafka；不改变现有业务接口响应。

## 任务分解
- [x] T1：盘点目标 Service 方法、事务边界、返回对象和用户归属。
- [ ] T2：新增行为埋点注解、SpEL 解析切面和提交后安全发布协调器。
- [ ] T3：接入点赞、收藏、评论、发布和生成成功方法。
- [ ] T4：补充单元测试、文档、编译和差异检查。

## 验收标准
- 业务方法抛异常或事务回滚时不得发布事件。
- Kafka 关闭、序列化失败或发送提交失败时不得改变业务返回。
- 首次反馈/评论点赞记录事件，重复幂等点赞不重复记录。
- 发布事件归属作品所有者，其他交互和生成事件归属当前登录用户。
- 目标模块可编译，AOP 条件、表达式、异常隔离和提交后发布有定向测试。

## 风险与回退
- 风险：SpEL 字段路径随 DTO/VO 变化而失效；切面解析失败时记录警告并跳过事件。
- 风险：Kafka 默认关闭时业务触发大量无效发送；协调器在调用发布器前检查开关。
- 回退：移除目标方法上的注解及新增 AOP 组件，原业务代码和接口契约无需回滚。

## 验证记录
- 待执行。

## 未完成项
- 待实现与验证。

# 20260824-ts-kafka-docker-deployment

## 元信息
- 任务名称：Kafka Docker 基础设施接入
- 分级：H2
- 负责人：Codex
- 开始时间：2026-08-24
- 关联：推荐行为 Kafka 链路

## 目标与非目标
- 目标：在根目录 Compose 和 `docker-deploy/monolith` Compose 中增加可持久化的单节点 Kafka Broker。
- 目标：让后台容器通过 Docker 服务名连接 Kafka，并保留行为 Kafka 默认关闭。
- 目标：同步环境配置、配置文档、变更记录和部署决策。
- 非目标：不启动 Kafka 容器、不打开 `TS_BEHAVIOR_KAFKA_ENABLED`、不执行数据库迁移、不做生产集群配置。

## 任务分解
### T1：登记部署方案
- 输入：现有 MySQL、Redis Compose 服务、Kafka Spring 配置和行为开关。
- 执行动作：确定服务名、网络、KRaft 单节点、数据卷、连接地址和默认开关。
- 输出：本任务计划与部署 ADR。
- 验收标准：两套 Compose 的网络和服务依赖边界明确。
- 证据类型：Compose 文件、ADR、计划记录。

### T2：修改 Docker 配置
- 输入：根目录 `docker-compose.yml`、`docker-deploy/monolith/docker-compose.yml`、根 `.env`。
- 执行动作：新增 Kafka Broker、持久化目录、共享网络和后台 `depends_on`；注入 Kafka 地址及行为配置。
- 输出：两套可解析的 Compose 配置。
- 验收标准：Compose 配置展开成功，Kafka 服务名为 `jeecg-boot-kafka`，行为开关为 `false`。
- 证据类型：`docker compose config` 输出和静态检索。

### T3：同步事实文档
- 输入：新增 Docker 环境变量和单节点限制。
- 执行动作：更新配置说明、变更记录和 ADR。
- 输出：配置矩阵、变更记录和回退路径。
- 验收标准：文档明确 Docker 地址、持久化目录、默认关闭和生产限制。
- 证据类型：Markdown 文件差异和编码检查。

## 验证矩阵
| 验证项 | 方法 | 阈值/期望 | 结果证据 |
|---|---|---|---|
| Compose 语法 | `docker compose config` | 两套均成功 | 待执行 |
| 服务依赖 | 静态检查 `depends_on`、网络和服务名 | 后台依赖 Kafka，三者同网 | 待执行 |
| 默认开关 | 检查 `.env` 和根 Compose 环境变量 | `TS_BEHAVIOR_KAFKA_ENABLED=false` | 待执行 |
| 数据持久化 | 检查 Kafka volume | `kafka/data:/var/lib/kafka/data` | 待执行 |
| 业务回归 | 不启动容器、不调用接口 | 无运行态变更 | 待执行 |

## 上下文与防漏策略
- 上下文预算：配置编辑、文档同步、静态验证分三个阶段完成。
- 分段策略：先改 Compose 和环境变量，再改文档，最后执行配置检查。
- 压缩策略：保留服务名、网络、Broker 地址、开关状态、验证结果和回退方式。
- 恢复策略：恢复后先读取本节，检查四类目标文件，再继续未完成验证。

## 风险与回退
- 风险：Kafka 镜像首次拉取需要网络，或镜像版本与本地 Docker 环境不兼容。
- 缓解：使用固定镜像版本；先执行 `docker compose config`，再单独拉取和启动 Kafka。
- 风险：单节点 `replicas=1` 不适合生产高可用。
- 缓解：文档明确仅用于开发/测试，生产需改为集群配置。
- 回退：删除两套 Compose 的 Kafka 服务、后台依赖、Kafka 环境变量和 `kafka/data` 卷挂载，恢复默认 `localhost:9092`。
- 回退验证：`docker compose config` 成功，后台仍可按原 MySQL/Redis 配置启动。

## 完成定义（DoD）
- [ ] 两套 Compose 增加 Kafka Broker 且配置可解析。
- [ ] 后台连接地址和行为开关已配置，默认关闭。
- [ ] 配置文档、变更记录和 ADR 已同步。
- [ ] 静态验证证据已记录。

## 未完成项
- 待执行 Compose 静态校验；未启动或拉取 Kafka 镜像。

# 20260824-ts-ad-media-expansion

## 元信息
- 任务名称：运营内容管理媒体与卡片扩展
- 分级：H2
- 负责人：Codex
- 开始时间：2026-08-24
- 关联：探拾 / 运营内容管理、广告投放接口

## 目标与非目标
- 目标：支持自有/外部图片、视频、运营卡片和统一点击动作。
- 目标：管理端可录入、编辑、列表预览和详情预览，投放接口返回规范化字段。
- 目标：保留 `imageUrl/linkType/linkValue` 兼容旧调用，并完成数据迁移。
- 非目标：本次不接入具体第三方广告供应商、计费、回调或素材审核流程。

## 任务分解
- [x] T1：确定媒体来源、媒体类型、卡片载荷和动作字段。
- [x] T2：扩展后端 DTO、实体、查询映射、投放响应和校验。
- [x] T3：新增 `V3.9.1_51__expand_ts_ad_content_media.sql` 并回填旧数据。
- [x] T4：扩展 Vue3 管理端表单、列表和内容预览。
- [x] T5a：完成后端 `jeecg-system-biz` 编译。
- [x] T5b：完成前端 ESLint、Prettier 和 Vite 生产构建。
- [x] T5c：完成迁移脚本、字段映射、差异和编码审计。
- [ ] T5d：完成前端 `vue-tsc` 类型检查，等待升级或锁定兼容的 `vue-tsc/TypeScript` 版本。

## 验收标准
- 图片、视频、卡片均可保存；卡片不要求媒体地址，视频可配置封面。
- 外部素材和 URL 动作拒绝非 HTTP/HTTPS 地址。
- 旧版 `imageUrl/linkType/linkValue` 请求仍可创建和更新内容。
- 投放响应同时提供规范化字段和旧字段。
- 管理端可区分来源、媒体类型和动作，并能预览图片、视频和卡片。

## 风险与回退
- 风险：`image_url` 变为可空后，旧版客户端若只读取该字段将无法展示视频或卡片。
- 缓解：图片和视频继续同步 `imageUrl`，卡片客户端需升级读取 `mediaType/payloadJson`。
- 风险：外部素材失效或跨域，管理端只能校验地址格式，不能保证资源可访问。
- 回退：先迁移或删除视频/卡片，再执行 V51 SQL 末尾的字段回滚；旧图片和旧跳转字段保留。

## 验证记录
- 2026-08-24：`mvn -pl jeecg-module-system/jeecg-system-biz -am -DskipTests compile` 成功。
- 2026-08-24：广告中心文件 ESLint、Prettier 检查成功，Vite 生产构建成功。
- 2026-08-24：`vue-tsc 1.8.27` 在 TypeScript 5.9.3 下因内部脚本兼容性报错，未进入业务文件检查。
- 2026-08-24：尝试运行广告投放单测时，前置 `jeecg-boot-module-airag` 既有测试
  `AiragPromptTemplateServiceTest` 编译失败，未进入 `jeecg-system-biz` 测试阶段。

## 未完成项
- 前端类型检查工具链兼容性待处理；广告单测需绕过或修复无关的 Airag 测试编译错误。

# 20260824-ts-story-scene-option-prompts

## 元信息
- 任务名称：故事场景图片选项提示词对接
- 分级：H2
- 负责人：Codex
- 开始时间：2026-08-24
- 关联：`create-scene` 场景生图与提示词优化

## 目标与非目标
- 目标：后端接收 `time/weather/mood` 的 `key + description` 结构。
- 目标：将三段英文描述注入场景生图和提示词优化模板。
- 目标：保持旧请求兼容，不新增业务数据表。
- 非目标：不把场景选项改为数据库动态配置，不修改页面布局。

## 任务分解
### T1：请求模型与规范化
- 输入：前端三个结构化场景选项。
- 执行动作：新增公共选项 DTO，在两个请求 DTO 中接入并限制合法 key 与描述长度。
- 输出：生图、提示词优化接口可接收新字段。
- 验收标准：旧请求仍可通过；非法 key 不进入 Prompt。已完成。
- 证据类型：DTO 单测与编译结果。

### T2：Prompt 变量与模板
- 输入：DTO 中的 key/description。
- 执行动作：扩展变量构建器，新增数据库 Prompt 模板变量。
- 输出：模型能读取时间、天气、气氛描述。
- 验收标准：渲染变量包含六个场景选项字段，模板迁移可重复执行。已完成。
- 证据类型：变量单测、SQL 静态检查。

### T3：文档与验证
- 输入：接口契约变化。
- 执行动作：更新 API 文档、变更记录并执行模块编译和测试。
- 输出：可追溯的接口说明与验证记录。
- 验收标准：`jeecg-system-biz` 编译通过，目标测试通过或明确记录既有阻塞。已完成。
- 证据类型：Maven 输出、Git diff 和编码检查。

## 风险与回退
- 风险：线上 Prompt 数据未执行迁移时，新变量不会生效。
- 缓解：提供 `V3.9.1_52` 可重复执行的 Prompt 更新脚本。
- 风险：前端暂时同时把描述拼入 `siteSetting`，后端模板可能重复读取。
- 缓解：结构化字段作为主来源；后续前端可将 `siteSetting` 收敛为自由输入。
- 回退：回滚 Java DTO/变量改动，并恢复 `airag_prompts` 中两个模板的上一版本内容。

## 未完成项
- 未执行真实接口冒烟测试；需要在后端、数据库及 Prompt 迁移均已部署的环境中验证完整链路。
- `db/ai-company完整数据库sql/jeecg-boot.sql` 未改动；全量初始化场景仍需执行 `V3.9.1_52` 迁移，避免覆盖其他未提交数据库变更。

## 验证记录
- `mvn -pl jeecg-module-system/jeecg-system-biz -am -DskipTests compile`：通过。
- `StorySceneOptionPromptTest`：3 个测试通过，0 失败，0 错误。
- `V3.9.1_52__expand_story_scene_option_prompts.sql`：关键表、Prompt key、六个变量及 `COMMIT` 静态检查通过。
- 目标文件 `git diff --check`：无空白错误；Git 仅提示现有文件可能发生换行符转换。
