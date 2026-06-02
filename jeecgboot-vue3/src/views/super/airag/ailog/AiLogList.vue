<template>
  <div class="p-4">
    <BasicTable @register="registerTable">
      <template #hasRepair="{ text }">
        <a-tag :color="text ? 'orange' : 'default'">{{ text ? '已修复' : '否' }}</a-tag>
      </template>
      <template #status="{ text }">
        <a-tag :color="text === 'success' ? 'green' : text === 'failed' ? 'red' : 'blue'">{{ text || '-' }}</a-tag>
      </template>
      <template #action="{ record }">
        <TableAction :actions="getActions(record)" />
      </template>
    </BasicTable>
    <AiLogDetailDrawer @register="registerDrawer" />
  </div>
</template>

<script lang="ts" name="super-airag-ailog" setup>
  import { BasicTable, TableAction } from '/@/components/Table';
  import { useDrawer } from '/@/components/Drawer';
  import { useListPage } from '/@/hooks/system/useListPage';
  import { columns, searchFormSchema } from './ailog.data';
  import { getAiLogList } from './ailog.api';
  import AiLogDetailDrawer from './components/AiLogDetailDrawer.vue';

  const [registerDrawer, { openDrawer }] = useDrawer();

  const { tableContext } = useListPage({
    designScope: 'airag-ailog-template',
    tableProps: {
      title: 'AI调用监控',
      api: getAiLogList,
      columns,
      formConfig: {
        labelWidth: 120,
        schemas: searchFormSchema,
      },
      actionColumn: {
        width: 120,
        title: '操作',
        dataIndex: 'action',
        slots: { customRender: 'action' },
      },
    },
  });

  const [registerTable] = tableContext;

  function getActions(record) {
    return [
      {
        label: '详情',
        onClick: () => openDrawer(true, { id: record.id }),
      },
    ];
  }
</script>
