<template>
  <div class="p-4">
    <BasicTable @register="registerTable">
      <template #tableTitle>
        <a-button type="primary" preIcon="ant-design:plus-outlined" @click="handleAdd">新增</a-button>
      </template>
      <template #status="{ text }">
        <a-tag :color="statusColorMap[text] || 'default'">{{ formatStatus(text) }}</a-tag>
      </template>
      <template #action="{ record }">
        <TableAction :actions="getActions(record)" :dropDownActions="getDropDownActions(record)" />
      </template>
    </BasicTable>
    <StoryPublicModal @register="registerModal" @success="reload" />
  </div>
</template>

<script lang="ts" name="system-tanshi-story-public" setup>
  import { onMounted } from 'vue';
  import { BasicTable, TableAction } from '/@/components/Table';
  import { useListPage } from '/@/hooks/system/useListPage';
  import { useModal } from '/@/components/Modal';
  import {
    approveStoryPublic,
    deleteStoryPublic,
    getStoryChannelOptions,
    getStoryPublicList,
    offlineStoryPublic,
    onlineStoryPublic,
    rejectStoryPublic,
    submitStoryPublic,
  } from './storyPublic.api';
  import { columns, searchFormSchema, storyPublicStatusOptions } from './storyPublic.data';
  import StoryPublicModal from './components/StoryPublicModal.vue';

  const statusColorMap = {
    draft: 'default',
    pending: 'blue',
    online: 'green',
    offline: 'orange',
    rejected: 'red',
  };

  const [registerModal, { openModal }] = useModal();

  const { tableContext } = useListPage({
    designScope: 'tanshi-story-public',
    tableProps: {
      title: '故事公开管理',
      api: getStoryPublicList,
      columns,
      formConfig: {
        labelWidth: 100,
        schemas: searchFormSchema,
      },
      actionColumn: {
        width: 220,
        title: '操作',
        dataIndex: 'action',
        slots: { customRender: 'action' },
      },
      showIndexColumn: true,
    },
  });

  const [registerTable, { reload, getForm }] = tableContext;

  onMounted(async () => {
    const channelOptions = await getStoryChannelOptions();
    getForm()?.updateSchema?.({
      field: 'channelCode',
      componentProps: {
        options: channelOptions,
        allowClear: true,
        placeholder: '请选择渠道',
      },
    });
  });

  function handleAdd() {
    openModal(true, { isUpdate: false, viewMode: false });
  }

  function handleEdit(record) {
    openModal(true, { record, isUpdate: true, viewMode: false });
  }

  function handleView(record) {
    openModal(true, { record, isUpdate: true, viewMode: true });
  }

  async function handleSubmit(record) {
    await submitStoryPublic({ id: record.id });
    reload();
  }

  async function handleApprove(record) {
    await approveStoryPublic({ id: record.id });
    reload();
  }

  async function handleReject(record) {
    await rejectStoryPublic({ id: record.id });
    reload();
  }

  async function handleOnline(record) {
    await onlineStoryPublic({ id: record.id });
    reload();
  }

  async function handleOffline(record) {
    await offlineStoryPublic({ id: record.id });
    reload();
  }

  async function handleDelete(record) {
    await deleteStoryPublic({ id: record.id }, reload);
  }

  function getActions(record) {
    const actions = [
      {
        label: '编辑',
        onClick: handleEdit.bind(null, record),
      },
      {
        label: '详情',
        onClick: handleView.bind(null, record),
      },
    ];
    if (record.status !== 'online') {
      actions.push({ label: '上架', onClick: handleOnline.bind(null, record) });
    } else {
      actions.push({ label: '下架', onClick: handleOffline.bind(null, record) });
    }
    return actions;
  }

  function getDropDownActions(record) {
    return [
      {
        label: '提交',
        onClick: handleSubmit.bind(null, record),
      },
      {
        label: '审核通过',
        onClick: handleApprove.bind(null, record),
      },
      {
        label: '驳回',
        onClick: handleReject.bind(null, record),
      },
      {
        label: '删除',
        color: 'error',
        onClick: handleDelete.bind(null, record),
      },
    ];
  }

  function formatStatus(value) {
    return storyPublicStatusOptions.find((item) => item.value === value)?.label || value || '-';
  }
</script>
