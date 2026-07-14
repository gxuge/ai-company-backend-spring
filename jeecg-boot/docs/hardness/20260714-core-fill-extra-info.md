# 20260714 Core Fill Extra Info Hardness

## 元信息
- 任务 ID：`20260714-core-fill-extra-info`
- 任务名称：角色与故事核心生成接口增加可选额外信息
- 分级：H2
- 负责人：AI
- 开始时间：2026-07-14
- 关联：`role_core_fill`、`story_core_fill`

## 目标与非目标
- 目标：两个普通核心生成接口支持可选 `extraInfo` / `extra_info` 请求字段。
- 目标：字段分别映射为 Prompt 变量 `extra_info`，未传或空白时渲染为 `null`。
- 目标：故事 Agent 普通生成工具同步声明并透传补充信息。
- 非目标：不修改响应字段、数据库结构及两个 preset 模板。

## 输入约束
- 已知上下文：Prompt 模板已包含 `{{extra_info}}`。
- 强约束：保持现有接口向后兼容；字段可传可不传。
- 禁止事项：不得将 `extra_info` 加入模型输出 schema 或持久化模型。

## 任务分解
### T1 请求契约
- 输入：角色与故事普通核心生成 DTO。
- 执行动作：增加可选字段、snake_case 别名和空白归一化。
- 输出：请求可接受 `extraInfo` 与 `extra_info`。
- 验收标准：两种命名均能反序列化，空白值归一化为 null。
- 证据类型：单元测试。

### T2 Prompt 变量
- 输入：归一化后的 DTO。
- 执行动作：向 `role_core_fill`、`story_core_fill` 变量表加入 `extra_info`。
- 输出：有值时原样渲染，无值时为字面量 `null`。
- 验收标准：变量表断言通过。
- 证据类型：单元测试、代码 diff。

### T3 调用链与文档
- 输入：故事 Agent 普通生成工具及现有 API 文档。
- 执行动作：补充工具 inputSchema、参数透传、API 字段说明和变更记录。
- 输出：HTTP 与 Agent 普通故事生成链路行为一致。
- 验收标准：模块编译通过，文档与实现一致。
- 证据类型：编译命令、文档 diff。

## 验证矩阵
| 验证项 | 方法 | 阈值/期望 | 结果证据 |
|---|---|---|---|
| DTO 兼容性 | JUnit | camelCase/snake_case 均可接收 | `CoreFillExtraInfoTest` |
| 空值行为 | JUnit | 未传或空白映射为 `null` | `CoreFillExtraInfoTest` |
| Prompt 映射 | JUnit | 两个变量表均包含 `extra_info` | `CoreFillExtraInfoTest` |
| 回归编译 | Maven compile | BUILD SUCCESS | 执行记录 |

## 上下文与防漏策略
- 上下文预算：按请求契约、模板映射、Agent 透传、验证四段执行。
- 分段策略：每段修改后执行 diff 检查。
- 压缩策略：保留目标、变更文件、测试结果和风险。
- 恢复策略：重新读取本文件、目标 DTO/Service 和最新 diff。

## 风险与回退
- 风险：共享 DTO 使 preset 接口也能反序列化该字段，但 preset 链路不消费它。
- 触发条件：调用方误认为 preset 模板会使用 `extraInfo`。
- 回退步骤：移除 DTO 字段、变量映射、Agent 参数和文档记录。

## 完成定义
- [x] 代码改动完成
- [x] 主代码编译验证完成
- [x] 证据归档
- [x] 未完成项列出

## 未完成项
- `CoreFillExtraInfoTest` 已新增，但当前工作区无法完成定向测试执行：
  - Reactor 测试被既有 `AiragPromptTemplateServiceTest` 类型不匹配阻塞。
  - system-biz 单模块测试受并行 Agent Event 源码与本地 AIRAG 依赖状态不一致影响。
- 主代码验证命令 `mvn -pl jeecg-module-system/jeecg-system-biz -am -DskipTests compile` 已执行成功。
