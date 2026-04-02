# AGENTS.md

## 1. 目标
本文件用于约束本项目的人类开发者与 AI 代理协作方式，确保以下三件事可追溯：
- 需求上下文
- 技术决策
- 代码与配置变更

## 2. 项目范围
项目根目录：`jeecg-boot`

核心模块：
- `jeecg-boot-base-core`：公共基础能力与通用组件
- `jeecg-module-system`：系统管理、登录鉴权、权限等
- `jeecg-boot-module`：业务模块集合（含 `airag`）
- `jeecg-server-cloud`：网关与云化部署相关模块

## 3. 运行上下文
- 单体启动入口：`jeecg-module-system/jeecg-system-start/src/main/java/org/jeecg/JeecgSystemApplication.java`
- 云端相关入口：`jeecg-server-cloud/*` 下的各启动类
- 默认应用名：`jeecg-system`
- 默认上下文路径：`/jeecg-boot`

## 4. 开发约束
- 严格遵循分层：`Controller -> Service -> Mapper`。
- 保持统一返回结构与异常处理风格，不引入破坏性响应格式。
- 改动 `Controller` 时必须同步更新 `docs/api/*.md`。
- 改动配置（`application*.yml/properties`）时必须同步更新 `docs/configuration.md`。
- 改动数据库结构或索引时必须写明影响范围与回滚方案。

## 5. 文档更新硬规则
- 中大型任务：先更新 `PLANS.md`（目标、步骤、风险、决策），再改代码。
- 业务或架构取舍：新增 `docs/decisions/*.md`（ADR）。
- 对外行为变化：在 `docs/changelog.md` 留痕（至少 1 条）。
- 所有新增文档统一使用中文描述。

## 6. 安全与合规
- 严禁提交真实密钥、令牌、数据库密码、对象存储凭证。
- 敏感配置一律改为环境变量占位符，示例值仅可使用 `***` 或 `demo`。
- 涉及登录、权限、数据范围、文件上传等高风险改动，PR 必须附回归清单。

## 7. 合并前检查清单
- 影响模块可编译并通过必要测试。
- API 文档已更新。
- 配置文档已更新。
- 关键决策已记录（如适用）。
- 兼容性与回滚路径已确认。
