<template>
  <PageWrapper contentFullHeight fixedHeight>
    <div class="doc-page">
      <div class="page-header">
        <div>
          <h2 class="title">文档管理</h2>
          <p class="desc">当前知识库：{{ kbId || '-' }}</p>
        </div>
        <a-space>
          <a-button @click="goWorkspace">返回工作台</a-button>
          <a-button type="primary" @click="goImport">导入文档</a-button>
        </a-space>
      </div>

      <BasicTable @register="registerTable">
        <template #tableTitle>
          <a-space>
            <a-button type="primary" @click="goImport">手动文本导入</a-button>
            <a-button @click="reload">刷新</a-button>
          </a-space>
        </template>
        <template #status="{ text }">
          <a-tag :color="Number(text) === 1 ? 'green' : 'red'">{{ Number(text) === 1 ? '启用' : '禁用' }}</a-tag>
        </template>
        <template #parse_status="{ text }">
          <a-tag :color="statusColor(text)">{{ text || '-' }}</a-tag>
        </template>
        <template #chunk_status="{ text }">
          <a-tag :color="statusColor(text)">{{ text || '-' }}</a-tag>
        </template>
        <template #embed_status="{ text }">
          <a-tag :color="statusColor(text)">{{ text || '-' }}</a-tag>
        </template>
        <template #action="{ record }">
          <TableAction :actions="getActions(record)" />
        </template>
      </BasicTable>
    </div>
  </PageWrapper>
</template>

<script lang="ts" setup>
  import { computed, onMounted, watch } from 'vue';
  import { useRoute, useRouter } from 'vue-router';
  import { PageWrapper } from '/@/components/Page';
  import { BasicTable, TableAction } from '/@/components/Table';
  import { useListPage } from '/@/hooks/system/useListPage';
  import { deleteKnowledgeBaseDocument, listKnowledgeBaseDocuments, triggerKnowledgeBaseDocumentEmbedding } from '../KnowledgeBase.api';

  const route = useRoute();
  const router = useRouter();
  const kbId = computed(() => String(route.params.kbId || ''));

  const columns = [
    { title: '文档名称', dataIndex: 'name', width: 180, ellipsis: true },
    { title: '来源类型', dataIndex: 'source_type', width: 120 },
    { title: '文件类型', dataIndex: 'file_type', width: 120 },
    { title: '解析状态', dataIndex: 'parse_status', width: 110, slots: { customRender: 'parse_status' } },
    { title: '分段状态', dataIndex: 'chunk_status', width: 110, slots: { customRender: 'chunk_status' } },
    { title: 'Embedding', dataIndex: 'embed_status', width: 110, slots: { customRender: 'embed_status' } },
    { title: '状态', dataIndex: 'status', width: 90, slots: { customRender: 'status' } },
  ];

  const { tableContext } = useListPage({
    tableProps: {
      title: '文档管理',
      api: async (params: Recordable) => {
        if (!kbId.value) {
          return { success: true, result: { records: [], total: 0 } };
        }
        return listKnowledgeBaseDocuments(kbId.value, params);
      },
      columns,
      canResize: true,
      useSearchForm: false,
      actionColumn: {
        width: 160,
      },
      defSort: {
        column: 'createdAt',
        order: 'desc',
      },
    },
  });

  const [registerTable, { reload }] = tableContext;

  onMounted(() => {
    reload();
  });

  watch(
    () => kbId.value,
    () => reload()
  );

  function goWorkspace() {
    router.push(`/super/airag/knowledge-base/${kbId.value}`);
  }

  function goImport() {
    router.push(`/super/airag/knowledge-base/${kbId.value}/import`);
  }

  function statusColor(text: any) {
    if (text === 'success') return 'green';
    if (text === 'failed') return 'red';
    if (text === 'processing') return 'orange';
    return 'blue';
  }

  function handleDelete(record: Recordable) {
    deleteKnowledgeBaseDocument(record.id).then(() => reload());
  }

  function handleEmbedding(record: Recordable) {
    triggerKnowledgeBaseDocumentEmbedding(record.id).then(() => reload());
  }

  function getActions(record: Recordable) {
    return [
      {
        label: 'Embedding',
        onClick: handleEmbedding.bind(null, record),
      },
      {
        label: '删除',
        popConfirm: {
          title: '是否确认删除',
          confirm: handleDelete.bind(null, record),
        },
      },
    ];
  }
</script>

<style scoped lang="less">
  .doc-page {
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

