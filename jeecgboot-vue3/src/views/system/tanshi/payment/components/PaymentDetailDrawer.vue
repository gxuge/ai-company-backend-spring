<template>
  <BasicDrawer @register="registerDrawer" title="支付流水详情" width="900">
    <a-spin :spinning="loading">
      <a-descriptions bordered :column="2" size="small">
        <a-descriptions-item label="会员订单号">{{ detail.orderNo || '-' }}</a-descriptions-item>
        <a-descriptions-item label="支付流水ID">{{ detail.id || '-' }}</a-descriptions-item>
        <a-descriptions-item label="用户"> {{ detail.realname || '-' }}（{{ detail.username || detail.userId || '-' }}） </a-descriptions-item>
        <a-descriptions-item label="会员套餐"> {{ detail.planName || '-' }} / {{ detail.cycleType || '-' }} </a-descriptions-item>
        <a-descriptions-item label="支付渠道">{{ formatProvider(detail.provider) }}</a-descriptions-item>
        <a-descriptions-item label="支付状态">
          <a-tag :color="statusColorMap[detail.paymentStatus] || 'default'">
            {{ formatPaymentStatus(detail.paymentStatus) }}
          </a-tag>
        </a-descriptions-item>
        <a-descriptions-item label="支付金额">{{ formatAmount(detail.amount, detail.currency) }}</a-descriptions-item>
        <a-descriptions-item label="订单状态">{{ formatOrderStatus(detail.orderStatus) }}</a-descriptions-item>
        <a-descriptions-item label="支付意图ID" :span="2">
          {{ detail.paymentIntentId || '-' }}
        </a-descriptions-item>
        <a-descriptions-item label="渠道交易ID" :span="2">
          {{ detail.transactionId || '-' }}
        </a-descriptions-item>
        <a-descriptions-item label="创建时间">{{ detail.createdAt || '-' }}</a-descriptions-item>
        <a-descriptions-item label="支付时间">{{ detail.payTime || '-' }}</a-descriptions-item>
        <a-descriptions-item label="回调时间" :span="2">{{ detail.callbackTime || '-' }}</a-descriptions-item>
      </a-descriptions>

      <div class="response-title">渠道响应（已脱敏）</div>
      <pre class="response-content">{{ detail.rawResponse || '-' }}</pre>
    </a-spin>
  </BasicDrawer>
</template>

<script lang="ts" setup>
  import { reactive, ref } from 'vue';
  import { BasicDrawer, useDrawerInner } from '/@/components/Drawer';
  import { getPaymentDetail } from '../payment.api';
  import { paymentProviderOptions, paymentStatusOptions } from '../payment.data';

  const loading = ref(false);
  const detail = reactive<any>({});
  const statusColorMap = {
    CREATING: 'processing',
    PENDING: 'warning',
    SUCCEEDED: 'success',
    FAILED: 'error',
    CANCELED: 'default',
  };

  const [registerDrawer] = useDrawerInner(async (data) => {
    loading.value = true;
    try {
      Object.keys(detail).forEach((key) => delete detail[key]);
      Object.assign(detail, await getPaymentDetail({ id: data.id }));
    } finally {
      loading.value = false;
    }
  });

  function formatProvider(value) {
    return paymentProviderOptions.find((item) => item.value === value)?.label || value || '-';
  }

  function formatPaymentStatus(value) {
    return paymentStatusOptions.find((item) => item.value === value)?.label || value || '-';
  }

  function formatOrderStatus(value) {
    return { 0: '待支付', 1: '支付成功', 2: '已退款' }[value] || '-';
  }

  function formatAmount(amount, currency) {
    if (amount === null || amount === undefined) {
      return '-';
    }
    return `${amount} ${currency || ''}`.trim();
  }
</script>

<style scoped>
  .response-title {
    margin: 18px 0 8px;
    font-weight: 600;
  }

  .response-content {
    max-height: 420px;
    margin: 0;
    padding: 16px;
    overflow: auto;
    color: rgba(0, 0, 0, 0.85);
    white-space: pre-wrap;
    overflow-wrap: anywhere;
    background: #f5f5f5;
    border: 1px solid #d9d9d9;
    border-radius: 4px;
  }
</style>
