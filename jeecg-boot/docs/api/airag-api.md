# AIRAG 业务 API（`/airag`）

## 1. 范围
- 模块：`jeecg-boot-module/jeecg-boot-module-airag`
- 完整访问前缀（默认）：`/jeecg-boot/airag`
- 文档定位：记录当前项目中 AI/RAG 能力的接口入口与职责边界

## 2. 控制器总览

| 控制器 | 基础路径 | 主要能力 |
|---|---|---|
| `AiragChatController` | `/airag/chat` | 对话发送、会话管理、消息管理、SSE 接收、上传、海报/写作生成 |
| `AiragAppController` | `/airag/app` | AI 应用配置、发布、调试、提示词生成 |
| `AiragKnowledgeController` | `/airag/knowledge` | 知识库管理、文档导入、重建、检索、插件管理 |
| `AiragModelController` | `/airag/airagModel` | 模型配置增删改查、连通性测试 |
| `AiragMcpController` | `/airag/airagMcp` | MCP 服务配置、同步、状态变更、工具保存 |
| `AiragPromptsController` | `/airag/prompts` | Prompt 管理与实验 |
| `AiragExtDataController` | `/airag/extData` | 扩展数据与评测调试 |
| `AiOcrController` | `/airag/ocr` | OCR 能力管理 |
| `EoaWordTemplateController` | `/airag/word` | Word 模板管理、解析与生成 |
| `SpringAiMiniMaxDemoController` | `/ai/minimax` | MiniMax Demo（chat/tts/image） |

## 3. 核心接口（高频）

### 3.1 对话会话（`AiragChatController`）
| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/chat/send` | 发送消息（SSE） |
| GET | `/chat/send` | 通过 query 参数发送消息（SSE） |
| GET | `/chat/init` | 初始化对话上下文 |
| GET | `/chat/conversations` | 查询会话列表 |
| DELETE | `/chat/conversation/{id}` | 删除会话 |
| PUT | `/chat/conversation/update/title` | 更新会话标题 |
| GET | `/chat/messages` | 查询消息列表 |
| GET | `/chat/receive/{requestId}` | 根据请求 ID 拉取 SSE |
| GET | `/chat/stop/{requestId}` | 停止生成 |
| POST | `/chat/upload` | 上传附件 |
| POST | `/chat/genAiPoster` | 生成海报内容 |
| POST | `/chat/genAiWriter` | AI 写作（SSE） |

### 3.2 模型管理（`AiragModelController`）
| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/airagModel/list` | 模型分页查询 |
| POST | `/airagModel/add` | 新增模型配置（需权限 `airag:model:add`） |
| PUT/POST | `/airagModel/edit` | 编辑模型配置（需权限 `airag:model:edit`） |
| DELETE | `/airagModel/delete` | 删除模型配置（需权限 `airag:model:delete`） |
| GET | `/airagModel/queryById` | 根据 ID 查询模型 |
| POST | `/airagModel/test` | 模型可用性测试 |

### 3.3 MiniMax Demo（`SpringAiMiniMaxDemoController`）
| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/ai/minimax/chat` | 聊天补全示例接口 |
| POST | `/ai/minimax/tts` | 语音合成示例接口 |
| POST | `/ai/minimax/image` | 图像生成示例接口 |

### 3.4 Prompt 模板（`AiragPromptsController`）
| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/prompts/template/query` | 从 classpath 模板文件读取指定 `code+version` 的 prompt sections |

## 4. 权限约定
- `airag` 模块大量接口使用 `@RequiresPermissions`。
- 接口文档中涉及权限字段时，需同步标注权限编码（如 `airag:knowledge:add`）。

## 5. 配置依赖（摘要）
- 关键配置位于：
  - `jeecg-boot-module-airag/src/main/resources/application.yml`
  - `jeecg-boot-module-airag/src/main/resources/application.properties`
- 包含 `MINIMAX_*` 与 `AIRAG_MINIMAX_*` 相关环境变量。
- 详细规则见 `docs/configuration.md`。

## 6. 维护说明
- 每次新增/变更 `airag` 控制器映射时，同步更新本文件与 `docs/changelog.md`。

## 7. 2026-04-01 MiniMax 迁移说明
- `SpringAiMiniMaxDemoController` 已迁移至 `jeecg-module-system/jeecg-system-biz/src/main/java/org/jeecg/modules/openapi/controller`。
- Controller 下游依赖链已同步迁移至 `jeecg-module-system/jeecg-system-biz/src/main/java/org/jeecg/modules/openapi` 现有目录（config/dto/vo/service/impl）。
- MiniMax 接口路径保持不变：`/ai/minimax/chat`、`/ai/minimax/tts`、`/ai/minimax/image`。
- MiniMax 相关配置已迁移到 `jeecg-module-system/jeecg-system-start/src/main/resources/application-dev.yml`、`application-prod.yml` 及对应 `application-*.properties`。
- `prompts` 资源目录已迁移至 `jeecg-module-system/jeecg-system-biz/src/main/resources/prompts`。
- `jeecg-boot-module-airag` 中的 MiniMax 配置段已移除，避免配置分散。
