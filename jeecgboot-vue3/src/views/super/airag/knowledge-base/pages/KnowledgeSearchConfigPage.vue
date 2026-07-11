<template>
  <PageWrapper contentFullHeight fixedHeight>
    <div class="config-page">
      <div class="page-header">
        <div>
          <h2 class="title">检索配置</h2>
          <p class="desc">当前知识库：{{ kbId || '-' }}</p>
        </div>
        <a-space>
          <a-button @click="goWorkspace">返回工作台</a-button>
          <a-button @click="goChunks">Chunk 管理</a-button>
          <a-button type="primary" :loading="saving" @click="saveConfig">保存配置</a-button>
        </a-space>
      </div>

      <a-card>
        <BasicForm @register="registerForm" />
      </a-card>
    </div>
  </PageWrapper>
</template>

<script lang="ts" setup>
  import { computed, onMounted, ref } from 'vue';
  import { useRoute, useRouter } from 'vue-router';
  import { PageWrapper } from '/@/components/Page';
  import BasicForm from '/@/components/Form/src/BasicForm.vue';
  import { useForm } from '/@/components/Form';
  import { getKnowledgeBaseSearchConfig, saveKnowledgeBaseSearchConfig } from '../KnowledgeBase.api';

  const route = useRoute();
  const router = useRouter();
  const kbId = computed(() => String(route.params.kbId || ''));
  const saving = ref(false);

  const [registerForm, { setFieldsValue, validate }] = useForm({
    schemas: [
      { label: 'search_mode', field: 'search_mode', component: 'Select', componentProps: { options: [
        { label: 'semantic', value: 'semantic' },
        { label: 'fulltext', value: 'fulltext' },
        { label: 'hybrid', value: 'hybrid' },
      ] } },
      { label: 'similarity_threshold', field: 'similarity_threshold', component: 'InputNumber' },
      { label: 'reference_limit', field: 'reference_limit', component: 'InputNumber' },
      { label: 'top_k', field: 'top_k', component: 'InputNumber' },
      { label: 'use_rerank', field: 'use_rerank', component: 'Switch' },
      { label: 'use_query_optimization', field: 'use_query_optimization', component: 'Switch' },
      { label: 'config_json', field: 'config_json', component: 'InputTextArea', componentProps: { rows: 6 } },
    ],
    showActionButtonGroup: false,
    layout: 'vertical',
    wrapperCol: { span: 24 },
  });

  onMounted(async () => {
    await reloadConfig();
  });

  async function reloadConfig() {
    if (!kbId.value) return;
    const res = await getKnowledgeBaseSearchConfig(kbId.value);
    if (res?.success && res?.result) {
      await setFieldsValue(res.result);
    }
  }

  async function saveConfig() {
    if (!kbId.value) return;
    saving.value = true;
    try {
      const values = await validate();
      await saveKnowledgeBaseSearchConfig(kbId.value, values);
    } finally {
      saving.value = false;
    }
  }

  function goWorkspace() {
    router.push(`/super/airag/knowledge-base/${kbId.value}`);
  }

  function goChunks() {
    router.push(`/super/airag/knowledge-base/${kbId.value}/chunks`);
  }
</script>

<style scoped lang="less">
  .config-page {
    padding: 16px;
  }

  .page-header {
    display: flex;
    align-items: flex-start;
    justify-content: space-between;
    gap: 16px;
    margin-bottom: 16px;
  }

  .title {
    margin: 0;
    font-size: 22px;
    font-weight: 600;
  }

  .desc {
    margin: 6px 0 0;
    color: #8f959e;
  }
</style>
