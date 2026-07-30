# 20260730 统一草稿 CRUD Hardness

## 元信息
- 任务 ID：20260730-unified-draft-crud
- 任务名称：角色与故事统一草稿增删改查
- 分级：H2
- 负责人：Codex
- 开始时间：2026-07-30
- 关联：`/sys/ts-drafts`

## 目标与非目标
- 目标：新增统一草稿表和标准增删改查接口。
- 目标：使用 `role/story` 类型区分角色草稿和故事草稿。
- 目标：列表、详情及写接口返回完整结构化内容，由前端直接组装草稿卡片。
- 非目标：不修改角色、故事正式数据，不实现草稿发布，不改前端页面。

## 输入约束
- 草稿属于当前登录用户，禁止跨用户查询、更新和删除。
- `draftType` 只允许 `role` 或 `story`。
- `content` 必须是 JSON 对象，数据库使用 LONGTEXT 保存。
- 分页默认 `1/10`，单页最大 `100`，按更新时间和 ID 倒序。
- 删除采用 `status=0` 软删除。

## 任务分解
### T1 数据模型
- 输入：现有 Ts 业务表和 MyBatis-Plus 实体规范。
- 执行动作：新增 `ts_draft` 表、Entity、DTO、PO、VO。
- 输出：统一草稿持久化与接口模型。
- 验收标准：字段注释完整，类型与状态约束明确。
- 证据类型：SQL、代码 diff。

### T2 CRUD 与权限
- 输入：现有 Controller/Service/Mapper 分层和归属 AOP。
- 执行动作：新增分页、详情、新增、编辑、删除接口。
- 输出：`GET/POST/PUT/DELETE /sys/ts-drafts`。
- 验收标准：列表显式排序；详情、编辑、删除均校验当前用户归属。
- 证据类型：Mapper XML、编译结果、测试。

### T3 验证与文档
- 输入：新增代码、SQL 和接口契约。
- 执行动作：执行单元测试、模块编译、XML 解析和差异检查。
- 输出：API 文档、ADR、变更记录和验证证据。
- 验收标准：新增模块编译通过，定向测试失败数为 0。
- 证据类型：Maven、XML 解析、`git diff --check`。

## 验证矩阵
| 验证项 | 方法 | 期望 |
|---|---|---|
| 模型转换 | JUnit 定向测试 | role/story JSON 往返一致 |
| 分页边界 | JUnit 定向测试 | pageSize 最大为 100 |
| Mapper XML | XML 解析 | namespace、标签合法 |
| Java 编译 | Maven 定向编译 | BUILD SUCCESS |
| 权限边界 | SQL/AOP 代码检查 | 所有单条写读均包含 userId |
| 格式检查 | `git diff --check` | 无新增错误 |

## 上下文与防漏策略
- 分段执行：模型、CRUD、数据库、验证分别完成。
- 压缩保留：接口契约、类型枚举、归属过滤、未完成验证和风险。
- 恢复顺序：读取本文件、`PLANS.md`、工作区 diff、最近验证输出。

## 风险与回退
- 风险：大 JSON 进入列表会增加查询和响应开销。
- 缓解：当前草稿箱固定按 20 条分页读取；不额外维护可能失步的 `cardData`。
- 风险：旧草稿状态数据不会自动迁移。
- 回退：删除新增 Controller/Service/Mapper/模型并执行 `DROP TABLE ts_draft`。

## 完成定义
- [x] 数据表和分层代码完成
- [x] 权限与分页约束完成
- [x] 验证矩阵执行
- [x] API、ADR、changelog 更新

## 未完成项
- 完整 Maven `test` 仍被仓库既有 `AiragPromptTemplateServiceTest.java:77` 类型错误阻塞，与本任务无关。

## 验证结果
- 跨模块编译：`mvn -pl jeecg-module-system/jeecg-system-biz -am -DskipTests compile`，结果 `BUILD SUCCESS`。
- 定向测试：JUnit Platform Launcher 执行 `TsDraftModelTest`，4 条成功、0 条失败。
- Mapper XML：PowerShell XML 解析通过。
- 权限检查：列表和单条查询均包含 `user_id` 与有效状态过滤，详情、编辑、删除使用归属 AOP。
- 格式检查：目标文件 `git diff --check` 通过。
