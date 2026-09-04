# ToolCall 模板渲染与变量填充规范

## 1. 目标
本文档用于统一说明“读取 Prompt 模板 -> 填充运行时变量 -> 发起 ToolCall -> 解析结果”的实现方式，确保不同业务线在同一规范下可维护、可扩展、可回溯。

## 2. 适用范围
- 所有采用模板驱动的结构化生成链路。
- 所有依赖 ToolCall 输出 JSON 参数的后端接口。
- 包含首轮解析与失败修复（JSON Repair）的调用流程。

## 3. 总体流程
1. 从模板系统按 `code + version` 获取模板定义。
2. 根据业务上下文组装变量字典（Map）。
3. 渲染模板分段内容（developer/user/tool_schema）。
4. 发起 ToolCall，请求模型按 schema 返回 JSON。
5. 首轮解析并校验 required 字段。
6. 若失败，进入修复模板流程并再次解析。
7. 将结果映射到业务对象，并写入日志/快照用于追溯。

## 4. 变量组装规范
- 变量来源应仅来自当前业务上下文与已授权数据源。
- 对空值统一做归一化处理，避免把脏值直接注入模板。
- 同一语义存在多值时，使用稳定顺序聚合并去重。
- 变量命名应保持语义化、可读性与跨场景一致性。
- 不在模板侧硬编码业务分支，优先在后端完成变量决策。

### 4.1 生成预设取值（角色/故事预设链路）
- 预设读取：
  - 从 `ts_preset` 按目标类型查询 `enabled=1` 的可用预设。
  - 按 `sort_order asc, id asc` 排序后随机选取一条可用预设。
- 变量注入：
  - 将预设名称、预设描述与用户业务输入一起注入模板变量 Map。
  - 缺失值统一走 `null token` 规范，避免残留占位符。

## 5. 模板渲染规范
- 模板路径仅作为 `code + version` 的定位入口，不作为业务逻辑来源。
- 渲染时必须输出分段结果，至少包括：
  - developer prompt
  - user prompt
  - tool schema（或等价的输出约束）
- 未填充占位符必须有统一兜底策略，避免残留模板符号进入模型输入。

### 5.1 AI 模板读取方式（code + version）
- 统一从 AI 应用元数据（`AiragApp.metadata`）解析模板信息。
- 支持两种模板引用格式：
  - 对象：`{"code":"xxx","version":"v1"}`
  - 字符串：`"xxx@v1"`
- 故事单字段模板解析优先级：
  - `storyPromptTemplate`
  - `storyPromptTemplates.<scene>`
  - 场景级显式键（如 `storySettingPromptCode/storySettingPromptVersion`）
- 渲染路径约定：
  - 故事模板：`prompts/story/{code}_{version}.txt`
  - ToolCall 修复模板：`prompts/toolcall/{code}_{version}.txt`

## 6. ToolCall 调用与结果校验
- 首轮调用使用 ToolCall 模式，要求模型返回 schema 兼容 JSON。
- 解析成功后必须执行 required 字段校验：
  - 字段存在
  - 字段非 null
  - 字符串字段非空白
- 通过校验后方可进入业务映射阶段。

## 7. 失败修复机制（JSON Repair）
- 首轮解析失败或 required 校验失败时，必须进入修复链路。
- 修复模板来源：
  - 从 `AiragApp.metadata` 读取，按以下 key 顺序择一：
    - `toolcallJsonRepairPromptTemplate`
    - `jsonRepairPromptTemplate`
    - `storyJsonRepairPromptTemplate`
  - 每个 key 均要求可解析出 `code + version`。
- 修复输入至少包含：
  - 原始返回内容
  - 目标 tool schema
  - required 字段集合
  - required 字段提示信息（字段含义描述）
- 推荐修复变量集合：
  - `scene`
  - `raw_content`
  - `tool_schema`
  - `required_fields`
  - `required_field_hints`
- 修复调用仍使用 ToolCall（`chatToolCall`），并继续使用原业务 `tool_schema` 做最终约束。
- 修复成功后需再次执行同等严格校验。
- 若修复后仍不满足 schema，返回可定位的业务异常，不允许静默吞错。

### 7.1 required_field_hints 生成
- 从 `tool_schema.parameters.properties` 中提取 required 字段的 `description`。
- 拼接格式建议：`field: description`，多字段使用中文分号 `；` 连接。
- 若字段缺少 description，至少保留字段名本身，确保修复提示完整。

## 8. 日志与快照
- 应记录以下关键节点：
  - 模板标识（code/version）
  - 渲染后的 prompt（可按策略裁剪）
  - 模型原始输出
  - 修复前后状态
  - 最终结构化结果
- 日志内容需做脱敏与长度控制，避免敏感信息泄露。

## 9. 兼容性与演进
- 新链路应尽量通过新增接口/新增模板版本实现，避免影响存量接口行为。
- 旧链路迁移时建议保留兼容代理窗口，逐步切流。
- 模板升级遵循“版本前进，不覆盖旧版本”的原则。

## 10. 安全约束
- 模板变量注入前需保证来源可信、格式安全。
- 不将未授权数据拼入模型输入。
- 严禁把内部异常堆栈直接回传给前端。
