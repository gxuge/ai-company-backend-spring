<template>
  <PageWrapper contentFullHeight fixedHeight>
    <a-spin :spinning="loading">
      <div class="workspace">
        <div class="workspace-header">
          <div>
            <h2 class="title">{{ kbInfo.name || '知识库工作台' }}</h2>
            <p class="desc">{{ kbInfo.description || '这里将承载文档、chunk、embedding、检索测试、日志、外部知识库与 RAG 问答。' }}</p>
          </div>
          <a-space wrap>
            <a-button type="primary" ghost @click="goDocuments">文档管理</a-button>
            <a-button type="primary" ghost @click="goImport">文档导入</a-button>
            <a-button type="primary" ghost @click="goRetrievalTest">检索测试</a-button>
            <a-button type="primary" ghost @click="goRag">RAG 问答</a-button>
            <a-button @click="goBack">返回列表</a-button>
            <a-button type="primary" @click="reloadWorkspace">刷新</a-button>
          </a-space>
        </div>

        <a-tabs v-model:activeKey="activeKey" class="workspace-tabs">
          <a-tab-pane key="overview" tab="概览">
            <a-card>
              <a-descriptions :column="2" bordered size="small">
                <a-descriptions-item label="知识库 ID">{{ kbId }}</a-descriptions-item>
                <a-descriptions-item label="状态">{{ statusText }}</a-descriptions-item>
                <a-descriptions-item label="业务类型">{{ kbInfo.biz_type || '-' }}</a-descriptions-item>
                <a-descriptions-item label="默认检索模式">{{ searchConfig.search_mode || '-' }}</a-descriptions-item>
              </a-descriptions>
            </a-card>
          </a-tab-pane>
          <a-tab-pane key="documents" tab="文档管理">
            <a-space>
              <a-button type="primary" @click="goDocuments">进入文档管理</a-button>
            </a-space>
          </a-tab-pane>
          <a-tab-pane key="chunks" tab="Chunk 管理">
            <a-space>
              <a-button type="primary" @click="goChunks">进入 Chunk 管理</a-button>
            </a-space>
          </a-tab-pane>
          <a-tab-pane key="indexes" tab="索引管理">
            <a-space>
              <a-button type="primary" @click="goIndexes">进入索引管理</a-button>
            </a-space>
          </a-tab-pane>
          <a-tab-pane key="searchConfig" tab="检索配置">
            <a-space>
              <a-button type="primary" @click="goSearchConfig">进入检索配置</a-button>
            </a-space>
          </a-tab-pane>
          <a-tab-pane key="retrievalTest" tab="检索测试">
            <a-space>
              <a-button type="primary" @click="goRetrievalTest">进入检索测试</a-button>
            </a-space>
          </a-tab-pane>
          <a-tab-pane key="logs" tab="检索日志">
            <a-space>
              <a-button type="primary" @click="goLogs">进入检索日志</a-button>
            </a-space>
          </a-tab-pane>
          <a-tab-pane key="external" tab="外部知识库">
            <a-space>
              <a-button type="primary" @click="goExternal">进入外部知识库</a-button>
            </a-space>
          </a-tab-pane>
          <a-tab-pane key="rag" tab="RAG 问答">
            <a-space>
              <a-button type="primary" @click="goRag">进入 RAG 问答</a-button>
            </a-space>
          </a-tab-pane>
        </a-tabs>
      </div>
    </a-spin>
  </PageWrapper>
</template>

<script lang="ts" setup>
  import { computed, onMounted, reactive, ref } from 'vue';
  import { useRoute, useRouter } from 'vue-router';
  import { PageWrapper } from '/@/components/Page';
  import { getKnowledgeBaseDetail, getKnowledgeBaseSearchConfig } from '../KnowledgeBase.api';

  const route = useRoute();
  const router = useRouter();
  const kbId = computed(() => String(route.params.kbId || ''));

  const activeKey = ref('overview');
  const loading = ref(false);
  const kbInfo = reactive<Recordable>({});
  const searchConfig = reactive<Recordable>({});

  const statusText = computed(() => (Number(kbInfo.status) === 1 ? '启用' : Number(kbInfo.status) === 0 ? '禁用' : '-'));

  onMounted(() => {
    reloadWorkspace();
  });

  async function reloadWorkspace() {
    if (!kbId.value) {
      return;
    }
    loading.value = true;
    try {
      const [detailRes, configRes] = await Promise.all([
        getKnowledgeBaseDetail(kbId.value),
        getKnowledgeBaseSearchConfig(kbId.value),
      ]);
      if (detailRes?.success && detailRes?.result) {
        Object.assign(kbInfo, detailRes.result);
      }
      if (configRes?.success && configRes?.result) {
        Object.assign(searchConfig, configRes.result);
      }
    } finally {
      loading.value = false;
    }
  }

  function goBack() {
    router.push('/super/airag/knowledge-base/index');
  }

  function goDocuments() {
    if (!kbId.value) {
      return;
    }
    router.push(`/super/airag/knowledge-base/${kbId.value}/documents`);
  }

  function goImport() {
    if (!kbId.value) {
      return;
    }
    router.push(`/super/airag/knowledge-base/${kbId.value}/import`);
  }

  function goChunks() {
    if (!kbId.value) {
      return;
    }
    router.push(`/super/airag/knowledge-base/${kbId.value}/chunks`);
  }

  function goIndexes() {
    if (!kbId.value) {
      return;
    }
    router.push(`/super/airag/knowledge-base/${kbId.value}/indexes`);
  }

  function goSearchConfig() {
    if (!kbId.value) {
      return;
    }
    router.push(`/super/airag/knowledge-base/${kbId.value}/search-config`);
  }

  function goRetrievalTest() {
    if (!kbId.value) {
      return;
    }
    router.push(`/super/airag/knowledge-base/${kbId.value}/retrieval-test`);
  }

  function goLogs() {
    if (!kbId.value) {
      return;
    }
    router.push(`/super/airag/knowledge-base/${kbId.value}/logs`);
  }

  function goExternal() {
    if (!kbId.value) {
      return;
    }
    router.push(`/super/airag/knowledge-base/${kbId.value}/external`);
  }

  function goRag() {
    if (!kbId.value) {
      return;
    }
    router.push(`/super/airag/knowledge-base/${kbId.value}/rag`);
  }
</script>

<style scoped lang="less">
  .workspace {
    padding: 16px;
  }

  .workspace-header {
    display: flex;
    align-items: flex-start;
    justify-content: space-between;
    gap: 16px;
    margin-bottom: 16px;
  }

  .title {
    margin: 0;
    font-size: 20px;
    font-weight: 600;
  }

  .desc {
    margin: 6px 0 0;
    color: #8f959e;
  }
</style>
