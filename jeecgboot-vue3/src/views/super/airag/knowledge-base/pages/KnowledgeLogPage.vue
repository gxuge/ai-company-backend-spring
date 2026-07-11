<template>
  <PageWrapper contentFullHeight fixedHeight>
    <div class="log-page">
      <div class="page-header">
        <div>
          <h2 class="title">检索日志</h2>
          <p class="desc">当前知识库：{{ kbId || '-' }}</p>
        </div>
        <a-space>
          <a-button @click="goWorkspace">返回工作台</a-button>
          <a-button @click="goTest">检索测试</a-button>
        </a-space>
      </div>

      <BasicTable @register="registerTable">
        <template #status="{ text }">
          <a-tag :color="text === 'success' ? 'green' : text === 'failed' ? 'red' : 'blue'">{{ text || '-' }}</a-tag>
        </template>
        <template #action="{ record }">
          <TableAction :actions="getActions(record)" />
        </template>
      </BasicTable>

      <a-modal v-model:open="detailOpen" title="日志详情" width="900px" :footer="null" destroyOnClose>
        <pre class="detail-json">{{ detailText }}</pre>
      </a-modal>
    </div>
  </PageWrapper>
</template>

<script lang="ts" setup>
  import { computed, onMounted, ref, watch } from 'vue';
  import { useRoute, useRouter } from 'vue-router';
  import { PageWrapper } from '/@/components/Page';
  import { BasicTable, TableAction } from '/@/components/Table';
  import { useListPage } from '/@/hooks/system/useListPage';
  import { getRetrievalTestLogDetail, listRetrievalTestLogs } from '../KnowledgeBase.api';

  const route = useRoute();
  const router = useRouter();
  const kbId = computed(() => String(route.params.kbId || ''));
  const detailOpen = ref(false);
  const detailText = ref('');

  const columns = [
    { title: 'query', dataIndex: 'query', ellipsis: true, width: 220 },
    { title: 'search_mode', dataIndex: 'search_mode', width: 120 },
    { title: '状态', dataIndex: 'status', width: 100, slots: { customRender: 'status' } },
    { title: 'result_count', dataIndex: 'result_count', width: 120 },
    { title: 'created_at', dataIndex: 'created_at', width: 180 },
  ];

  const { tableContext } = useListPage({
    tableProps: {
      title: '检索日志',
      api: async (params: Recordable) => {
        return listRetrievalTestLogs({ ...params, kb_id: kbId.value });
      },
      columns,
      canResize: true,
      useSearchForm: false,
      actionColumn: { width: 120 },
      defSort: {
        column: 'created_at',
        order: 'desc',
      },
    },
  });

  const [registerTable, { reload }] = tableContext;

  onMounted(() => reload());
  watch(() => kbId.value, () => reload());

  function goWorkspace() {
    router.push(`/super/airag/knowledge-base/${kbId.value}`);
  }

  function goTest() {
    router.push(`/super/airag/knowledge-base/${kbId.value}/retrieval-test`);
  }

  async function showDetail(record: Recordable) {
    const res = await getRetrievalTestLogDetail(record.id);
    detailText.value = JSON.stringify(res?.result || res, null, 2);
    detailOpen.value = true;
  }

  function getActions(record: Recordable) {
    return [
      {
        label: '详情',
        onClick: showDetail.bind(null, record),
      },
    ];
  }
</script>

<style scoped lang="less">
  .log-page {
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

  .detail-json {
    max-height: 70vh;
    overflow: auto;
    background: #0f172a;
    color: #e2e8f0;
    padding: 16px;
    border-radius: 8px;
  }
</style>
