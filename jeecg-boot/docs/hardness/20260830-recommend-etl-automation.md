# 20260830 推荐训练数据 ETL 自动化 Hardness

## 元信息
- 任务 ID：`20260830-recommend-etl-automation`
- 任务名称：推荐训练数据 ETL 自动化
- 分级：H3
- 负责人：AI
- 时间窗口：2026-08-30
- 关联：管理后台训练数据 ETL 自动化需求

## 目标与非目标
- 目标：交付任务管理、Quartz 调度、异步 Python 执行、执行记录和 Vue3 管理页面。
- 目标：同一任务并发执行拦截成功率为 100%，进程超时后必须释放任务占位。
- 目标：后端编译与定向测试通过，前端目标文件静态检查通过。
- 非目标：不实现 Python 数据处理算法及 EasyRec 后续训练发布流水线。

## 输入约束
- 已知上下文：MySQL 为事务库，ClickHouse 已注册为动态数据源，Kafka 与 Quartz 已集成。
- 强约束：保持 `Controller -> Service -> Mapper`；敏感配置使用环境变量；脚本和输出路径限制在允许根目录。
- 禁止事项：禁止 Shell 字符串拼接执行；禁止将密码写入命令参数、任务表或日志。

## 任务分解
### T1 数据模型与接口
- 输入：现有 MyBatis-Plus、后台接口和菜单迁移模式。
- 执行动作：新增任务/执行表、Entity、DTO、VO、Mapper、Service、Controller。
- 输出：可分页管理任务和执行记录的管理员接口。
- 验收标准：CRUD、启停、手动执行和详情接口可编译，分页显式排序。
- 证据类型：SQL、编译输出、接口文档。

### T2 调度与执行
- 输入：Quartz、Kafka、ProcessBuilder 和 Python JSON 约定。
- 执行动作：实现任务级 Quartz、事务后分发、数据库占位锁、超时终止、日志落盘和 JSON 解析。
- 输出：手动与定时任务统一产生 WAITING/RUNNING/SUCCESS/FAILED 记录。
- 验收标准：重复运行被拒绝；超时和异常均进入 FAILED；终态释放占位。
- 证据类型：单元测试、Mapper SQL、编译输出。

### T3 管理前端
- 输入：`jeecgboot-vue3/src/views/system/tanshi` 现有页面规范。
- 执行动作：新增任务列表、配置弹窗、执行记录列表和详情抽屉。
- 输出：管理员可完成任务配置、启停、执行及日志查看。
- 验收标准：目标 TypeScript/Vue 文件通过静态检查，菜单组件路径可解析。
- 证据类型：前端检查命令、文件路径。

### T4 配置与部署
- 输入：应用 profile、Docker Compose 和系统镜像。
- 执行动作：增加 ETL 环境变量、Python 运行时和目录挂载，更新事实文档。
- 输出：本地与容器环境均可配置执行链路。
- 验收标准：配置可解析，文档说明默认值、敏感项和回滚方式。
- 证据类型：配置解析、Compose 检查、文档差异。

## 验证矩阵
| 验证项 | 方法 | 阈值/期望 | 结果证据 |
|---|---|---|---|
| 后端编译 | Maven 定向编译 | Reactor 成功 | 命令输出 |
| 结果解析 | JUnit | 成功/失败 JSON 全覆盖 | 测试输出 |
| 并发占位 | Service/Mapper 检查 | 同一任务仅一个 executionId | SQL 与测试 |
| 前端静态检查 | pnpm lint/typecheck 或定向检查 | 目标文件无错误 | 命令输出 |
| 配置 | YAML/Compose 解析 | 无语法错误 | 命令输出 |
| 权限 | Controller 注解与菜单授权检查 | 仅 admin 可访问 | 代码与 SQL |

## 上下文与防漏策略
- 上下文预算：模型与后端 45%，前端 25%，配置文档 15%，验证 15%。
- 分段策略：数据模型、执行链路、前端、部署验证分别收口。
- 压缩策略：保留完成项、未完成项、风险、验证证据和下一步文件。
- 恢复策略：读取本文件、`PLANS.md`、Git 差异和最近验证输出后继续。

## 风险与回退
- 风险：脚本输出不是最后一行 JSON。触发信号为 `RESULT_JSON_INVALID`，保留完整日志定位。
- 风险：Kafka 不可用。开发环境可切换 `local`，生产环境恢复 Kafka 后重试失败任务。
- 风险：容器缺少脚本挂载。启动前检查允许目录，失败时不启动进程。
- 回退步骤：关闭功能开关，删除 ETL Quartz Job，回滚菜单；必要时删除新增业务表。
- 回退验证：应用正常启动，原有 Quartz/Kafka/ClickHouse 业务不受影响。

## 完成定义（DoD）
- [x] 代码改动完成
- [x] 验证矩阵执行
- [x] 证据归档
- [x] 未完成项列出

## 未完成项
- 未提供真实 Python ETL 脚本，未启动 MySQL、Kafka、ClickHouse 执行端到端任务。

## 证据索引
- 后端编译：`mvn -Pdev -pl jeecg-module-system/jeecg-system-biz -am -DskipTests compile`，BUILD SUCCESS。
- 测试执行：JUnit Platform Launcher，tests found=3、succeeded=3、failed=0。
- 前端：定向 ESLint 无错误；Vue SFC 与 TypeScript 转译输出 `frontend transform ok`。
- 配置：YAML 输出 `yaml ok`，Mapper 输出 `mapper xml ok`，根 Compose 校验通过。
- 编码：后端目标输出 `encoding ok: 19 files`，前端输出 `frontend encoding ok: 5 files`。
