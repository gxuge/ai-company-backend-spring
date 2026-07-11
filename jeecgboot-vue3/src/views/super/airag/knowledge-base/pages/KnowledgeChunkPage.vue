<template>
  <PageWrapper contentFullHeight fixedHeight>
    <div class="chunk-page">
      <div class="page-header">
        <div>
          <h2 class="title">Chunk 管理</h2>
          <p class="desc">当前知识库：{{ kbId || '-' }}</p>
        </div>
        <a-space>
          <a-button @click="goWorkspace">返回工作台</a-button>
          <a-button @click="goDocuments">文档管理</a-button>
          <a-button type="primary" @click="handleCreate">新增 Chunk</a-button>
        </a-space>
      </div>

      <BasicTable @register="registerTable">
        <template #tableTitle>
          <a-space>
            <a-button type="primary" @click="handleCreate">新增 Chunk</a-button>
            <a-button @click="reload">刷新</a-button>
          </a-space>
        </template>
        <template #status="{ text }">
          <a-tag :color="Number(text) === 1 ? 'green' : 'red'">{{ Number(text) === 1 ? '启用' : '禁用' }}</a-tag>
        </template>
        <template #action="{ record }">
          <TableAction :actions="getActions(record)" />
        </template>
      </BasicTable>

      <KnowledgeChunkModal @register="registerModal" @success="reload" />
    </div>
  </PageWrapper>
</template>

<script lang="ts" setup>
  import { computed, onMounted, watch } from 'vue';
  import { useRoute, useRouter } from 'vue-router';
  import { PageWrapper } from '/@/components/Page';
  import { BasicTable, TableAction } from '/@/components/Table';
  import { useListPage } from '/@/hooks/system/useListPage';
  import { useModal } from '/@/components/Modal';
  import KnowledgeChunkModal from '../components/KnowledgeChunkModal.vue';
  import { deleteKnowledgeBaseChunk, listKnowledgeBaseChunks, triggerKnowledgeBaseChunkEmbedding } from '../KnowledgeBase.api';

  const route = useRoute();
  const router = useRouter();
  const kbId = computed(() => String(route.params.kbId || ''));
  const [registerModal, { openModal }] = useModal();

  const columns = [
    { title: '内容', dataIndex: 'content', ellipsis: true, width: 360 },
    { title: '类型', dataIndex: 'chunk_type', width: 120 },
    { title: '顺序', dataIndex: 'sort_no', width: 80 },
    { title: 'Token', dataIndex: 'token_count', width: 100 },
    { title: '状态', dataIndex: 'status', width: 90, slots: { customRender: 'status' } },
  ];

  const { tableContext } = useListPage({
    tableProps: {
      title: 'Chunk 管理',
      api: async (params: Recordable) => {
        if (!kbId.value) {
          return { success: true, result: { records: [], total: 0 } };
        }
        return listKnowledgeBaseChunks(kbId.value, params);
      },
      columns,
      canResize: true,
      useSearchForm: false,
      actionColumn: {
        width: 180,
      },
      defSort: {
        column: 'sort_no',
        order: 'asc',
      },
    },
  });

  const [registerTable, { reload }] = tableContext;

  onMounted(() => reload());

  watch(() => kbId.value, () => reload());

  function goWorkspace() {
    router.push(`/super/airag/knowledge-base/${kbId.value}`);
  }

  function goDocuments() {
    router.push(`/super/airag/knowledge-base/${kbId.value}/documents`);
  }

  function handleCreate() {
    openModal(true, { kbId: kbId.value, isUpdate: false });
  }

  function handleEdit(record: Recordable) {
    openModal(true, { kbId: kbId.value, isUpdate: true, chunkId: record.id, record });
  }

  function handleDelete(record: Recordable) {
    deleteKnowledgeBaseChunk(record.id).then(() => reload());
  }

  function handleEmbedding(record: Recordable) {
    triggerKnowledgeBaseChunkEmbedding(record.id).then(() => reload());
  }

  function getActions(record: Recordable) {
    return [
      { label: 'Embedding', onClick: handleEmbedding.bind(null, record) },
      { label: '编辑', onClick: handleEdit.bind(null, record) },
      {
        label: '删除',
        popConfirm: { title: '是否确认删除', confirm: handleDelete.bind(null, record) },
      },
    ];
  }
</script>

<style scoped lang="less">
  .chunk-page {
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

