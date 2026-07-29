# 20260729 故事场景背景图片生成接口 Hardness

## 元信息
- 任务 ID：20260729-story-scene-image-generate
- 任务名称：故事场景背景图片生成接口与 Agent Tool
- 分级：H2
- 负责人：Codex
- 开始时间：2026-07-29
- 关联：`POST /sys/ts-stories/one-click-scene-image`

## 目标与非目标
- 目标：新增故事场景背景图片生成接口，返回原始图片地址和提示词版本信息。
- 目标：新增 `story_generate_scene_image` Tool，并接入故事背景子 Agent。
- 目标：保证生成、素材保存、故事关联三个动作分离。
- 非目标：不修改创建故事前端页面，不自动导入用户素材，不自动更新故事 `sceneImageUrl`。

## 输入约束
- 接口不接收 `storyId`。
- `storySetting` 与 `siteSetting` 禁止同时为空。
- 默认风格为“写实影视级场景概念图”，默认比例为 `9:16`。
- 生图必须设置 `uploadGeneratedMedia=false`，只返回供应商原始地址。

## 任务分解
### T1 接口主链路
- 输入：现有故事 Controller/Service 分层。
- 执行动作：新增 DTO、VO、Service 方法和 Controller 路由。
- 输出：`POST /sys/ts-stories/one-click-scene-image`。
- 验收标准：编译通过；响应 VO 仅包含三个约定字段。
- 证据类型：代码 diff、Maven 编译结果。

### T2 Prompt 与生图
- 输入：PromptRender、ToolCall 修复链路和 MiniMax 生图服务。
- 执行动作：新增 `story_scene_image_generate::v1` 本地模板资源；数据库模板由现有配置维护，不新增 Flyway 迁移。
- 输出：结构化视觉提示词与原始图片地址。
- 验收标准：模板 JSON Schema 合法；生图请求禁止自动上传。
- 证据类型：模板检查、单元测试、代码 diff。

### T3 Agent Tool
- 输入：故事 ToolRegistry 与故事背景节点。
- 执行动作：注册并暴露 `story_generate_scene_image`。
- 输出：Agent 可按故事设定生成临时背景图片。
- 验收标准：Tool 注册测试和 Agent 定义测试通过。
- 证据类型：JUnit 输出。

## 验证矩阵
| 验证项 | 方法 | 期望 |
|---|---|---|
| Java 编译 | Maven 定向编译 | BUILD SUCCESS |
| Tool 注册 | `StoryTaskToolRegistrarTest` | 通过 |
| Agent 定义 | `DeepAgentDefinitionRegistryTest` | 通过 |
| 格式检查 | `git diff --check` | 无新增错误 |
| 编码换行 | BOM/EOL 检查 | 保持原文件格式 |

## 上下文与防漏策略
- 分段执行：接口、模板、Tool、验证分别完成。
- 压缩保留：接口契约、不入库边界、未完成验证和风险。
- 恢复顺序：读取本文件、工作区 diff、测试输出。

## 风险与回退
- 风险：数据库未配置 `story_scene_image_generate::v1` 时模板无法加载。
- 风险：模型未返回 `visual_prompt` 时接口明确失败，不使用模板正文回退生图。
- 回退：删除新增路由、DTO/VO、Service 方法、Tool 注册与本地模板资源。

## 完成定义
- [x] 接口代码完成
- [x] Prompt 资源完成
- [x] Agent Tool 完成
- [x] 验证矩阵执行
- [x] API 与 changelog 更新

## 未完成项
- 无。
