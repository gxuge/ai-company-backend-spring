# 知识库前端并行目录说明

## 1. 目标

在不影响旧的 `src/views/super/airag/aiknowledge/` 模块的前提下，新增一套独立的知识库前端目录，用于对接后端 `/kb/*` 系列接口。

## 2. 新模块位置

- 模块根目录：`src/views/super/airag/knowledge-base/`
- 注册入口：`src/views/super/registerSuper.ts`
- 入口页面：`src/views/super/airag/knowledge-base/index.vue`

## 3. 页面结构

### 3.1 总入口

- `index.vue`
  - 知识库列表
  - 创建 / 编辑 / 删除知识库
  - 进入工作台

### 3.2 工作台

- `pages/KnowledgeWorkspace.vue`
  - 文档管理
  - 文档导入
  - Chunk 管理
  - 索引管理
  - 检索配置
  - 检索测试
  - 检索日志
  - 外部知识库
  - RAG 问答

### 3.3 子页面

- `pages/KnowledgeDocumentPage.vue`
- `pages/KnowledgeImportPage.vue`
- `pages/KnowledgeChunkPage.vue`
- `pages/KnowledgeChunkIndexPage.vue`
- `pages/KnowledgeSearchConfigPage.vue`
- `pages/KnowledgeRetrievalTestPage.vue`
- `pages/KnowledgeLogPage.vue`
- `pages/KnowledgeExternalPage.vue`
- `pages/KnowledgeRagPage.vue`

## 4. API 封装

统一封装在：

- `KnowledgeBase.api.ts`

当前覆盖的能力：

- 知识库 CRUD
- 文档 CRUD
- Chunk CRUD
- Chunk Index CRUD
- 文档导入与预览
- Embedding 触发
- 检索测试
- 检索日志
- 外部知识库
- RAG 问答

## 5. 后端接口对接范围

当前前端直接对接的后端能力主要是：

- `/kb/create`
- `/kb/list`
- `/kb/{id}`
- `/kb/{kbId}/documents`
- `/kb/{kbId}/chunks`
- `/kb/{kbId}/indexes`
- `/kb/{kbId}/search-config`
- `/kb/{kbId}/import/text`
- `/kb/{kbId}/import/file`
- `/kb/{kbId}/import/confirm`
- `/kb/{kbId}/chunks/preview-text`
- `/kb/{kbId}/chunks/preview-file`
- `/kb/{kbId}/embedding`
- `/kb/{kbId}/search`
- `/kb/retrieval-test/{kbId}`
- `/kb/retrieval-test/logs`
- `/kb/external/*`
- `/kb/rag/*`

## 6. 约定

1. 页面层不直接拼接后端 URL，统一走 `KnowledgeBase.api.ts`。
2. 新增子页面优先挂在 `knowledge-base/` 下，不回写旧模块。
3. 进入工作台后，再细分到文档、chunk、索引、检索和 RAG 能力。
4. 目前导入页采用“先预览，再确认写库”的方式。

## 7. 后续建议

1. 给日志页补结构化详情抽屉。
2. 给 RAG 页补外部知识库选择器。
3. 给检索测试页补参数调试面板。
4. 如后端新增字段，可继续在 `KnowledgeBase.api.ts` 中统一收口。
