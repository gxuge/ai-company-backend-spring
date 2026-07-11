import type { App } from 'vue';
import type { RouteRecordRaw } from 'vue-router';
import { router } from '/@/router';
import { LAYOUT } from '/@/router/constant';

const KnowledgeBaseRoutes: RouteRecordRaw[] = [
  {
    path: '/super/airag/knowledge-base',
    name: 'superAiragKnowledgeBase',
    component: LAYOUT,
    redirect: '/super/airag/knowledge-base/index',
    meta: {
      title: '知识库中心',
      icon: 'ant-design:database-outlined',
      ignoreAuth: false,
    },
    children: [
      {
        path: 'index',
        name: 'superAiragKnowledgeBaseIndex',
        component: () => import('/@/views/super/airag/knowledge-base/index.vue'),
        meta: {
          title: '知识库中心',
          ignoreAuth: false,
        },
      },
      {
        path: ':kbId',
        name: 'superAiragKnowledgeBaseWorkspace',
        component: () => import('/@/views/super/airag/knowledge-base/pages/KnowledgeWorkspace.vue'),
        meta: {
          title: '知识库工作台',
          ignoreAuth: false,
          hideMenu: true,
        },
      },
      {
        path: ':kbId/documents',
        name: 'superAiragKnowledgeBaseDocuments',
        component: () => import('/@/views/super/airag/knowledge-base/pages/KnowledgeDocumentPage.vue'),
        meta: {
          title: '文档管理',
          ignoreAuth: false,
          hideMenu: true,
        },
      },
      {
        path: ':kbId/import',
        name: 'superAiragKnowledgeBaseImport',
        component: () => import('/@/views/super/airag/knowledge-base/pages/KnowledgeImportPage.vue'),
        meta: {
          title: '文档导入',
          ignoreAuth: false,
          hideMenu: true,
        },
      },
      {
        path: ':kbId/chunks',
        name: 'superAiragKnowledgeBaseChunks',
        component: () => import('/@/views/super/airag/knowledge-base/pages/KnowledgeChunkPage.vue'),
        meta: {
          title: 'Chunk 管理',
          ignoreAuth: false,
          hideMenu: true,
        },
      },
      {
        path: ':kbId/indexes',
        name: 'superAiragKnowledgeBaseIndexes',
        component: () => import('/@/views/super/airag/knowledge-base/pages/KnowledgeChunkIndexPage.vue'),
        meta: {
          title: '索引管理',
          ignoreAuth: false,
          hideMenu: true,
        },
      },
      {
        path: ':kbId/search-config',
        name: 'superAiragKnowledgeBaseSearchConfig',
        component: () => import('/@/views/super/airag/knowledge-base/pages/KnowledgeSearchConfigPage.vue'),
        meta: {
          title: '检索配置',
          ignoreAuth: false,
          hideMenu: true,
        },
      },
      {
        path: ':kbId/retrieval-test',
        name: 'superAiragKnowledgeBaseRetrievalTest',
        component: () => import('/@/views/super/airag/knowledge-base/pages/KnowledgeRetrievalTestPage.vue'),
        meta: {
          title: '检索测试',
          ignoreAuth: false,
          hideMenu: true,
        },
      },
      {
        path: ':kbId/logs',
        name: 'superAiragKnowledgeBaseLogs',
        component: () => import('/@/views/super/airag/knowledge-base/pages/KnowledgeLogPage.vue'),
        meta: {
          title: '检索日志',
          ignoreAuth: false,
          hideMenu: true,
        },
      },
      {
        path: ':kbId/external',
        name: 'superAiragKnowledgeBaseExternal',
        component: () => import('/@/views/super/airag/knowledge-base/pages/KnowledgeExternalPage.vue'),
        meta: {
          title: '外部知识库',
          ignoreAuth: false,
          hideMenu: true,
        },
      },
      {
        path: ':kbId/rag',
        name: 'superAiragKnowledgeBaseRag',
        component: () => import('/@/views/super/airag/knowledge-base/pages/KnowledgeRagPage.vue'),
        meta: {
          title: 'RAG 问答',
          ignoreAuth: false,
          hideMenu: true,
        },
      },
    ],
  },
];

/** 注册路由 */
export async function register(_: App) {
  for (const appRoute of KnowledgeBaseRoutes) {
    await router.addRoute(appRoute);
  }
  console.log('[知识库路由] 注册完成！');
}
