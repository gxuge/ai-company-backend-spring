<template>
  <div class="p-4">
    <BasicTable @register="registerTable">
      <template #tableTitle>
        <a-button type="primary" preIcon="ant-design:plus-outlined" @click="handleAdd">新增</a-button>
      </template>
      <template #targetType="{ text }">
        <a-tag>{{ formatTargetType(text) }}</a-tag>
      </template>
      <template #channelImage="{ text }">
        <a-image v-if="text" :src="text" :width="48" :height="48" style="object-fit: cover; border-radius: 8px" />
        <span v-else>-</span>
      </template>
      <template #status="{ text }">
        <a-tag :color="text === 'enabled' ? 'green' : 'red'">{{ formatChannelStatus(text) }}</a-tag>
      </template>
      <template #action="{ record }">
        <TableAction :actions="getActions(record)" />
      </template>
    </BasicTable>
    <ChannelModal @register="registerModal" @success="reload" />
  </div>
</template>

<script lang="ts" name="system-tanshi-channel" setup>
  import { BasicTable, TableAction } from '/@/components/Table';
  import { useListPage } from '/@/hooks/system/useListPage';
  import { useModal } from '/@/components/Modal';
  import { columns, searchFormSchema, channelStatusOptions, channelTargetTypeOptions } from './channel.data';
  import { deletePublicChannel, getPublicChannelList } from './channel.api';
  import ChannelModal from './components/ChannelModal.vue';

  const [registerModal, { openModal }] = useModal();

  const { tableContext } = useListPage({
    designScope: 'tanshi-channel',
    tableProps: {
      title: '公开渠道管理',
      api: getPublicChannelList,
      columns,
      formConfig: {
        labelWidth: 100,
        schemas: searchFormSchema,
      },
      actionColumn: {
        width: 180,
        title: '操作',
        dataIndex: 'action',
        slots: { customRender: 'action' },
      },
      showIndexColumn: true,
    },
  });

  const [registerTable, { reload }] = tableContext;

  function handleAdd() {
    openModal(true, { isUpdate: false, viewMode: false });
  }

  function handleEdit(record) {
    openModal(true, { record, isUpdate: true, viewMode: false });
  }

  function handleView(record) {
    openModal(true, { record, isUpdate: true, viewMode: true });
  }

  async function handleDelete(record) {
    await deletePublicChannel({ id: record.id }, reload);
  }

  function getActions(record) {
    return [
      {
        label: '编辑',
        onClick: handleEdit.bind(null, record),
      },
      {
        label: '详情',
        onClick: handleView.bind(null, record),
      },
      {
        label: '删除',
        color: 'error',
        onClick: handleDelete.bind(null, record),
      },
    ];
  }

  function formatTargetType(value) {
    return channelTargetTypeOptions.find((item) => item.value === value)?.label || value || '-';
  }

  function formatChannelStatus(value) {
    return channelStatusOptions.find((item) => item.value === value)?.label || value || '-';
  }
</script>
