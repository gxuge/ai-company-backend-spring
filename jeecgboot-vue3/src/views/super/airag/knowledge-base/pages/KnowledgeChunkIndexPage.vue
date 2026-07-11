<template>
  <PageWrapper contentFullHeight fixedHeight>
    <div class="index-page">
      <div class="page-header">
        <div>
          <h2 class="title">索引管理</h2>
          <p class="desc">当前知识库：{{ kbId || '-' }}</p>
        </div>
        <a-space>
          <a-button @click="goWorkspace">返回工作台</a-button>
          <a-button @click="goChunks">Chunk 管理</a-button>
          <a-button type="primary" @click="handleCreate">新增索引</a-button>
        </a-space>
      </div>

      <BasicTable @register="registerTable">
        <template #tableTitle>
          <a-space>
            <a-input v-model:value="chunkIdFilter" placeholder="按 chunk_id 过滤" style="width: 220px" allow-clear @press-enter="reload" />
            <a-button @click="reload">查询</a-button>
            <a-button type="primary" @click="handleCreate">新增索引</a-button>
          </a-space>
        </template>
        <template #embedding_status="{ text }">
          <a-tag :color="statusColor(text)">{{ text || '-' }}</a-tag>
        </template>
        <template #status="{ text }">
          <a-tag :color="Number(text) === 1 ? 'green' : 'red'">{{ Number(text) === 1 ? '启用' : '禁用' }}</a-tag>
        </template>
        <template #action="{ record }">
          <TableAction :actions="getActions(record)" />
        </template>
      </BasicTable>

      <KnowledgeChunkIndexModal @register="registerModal" @success="reload" />
    </div>
  </PageWrapper>
</template>

<script lang="ts" setup>
  import { computed, onMounted, ref, watch } from 'vue';
  import { useRoute, useRouter } from 'vue-router';
  import { PageWrapper } from '/@/components/Page';
  import { BasicTable, TableAction } from '/@/components/Table';
  import { useListPage } from '/@/hooks/system/useListPage';
  import { useModal } from '/@/components/Modal';
  import KnowledgeChunkIndexModal from '../components/KnowledgeChunkIndexModal.vue';
  import {
    deleteKnowledgeBaseChunkIndex,
    listKnowledgeBaseChunkIndexes,
    listKnowledgeBaseChunkIndexesByChunk,
    triggerKnowledgeBaseChunkIndexEmbedding,
  } from '../KnowledgeBase.api';

  const route = useRoute();
  const router = useRouter();
  const kbId = computed(() => String(route.params.kbId || ''));
  const chunkIdFilter = ref('');
  const [registerModal, { openModal }] = useModal();

  const columns = [
    { title: 'chunk_id', dataIndex: 'chunk_id', width: 160, ellipsis: true },
    { title: 'index_text', dataIndex: 'index_text', width: 320, ellipsis: true },
    { title: 'index_type', dataIndex: 'index_type', width: 120 },
    { title: 'embedding_status', dataIndex: 'embedding_status', width: 130, slots: { customRender: 'embedding_status' } },
    { title: 'status', dataIndex: 'status', width: 90, slots: { customRender: 'status' } },
  ];

  const { tableContext } = useListPage({
    tableProps: {
      title: '索引管理',
      api: async (params: Recordable) => {
        const query = { ...params, chunk_id: chunkIdFilter.value || params?.chunk_id };
        if (!kbId.value) {
          return { success: true, result: { records: [], total: 0 } };
        }
        if (query.chunk_id) {
          return listKnowledgeBaseChunkIndexesByChunk(kbId.value, query.chunk_id, query);
        }
        return listKnowledgeBaseChunkIndexes(kbId.value, query);
      },
      columns,
      canResize: true,
      useSearchForm: false,
      actionColumn: {
        width: 220,
      },
      defSort: {
        column: 'createdAt',
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

  function goChunks() {
    router.push(`/super/airag/knowledge-base/${kbId.value}/chunks`);
  }

  function handleCreate() {
    openModal(true, { kbId: kbId.value, chunkId: chunkIdFilter.value, isUpdate: false });
  }

  function handleEdit(record: Recordable) {
    openModal(true, { kbId: kbId.value, chunkId: record.chunk_id, indexId: record.id, record, isUpdate: true });
  }

  function handleDelete(record: Recordable) {
    deleteKnowledgeBaseChunkIndex(record.id).then(() => reload());
  }

  function handleEmbedding(record: Recordable) {
    triggerKnowledgeBaseChunkIndexEmbedding(record.id).then(() => reload());
  }

  function statusColor(text: any) {
    if (text === 'success') return 'green';
    if (text === 'failed') return 'red';
    if (text === 'processing') return 'orange';
    return 'blue';
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
  .index-page {
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
