<template>
  <div class="etl-page">
    <a-tabs v-model:activeKey="activeTab" destroyInactiveTabPane>
      <a-tab-pane key="tasks" tab="ETL 任务">
        <BasicTable v-if="activeTab === 'tasks'" @register="registerTaskTable">
          <template #tableTitle>
            <a-button type="primary" @click="openTaskEditor()">新增任务</a-button>
          </template>
          <template #recommendType="{ text }">{{ optionLabel(recommendTypeOptions, text) }}</template>
          <template #range="{ record }">{{ formatRange(record) }}</template>
          <template #ratio="{ record }">{{ formatRatio(record) }}</template>
          <template #storageType="{ text }">{{ optionLabel(storageTypeOptions, text) }}</template>
          <template #enabled="{ text }">
            <a-tag :color="text === 1 ? 'success' : 'default'">{{ text === 1 ? '启用' : '停用' }}</a-tag>
          </template>
          <template #running="{ text }">
            <a-tag :color="text ? 'processing' : 'default'">{{ text ? '执行中' : '空闲' }}</a-tag>
          </template>
          <template #action="{ record }">
            <TableAction :actions="taskActions(record)" />
          </template>
        </BasicTable>
      </a-tab-pane>

      <a-tab-pane key="executions" tab="执行记录">
        <BasicTable v-if="activeTab === 'executions'" @register="registerExecutionTable">
          <template #recommendType="{ text }">{{ optionLabel(recommendTypeOptions, text) }}</template>
          <template #triggerType="{ text }">{{ optionLabel(triggerTypeOptions, text) }}</template>
          <template #status="{ text }">
            <a-tag :color="statusColorMap[text] || 'default'">{{ optionLabel(executionStatusOptions, text) }}</a-tag>
          </template>
          <template #duration="{ text }">{{ formatDuration(text) }}</template>
          <template #action="{ record }">
            <TableAction :actions="[{ label: '详情', onClick: () => openExecutionDetail(record.id) }]" />
          </template>
        </BasicTable>
      </a-tab-pane>
    </a-tabs>

    <EtlTaskModal @register="registerTaskModal" @success="reloadTaskTable" />
    <EtlExecutionDetailDrawer @register="registerExecutionDrawer" />
  </div>
</template>

