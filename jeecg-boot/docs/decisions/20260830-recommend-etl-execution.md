# ADR：推荐 ETL 任务执行与调度方案

## 状态
已采纳，2026-08-30。

## 背景
推荐训练集生成由 Python 完成，但任务生命周期、权限、定时和审计属于 Java 管理系统。系统已有 Quartz、Kafka、MySQL 和 ClickHouse。

## 决策
1. 每个启用任务使用独立 Quartz `JobKey/TriggerKey`，任务 ID 放入 `JobDataMap`。
2. 手动和定时执行统一先创建执行记录，并通过任务表 `running_execution_id` 原子占位防重。
3. 执行分发抽象为 `local` 与 `kafka` 两种模式；开发默认 local，部署环境可使用 Kafka。
4. Python 使用参数数组形式的 `ProcessBuilder` 启动，敏感配置仅通过环境变量注入。
5. 最后一条非空标准输出必须是结果 JSON；完整输出写日志文件，数据库仅保存受限长度日志。
6. 执行状态固定为 `WAITING/RUNNING/SUCCESS/FAILED`，超时用 FAILED 加机器错误码表达。

## 权衡
- 独立 Quartz 任务避免通用 `sys_quartz_job.job_class_name` 唯一约束，但需要维护启动同步逻辑。
- 数据库原子占位可跨实例防重，比仅使用 JVM 锁可靠；应用异常退出需增加超时恢复。
- Kafka 提供削峰和解耦，local 模式降低本地开发依赖，两者复用同一个执行 Worker。

## 后续演进
执行器接口可继续增加 EasyRec 训练、评估、模型版本和发布阶段；每个阶段独立记录输入、输出和状态，不改变 ETL 任务的基础调度契约。
