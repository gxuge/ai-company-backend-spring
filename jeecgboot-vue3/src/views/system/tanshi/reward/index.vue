<template>
  <div class="reward-page">
    <div class="summary-grid">
      <a-card title="待处理" size="small">
        <div class="summary-value pending">{{ formatCount(summary.pendingCount) }}</div>
      </a-card>
      <a-card title="处理中" size="small">
        <div class="summary-value processing">{{ formatCount(summary.processingCount) }}</div>
      </a-card>
      <a-card title="执行成功" size="small">
        <div class="summary-value success">{{ formatCount(summary.successCount) }}</div>
      </a-card>
      <a-card title="执行失败" size="small">
        <div class="summary-value failed">{{ formatCount(summary.failedCount) }}</div>
      </a-card>
      <a-card title="今日事件" size="small">
        <div class="summary-value">{{ formatCount(summary.todayCount) }}</div>
      </a-card>
    </div>

    <BasicTable @register="registerTable">
      <template #eventType="{ text }">
        {{ getRewardOptionLabel(rewardEventTypeOptions, text) }}
      </template>
      <template #status="{ text }">
        <a-tag :color="statusColorMap[text] || 'default'">
          {{ getRewardOptionLabel(rewardEventStatusOptions, text) }}
        </a-tag>
      </template>
      <template #rewardValue="{ text }">{{ formatPoints(text) }}</template>
      <template #retry="{ record }"> {{ record.retryCount ?? 0 }} / {{ record.maxRetryCount ?? 0 }} </template>
      <template #action="{ record }">
        <TableAction :actions="getActions(record)" />
      </template>
    </BasicTable>

    <RewardEventDetailDrawer @register="registerDrawer" />
  </div>
</template>

<script lang="ts" setup>
  import { reactive } from 'vue';
  import { BasicTable, TableAction } from '/@/components/Table';
  import { useDrawer } from '/@/components/Drawer';
  import { useListPage } from '/@/hooks/system/useListPage';
  import { useMessage } from '/@/hooks/web/useMessage';
  import {
    pageRewardEvents,
    retryRewardEvent,
    summarizeRewardEvents,
    type RewardEventItem,
    type RewardEventQuery,
    type RewardEventSummary,
  } from './reward.api';
  import {
    getRewardOptionLabel,
    rewardEventColumns,
    rewardEventSearchFormSchema,
    rewardEventStatusOptions,
    rewardEventTypeOptions,
  } from './reward.data';
  import RewardEventDetailDrawer from './components/RewardEventDetailDrawer.vue';

  defineOptions({ name: 'SystemTanshiReward' });

  const { createMessage } = useMessage();
  const summary = reactive<RewardEventSummary>({
    pendingCount: 0,
    processingCount: 0,
    successCount: 0,
    failedCount: 0,
    todayCount: 0,
  });
  const statusColorMap: Record<string, string> = {
    PENDING: 'warning',
    PROCESSING: 'processing',
    SUCCESS: 'success',
    FAILED: 'error',
  };

  const [registerDrawer, { openDrawer }] = useDrawer();
  const { tableContext } = useListPage({
    designScope: 'tanshi-reward-events',
    tableProps: {
      title: '奖励事件',
      api: pageRewardEvents,
      columns: rewardEventColumns,
      formConfig: {
        labelWidth: 90,
        schemas: rewardEventSearchFormSchema,
        fieldMapToTime: [['timeRange', ['startTime', 'endTime'], 'YYYY-MM-DD HH:mm:ss']],
      },
      actionColumn: {
        width: 145,
        title: '操作',
        dataIndex: 'action',
        slots: { customRender: 'action' },
      },
      showIndexColumn: true,
      beforeFetch: loadSummary,
    },
  });
  const [registerTable, { reload }] = tableContext;

  function formatCount(value?: number) {
    return Number(value || 0).toLocaleString();
  }

  function formatPoints(value?: number) {
    return value === null || value === undefined ? '-' : Number(value).toLocaleString();
  }

  function getActions(record: RewardEventItem) {
    const actions: any[] = [
      {
        label: '详情',
        onClick: () => openDrawer(true, { id: record.id }),
      },
    ];
    if (record.status === 'FAILED' && Number(record.retryCount || 0) < Number(record.maxRetryCount || 0)) {
      actions.push({
        label: '重新执行',
        popConfirm: {
          title: '确认重新执行该奖励事件？',
          confirm: () => handleRetry(record),
        },
      });
    }
    return actions;
  }

  async function loadSummary(params: RewardEventQuery) {
    delete (params as Recordable).column;
    delete (params as Recordable).order;
    const result: any = await summarizeRewardEvents(params);
    Object.assign(summary, result?.result ?? result ?? {});
    return params;
  }

  async function handleRetry(record: RewardEventItem) {
    if (!record.eventId) {
      createMessage.error('奖励事件ID不存在');
      return;
    }
    await retryRewardEvent({ eventId: record.eventId });
    createMessage.success('奖励事件重新执行成功');
    await reload();
  }
</script>

<style lang="less" scoped>
  .reward-page {
    min-height: calc(100vh - 112px);
    padding: 16px;
    background: @component-background;
  }

  .summary-grid {
    display: grid;
    grid-template-columns: repeat(5, minmax(0, 1fr));
    gap: 12px;
    margin-bottom: 16px;
  }

  .summary-value {
    font-size: 24px;
    font-weight: 600;
    line-height: 1.4;
  }

  .pending {
    color: #d48806;
  }

  .processing {
    color: #1677ff;
  }

  .success {
    color: #389e0d;
  }

  .failed {
    color: #cf1322;
  }

  @media (max-width: 1200px) {
    .summary-grid {
      grid-template-columns: repeat(3, minmax(0, 1fr));
    }
  }

  @media (max-width: 720px) {
    .summary-grid {
      grid-template-columns: repeat(2, minmax(0, 1fr));
    }
  }
</style>