<script lang="ts" setup>
  import { ref } from 'vue';
  import { useDrawer } from '/@/components/Drawer';
  import { useModal } from '/@/components/Modal';
  import { BasicTable, TableAction } from '/@/components/Table';
  import { useListPage } from '/@/hooks/system/useListPage';
  import { useMessage } from '/@/hooks/web/useMessage';
  import { deleteEtlTask, executeEtlTask, pageEtlExecutions, pageEtlTasks, toggleEtlTask, type EtlId, type EtlTask } from './recommendEtl.api';
  import {
    etlExecutionColumns,
    etlExecutionSearchSchema,
    etlTaskColumns,
    etlTaskSearchSchema,
    executionStatusOptions,
    optionLabel,
    recommendTypeOptions,
    storageTypeOptions,
    triggerTypeOptions,
  } from './recommendEtl.data';
  import EtlExecutionDetailDrawer from './components/EtlExecutionDetailDrawer.vue';
  import EtlTaskModal from './components/EtlTaskModal.vue';

  defineOptions({ name: 'SystemTanshiRecommendEtl' });

  const activeTab = ref('tasks');
  const { createMessage } = useMessage();
  const [registerTaskModal, { openModal: openTaskModal }] = useModal();
  const [registerExecutionDrawer, { openDrawer: openExecutionDrawer }] = useDrawer();
  const statusColorMap: Record<string, string> = {
    WAITING: 'warning',
    RUNNING: 'processing',
    SUCCESS: 'success',
    FAILED: 'error',
  };

  const { tableContext: taskTableContext } = useListPage({
    designScope: 'tanshi-recommend-etl-tasks',
    tableProps: {
      title: '推荐 ETL 任务',
      api: pageEtlTasks,
      columns: etlTaskColumns,
      formConfig: { labelWidth: 90, schemas: etlTaskSearchSchema },
      actionColumn: { width: 245, title: '操作', dataIndex: 'action', slots: { customRender: 'action' } },
      beforeFetch: stripTableSort,
    },
  });
  const [registerTaskTable, { reload: reloadTaskTable }] = taskTableContext;

  const { tableContext: executionTableContext } = useListPage({
    designScope: 'tanshi-recommend-etl-executions',
    tableProps: {
      title: 'ETL 执行记录',
      api: pageEtlExecutions,
      columns: etlExecutionColumns,
      formConfig: { labelWidth: 90, schemas: etlExecutionSearchSchema },
      actionColumn: { width: 90, title: '操作', dataIndex: 'action', slots: { customRender: 'action' } },
      beforeFetch: stripTableSort,
    },
  });
  const [registerExecutionTable, { reload: reloadExecutionTable }] = executionTableContext;

  function stripTableSort(params: Recordable) {
    delete params.column;
    delete params.order;
    return params;
  }

  function openTaskEditor(record?: EtlTask) {
    openTaskModal(true, { record });
  }

  function openExecutionDetail(id?: EtlId) {
    if (id !== undefined) {
      openExecutionDrawer(true, { id });
    }
  }

  function taskActions(record: EtlTask) {
    const actions: any[] = [
      { label: '编辑', onClick: () => openTaskEditor(record) },
      {
        label: '执行',
        disabled: !!record.runningExecutionId,
        popConfirm: {
          title: '确认立即执行该 ETL 任务？',
          confirm: () => handleExecute(record),
        },
      },
      {
        label: record.enabled === 1 ? '停用' : '启用',
        popConfirm: {
          title: `确认${record.enabled === 1 ? '停用' : '启用'}该任务？`,
          confirm: () => handleToggle(record),
        },
      },
      {
        label: '删除',
        color: 'error',
        disabled: !!record.runningExecutionId,
        popConfirm: {
          title: '删除后不可恢复，确认删除？',
          confirm: () => handleDelete(record),
        },
      },
    ];
    return actions;
  }

  async function handleExecute(record: EtlTask) {
    if (record.id === undefined) {
      return;
    }
    await executeEtlTask({ id: record.id });
    createMessage.success('任务已进入执行队列');
    activeTab.value = 'executions';
    await reloadTaskTable();
    setTimeout(() => reloadExecutionTable(), 0);
  }

  async function handleToggle(record: EtlTask) {
    if (record.id === undefined) {
      return;
    }
    await toggleEtlTask({ id: record.id, enabled: record.enabled === 1 ? 0 : 1 });
    createMessage.success(record.enabled === 1 ? '任务已停用' : '任务已启用');
    await reloadTaskTable();
  }

  async function handleDelete(record: EtlTask) {
    if (record.id === undefined) {
      return;
    }
    await deleteEtlTask({ id: record.id });
    createMessage.success('任务已删除');
    await reloadTaskTable();
  }

  function formatRange(record: EtlTask) {
    return record.timeRangeMode === 'FIXED' ? `${record.startTime || '-'} 至 ${record.endTime || '-'}` : `最近 ${record.recentDays || '-'} 天`;
  }

  function formatRatio(record: EtlTask) {
    return `${Number(record.trainRatio || 0) * 100}% / ${Number(record.evalRatio || 0) * 100}%`;
  }

  function formatDuration(value?: number) {
    if (value === null || value === undefined) {
      return '-';
    }
    if (value < 1000) {
      return `${value} ms`;
    }
    return `${(value / 1000).toFixed(1)} s`;
  }
</script>

<style lang="less" scoped>
  .etl-page {
    min-height: calc(100vh - 112px);
    padding: 16px;
    background: @component-background;
  }
</style>
