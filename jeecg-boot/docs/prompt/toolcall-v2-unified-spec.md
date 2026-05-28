# Prompt ToolCall 通用规范 v2（H2-6）

## 1. 目标与范围
- 目标：把现有所有 `JSON直出` 模板统一升级为 `ToolCall直出`。
- 后端职责固定为：`工具参数解析 + 业务字段白名单提取 + 前端返回`。
- 版本策略：不覆盖旧版，保留 `v1(JSON)`，新增 `v2(tool_call)`。

## 2. 模板层统一规范（对所有 prompt 生效）
- 模板必须包含并标准化以下 section：
- `SECTION::meta`
- `SECTION::developer_prompt`
- `SECTION::user_prompt_template`
- `SECTION::tool_schema`（新增，替代/补充 `output_schema_hint`）
- `SECTION::output_extract` 不再单独存在，必须并入 `SECTION::developer_prompt`。
- 并入方式要求：在 `developer_prompt` 中显式写出“后端白名单提取字段”，且每个字段都要用中文说明其业务含义与用途。

## 3. meta 规范（固定字段）
- 必填：`code`、`version`、`scenario`
- 必填：`output_mode=tool_call`
- 必填：`tool_name=submit_{code}`
- 必填：`strict=true|false`（是否严格校验）

## 4. tool_schema 规范（必须完整）
- 必须声明：`name`、`description`、`parameters(JSON Schema)`、`required`
- 枚举必须有语义说明（可放 `x-enum-descriptions`，或使用 `oneOf + description`）
- `parameters.properties` 下每个字段都必须有中文 `description`（强制）
- 推荐约束：
- `additionalProperties=false`
- 数组字段声明 `minItems/maxItems`
- 字符串字段声明 `minLength/maxLength`
- 严格 JSON 输出纪律应写在 `tool_schema.description`：仅允许 tool call 返回结构化 JSON，不得输出解释、Markdown 或额外文本。

## 5. 调用协议规范
- 调用链统一抽象成：
- `messages`（system + user 渲染结果）
- `tools`（tool_schema 转译结果）
- `tool_choice`（required + 指定 tool_name）

## 6. Tool 入参通用约束
- 每个 Prompt 只允许一个业务工具（单次调用单工具）。
- 工具参数必须是结构化 JSON，禁止自然语言混杂。
- 枚举必须显式定义与解释，例如：
- `emotional`：情绪回应
- `probing`：探询推进
- `actionable`：轻行动建议
- `SECTION::user_prompt_template` 必须使用 JSON 格式组织参数。
- `user_prompt_template` 中每个参数都必须附带中文释义字段（例如 `desc`/`含义`），不能只给裸变量值。
- 新增或迁移模板文件时，必须沿用原文件编码（含 BOM 与换行风格）创建/保存新文件，防止中文乱码。
