# 文档索引

## 1. 治理与流程
- `../AGENTS.md`：协作规则、提交约束、文档更新硬规则
- `../PLANS.md`：任务计划、进度、风险、决策记录
- `changelog.md`：面向行为变化的变更记录

## 2. 架构与配置
- `architecture.md`：模块边界、启动入口、配置激活机制
- `configuration.md`：环境配置矩阵与敏感配置规范

## 3. API 文档
- `api/README.md`：API 文档编写规范
- `api/sys-auth-api.md`：系统登录鉴权接口
- `api/airag-api.md`：AIRAG 业务接口
- `api/hardness-api-inventory.md`：全项目接口资产清单（扫描基线）

## 4. 决策记录
- `decisions/0001-template.md`：ADR 模板
- `decisions/*.md`：架构/策略决策记录

## 5. 推荐使用流程
1. 在 `PLANS.md` 建立任务上下文与执行计划。
2. 实施代码改动并完成验证。
3. 同步更新 API/配置文档与 `changelog`。
4. 关键技术取舍写入 ADR。
