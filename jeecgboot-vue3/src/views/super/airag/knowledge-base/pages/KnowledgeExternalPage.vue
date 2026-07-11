<template>
  <PageWrapper contentFullHeight fixedHeight>
    <div class="external-page">
      <div class="page-header">
        <div>
          <h2 class="title">外部知识库</h2>
          <p class="desc">当前知识库：{{ kbId || '-' }}。这里管理外部/API 知识库连接配置。</p>
        </div>
        <a-space>
          <a-button @click="goWorkspace">返回工作台</a-button>
          <a-button type="primary" @click="handleCreate">新增外部知识库</a-button>
        </a-space>
      </div>

      <BasicTable @register="registerTable">
        <template #enabled="{ text }">
          <a-tag :color="text ? 'green' : 'red'">{{ text ? '启用' : '禁用' }}</a-tag>
        </template>
        <template #action="{ record }">
          <TableAction :actions="getActions(record)" />
        </template>
      </BasicTable>

      <ExternalKnowledgeModal @register="registerModal" @success="reload" />
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
  import ExternalKnowledgeModal from '../components/ExternalKnowledgeModal.vue';
  import {
    deleteExternalKnowledgeBase,
    listExternalKnowledgeBases,
    testExternalKnowledgeBaseConnection,
  } from '../KnowledgeBase.api';

  const route = useRoute();
  const router = useRouter();
  const kbId = computed(() => String(route.params.kbId || ''));
  const [registerModal, { openModal }] = useModal();

  const columns = [
    { title: 'external_kb_id', dataIndex: 'external_kb_id', width: 160 },
    { title: 'name', dataIndex: 'name', width: 180 },
    { title: 'endpoint_url', dataIndex: 'endpoint_url', width: 260, ellipsis: true },
    { title: 'auth_type', dataIndex: 'auth_type', width: 120 },
    { title: 'enabled', dataIndex: 'enabled', width: 90, slots: { customRender: 'enabled' } },
  ];

  const { tableContext } = useListPage({
    tableProps: {
      title: '外部知识库',
      api: async (params: Recordable) => listExternalKnowledgeBases({ ...params, kb_id: kbId.value }),
      columns,
      canResize: true,
      useSearchForm: false,
      actionColumn: {
        width: 220,
      },
    },
  });

  const [registerTable, { reload }] = tableContext;

  onMounted(() => reload());
  watch(() => kbId.value, () => reload());

  function goWorkspace() {
    router.push(`/super/airag/knowledge-base/${kbId.value}`);
  }

  function handleCreate() {
    openModal(true, { isUpdate: false });
  }

  function handleEdit(record: Recordable) {
    openModal(true, { isUpdate: true, id: record.id, record });
  }

  function handleDelete(record: Recordable) {
    deleteExternalKnowledgeBase(record.id).then(() => reload());
  }

  function handleTest(record: Recordable) {
    testExternalKnowledgeBaseConnection(record.id).then(() => reload());
  }

  function getActions(record: Recordable) {
    return [
      { label: '测试连接', onClick: handleTest.bind(null, record) },
      { label: '编辑', onClick: handleEdit.bind(null, record) },
      {
        label: '删除',
        popConfirm: { title: '是否确认删除', confirm: handleDelete.bind(null, record) },
      },
    ];
  }
</script>

<style scoped lang="less">
  .external-page {
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

