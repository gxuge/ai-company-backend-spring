# 文档索引

## 1. 治理与流程
- `../AGENTS.md`：协作规则、提交约束、文档更新硬规则
- `../PLANS.md`：任务计划、进度、风险、决策记录
- `changelog.md`：面向行为变化的变更记录
- `spring-boot-dev.skill`：通用 Spring Boot 接口开发标准（Spring Boot 任务起始即加载）
- `spring-boot-hardness.skill`：Spring Boot 任务 Hardness 执行约束（与 dev skill 配套加载）

## 2. 架构与配置
- `architecture.md`：模块边界、启动入口、配置激活机制
- `configuration.md`：环境配置矩阵与敏感配置规范

## 3. API 文档
- `api/Index.md`：API 文档目录索引（含跨模块链路索引）
- `api/sys-auth-api.md`：系统登录鉴权接口
- `api/airag-api.md`：AIRAG 业务接口
- `api/ts-api.md`：TS 业务接口

## 4. 决策记录
- `decisions/0001-template.md`：ADR 模板
- `decisions/*.md`：架构/策略决策记录

## 5. 推荐使用流程
1. 涉及 Spring Boot 代码任务时，先加载 `spring-boot-dev.skill` 与 `spring-boot-hardness.skill`。
2. 在 `PLANS.md` 建立任务上下文与执行计划。
3. 实施代码改动并完成验证。
4. 同步更新 API/配置文档与 `changelog`。
5. 关键技术取舍写入 ADR。
