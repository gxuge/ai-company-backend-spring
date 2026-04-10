# Hardness 任务计划：文本语音接口与 Langfuse 配置落地

## 任务 ID
`20260331-airag-text-voice-langfuse-hardness`

## 背景
- 已确认存在文本与语音相关能力：`/airag/chat/send`（主聊天链路）与 `/ai/minimax/tts`（MiniMax Demo 链路）。
- 已确认存在 Langfuse 相关配置与观测代码（OTLP exporter、ObservationFilter），但主聊天链路是否完整上报仍需实证。
- 当前 `application.properties` 存在明文密钥与凭证，违反文档安全规范，存在泄露风险。

## 目标
- 明确“文本接口、语音接口、Langfuse”三类能力的运行状态（已启用/可启用/未生效）。
- 形成可执行的配置落地与验证方案，避免“配置存在但未应用”。
- 完成敏感配置治理方案（环境变量注入 + 密钥轮换 + 文档同步）。

## 范围
- 范围内：
  - `jeecg-boot-module-airag` 的配置、控制器、服务调用链与观测链路。
  - `docs/api/*`、`docs/configuration.md`、`docs/changelog.md`、`docs/decisions/*`。
- 范围外：
  - 前端页面功能改造。
  - 非 AIRAG 模块的业务重构。

## 执行步骤
1. 现状核对：梳理文本/语音/Langfuse 的配置项、开关项、Bean 装配与调用链。
2. 启用策略：明确 `AIRAG_MINIMAX_DEMO_ENABLED`、OTLP headers、采样率等环境差异与默认值。
3. 生效验证：建立“接口调用 -> 日志/指标 -> Langfuse Trace 可见”的最小闭环验证清单。
4. 安全整改：移除仓库明文密钥，改为 `${ENV_VAR}`，并补充密钥轮换与回滚说明。
5. 文档固化：同步更新 API 文档、配置说明、变更记录与 ADR。

## 进度
- [ ] 步骤 1：现状核对完成并输出证据清单。
- [ ] 步骤 2：形成环境配置矩阵（dev/test/prod）。
- [ ] 步骤 3：完成一次端到端验证并记录结果。
- [ ] 步骤 4：完成敏感配置替换与密钥轮换。
- [ ] 步骤 5：完成文档与 ADR 同步。

## 决策记录
- 决策：采用“先验证链路可观测性，再启用能力开关，最后执行安全收口”的顺序推进。
- 备选方案：
  - 方案 A：直接开启全部开关后统一联调。
  - 方案 B：按链路分阶段验证（chat -> tts -> tracing）。
- 选择原因：降低故障定位成本，避免开关叠加造成问题归因困难。
- 影响面：AIRAG 配置、Demo 接口可用性、观测平台可见性、配置安全基线。

## 风险与回滚
- 风险：
  - Langfuse 仅覆盖 Spring AI 链路，主聊天链路观测缺口仍可能存在。
  - 开启 Demo 接口后出现滥用风险（鉴权/限流不足）。
  - 密钥轮换窗口处理不当导致调用失败。
- 监控/告警信号：
  - `/airag/chat/send` 或 `/ai/minimax/*` 5xx 增长。
  - OTLP 导出失败日志、Langfuse Trace 缺失。
  - MiniMax 调用 401/429 升高。
- 回滚步骤：
  1. 关闭 `AIRAG_MINIMAX_DEMO_ENABLED`。
  2. 回退 OTLP/Tracing 相关环境变量至上一稳定版本。
  3. 回退配置文件变更并恢复上一个有效密钥版本。

## 验证记录
- 构建命令：待执行。
- 测试命令：待执行。
- 手工验证：待执行（记录请求参数、响应、traceId、Langfuse 链接）。

## 结果
- 结果摘要：待执行。
- 后续事项：
  - 评估主聊天链路是否补充 Langfuse 统一埋点策略。
  - 维护 `docs/api/Index.md` 的跨模块索引，并移除无效文档引用。
