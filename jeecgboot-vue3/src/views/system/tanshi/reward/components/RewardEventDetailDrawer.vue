<template>
  <BasicDrawer v-bind="$attrs" @register="registerDrawer" title="奖励事件详情" :width="760">
    <a-spin :spinning="loading">
      <a-descriptions v-if="detail.id" :column="2" bordered size="small">
        <a-descriptions-item label="事件ID" :span="2">{{ detail.eventId || '-' }}</a-descriptions-item>
        <a-descriptions-item label="事件类型">{{ getRewardOptionLabel(rewardEventTypeOptions, detail.eventType) }}</a-descriptions-item>
        <a-descriptions-item label="执行状态">
          <a-tag :color="statusColorMap[detail.status || ''] || 'default'">
            {{ getRewardOptionLabel(rewardEventStatusOptions, detail.status) }}
          </a-tag>
        </a-descriptions-item>
        <a-descriptions-item label="用户"> {{ detail.realname || '-' }}（{{ detail.username || detail.userId || '-' }}） </a-descriptions-item>
        <a-descriptions-item label="用户ID">{{ detail.userId || '-' }}</a-descriptions-item>
        <a-descriptions-item label="业务ID" :span="2">{{ detail.bizId || '-' }}</a-descriptions-item>
        <a-descriptions-item label="执行次数"> {{ detail.retryCount ?? 0 }} / {{ detail.maxRetryCount ?? 0 }} </a-descriptions-item>
        <a-descriptions-item label="奖励数量">{{ formatPoints(detail.rewardValue) }}</a-descriptions-item>
        <a-descriptions-item label="积分流水号" :span="2">{{ detail.pointsTransactionNo || '-' }}</a-descriptions-item>
        <a-descriptions-item label="机器错误码" :span="2">{{ detail.lastErrorCode || '-' }}</a-descriptions-item>
        <a-descriptions-item label="错误信息" :span="2">{{ detail.lastErrorMessage || '-' }}</a-descriptions-item>
        <a-descriptions-item label="创建时间">{{ detail.createdAt || '-' }}</a-descriptions-item>
        <a-descriptions-item label="处理时间">{{ detail.processedAt || '-' }}</a-descriptions-item>
      </a-descriptions>

      <template v-if="detail.id">
        <div class="json-title">事件负载</div>
        <pre class="json-content">{{ formatJson(detail.payloadJson) }}</pre>
        <div class="json-title">执行结果</div>
        <pre class="json-content">{{ formatJson(detail.resultJson) }}</pre>
      </template>
      <a-empty v-else description="暂无奖励事件详情" />
    </a-spin>
  </BasicDrawer>
</template>

<script lang="ts" setup>
  import { ref } from 'vue';
  import { BasicDrawer, useDrawerInner } from '/@/components/Drawer';
  import { getRewardEventDetail, type RewardEventDetail } from '../reward.api';
  import { getRewardOptionLabel, rewardEventStatusOptions, rewardEventTypeOptions } from '../reward.data';

  const loading = ref(false);
  const detail = ref<RewardEventDetail>({});
  const statusColorMap: Record<string, string> = {
    PENDING: 'warning',
    PROCESSING: 'processing',
    SUCCESS: 'success',
    FAILED: 'error',
  };

  const [registerDrawer] = useDrawerInner(async (data) => {
    detail.value = {};
    if (!data?.id) {
      return;
    }
    loading.value = true;
    try {
      detail.value = await getRewardEventDetail({ id: data.id });
    } finally {
      loading.value = false;
    }
  });

  function formatPoints(value?: number) {
    return value === null || value === undefined ? '-' : Number(value).toLocaleString();
  }

  function formatJson(value?: string) {
    if (!value) {
      return '-';
    }
    try {
      return JSON.stringify(JSON.parse(value), null, 2);
    } catch {
      return value;
    }
  }
</script>

<style lang="less" scoped>
  .json-title {
    margin: 18px 0 8px;
    font-weight: 600;
  }

  .json-content {
    max-height: 320px;
    margin: 0;
    padding: 14px;
    overflow: auto;
    color: rgba(0, 0, 0, 0.85);
    white-space: pre-wrap;
    overflow-wrap: anywhere;
    background: #f5f5f5;
    border: 1px solid #d9d9d9;
    border-radius: 4px;
  }
</style>
