<template>
  <div class="p-4">
    <BasicTable @register="registerTable">
      <template #tableTitle>
        <a-button type="primary" @click="openEditor()">手动开通会员</a-button>
      </template>
      <template #status="{ text }"
        ><a-tag :color="text === 1 ? 'success' : 'default'">{{ text === 1 ? '有效' : '失效' }}</a-tag></template
      >
      <template #autoRenew="{ text }">{{ text === 1 ? '开启' : '关闭' }}</template>
      <template #action="{ record }"><TableAction :actions="getActions(record)" :dropDownActions="getMoreActions(record)" /></template>
    </BasicTable>
    <MembershipModal @register="registerModal" @success="reload" />
    <MembershipDetailDrawer @register="registerDrawer" />
  </div>
</template>

<script lang="ts" name="super-airag-user-membership" setup>
  import { BasicTable, TableAction } from '/@/components/Table';
  import { useListPage } from '/@/hooks/system/useListPage';
  import { useModal } from '/@/components/Modal';
  import { useDrawer } from '/@/components/Drawer';
  import { columns } from './userMembership.data';
  import { deleteMembership, pageMemberships } from './userMembership.api';
  import MembershipModal from './components/MembershipModal.vue';
  import MembershipDetailDrawer from './components/MembershipDetailDrawer.vue';

  const [registerModal, { openModal }] = useModal();
  const [registerDrawer, { openDrawer }] = useDrawer();
  const { tableContext } = useListPage({
    designScope: 'airag-user-membership',
    tableProps: {
      title: '用户会员管理',
      api: pageMemberships,
      columns,
      formConfig: {
        labelWidth: 100,
        schemas: [
          { field: 'keyword', label: '用户关键词', component: 'Input', colProps: { span: 8 } },
          {
            field: 'status',
            label: '会员状态',
            component: 'Select',
            componentProps: {
              options: [
                { label: '有效', value: 1 },
                { label: '失效', value: 0 },
              ],
            },
            colProps: { span: 8 },
          },
        ],
      },
      actionColumn: { width: 170, title: '操作', dataIndex: 'action', slots: { customRender: 'action' } },
    },
  });
  const [registerTable, { reload }] = tableContext;
  function openEditor(record?) {
    openModal(true, { record });
  }
  function getActions(record) {
    return [
      { label: '详情', onClick: () => openDrawer(true, { id: record.id }) },
      { label: '编辑', onClick: () => openEditor(record) },
    ];
  }
  function getMoreActions(record) {
    return [
      {
        label: '删除',
        popConfirm: {
          title: '确认删除该用户会员记录？',
          confirm: async () => {
            await deleteMembership({ id: record.id });
            reload();
          },
        },
      },
    ];
  }
</script>
