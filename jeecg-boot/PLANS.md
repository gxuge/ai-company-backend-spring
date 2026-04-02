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
