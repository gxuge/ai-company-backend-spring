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
    <RolePublicModal @register="registerModal" @success="reload" />
  </div>
</template>

<script lang="ts" name="system-tanshi-role-public" setup>
  import { onMounted } from 'vue';
  import { BasicTable, TableAction } from '/@/components/Table';
  import { useListPage } from '/@/hooks/system/useListPage';
  import { useModal } from '/@/components/Modal';
  import {
    approveRolePublic,
    deleteRolePublic,
    getRoleChannelOptions,
    getRolePublicList,
    offlineRolePublic,
    onlineRolePublic,
    rejectRolePublic,
    submitRolePublic,
  } from './rolePublic.api';
  import { columns, rolePublicStatusOptions, searchFormSchema } from './rolePublic.data';
  import RolePublicModal from './components/RolePublicModal.vue';

  const statusColorMap = {
    draft: 'default',
    pending: 'blue',
    online: 'green',
    offline: 'orange',
    rejected: 'red',
  };

  const [registerModal, { openModal }] = useModal();

  const { tableContext } = useListPage({
    designScope: 'tanshi-role-public',
    tableProps: {
      title: '角色公开管理',
      api: getRolePublicList,
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
    const channelOptions = await getRoleChannelOptions();
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
    await submitRolePublic({ id: record.id });
    reload();
  }

  async function handleApprove(record) {
    await approveRolePublic({ id: record.id });
    reload();
  }

  async function handleReject(record) {
    await rejectRolePublic({ id: record.id });
    reload();
  }

  async function handleOnline(record) {
    await onlineRolePublic({ id: record.id });
    reload();
  }

  async function handleOffline(record) {
    await offlineRolePublic({ id: record.id });
    reload();
  }

  async function handleDelete(record) {
    await deleteRolePublic({ id: record.id }, reload);
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
    return rolePublicStatusOptions.find((item) => item.value === value)?.label || value || '-';
  }
</script>
