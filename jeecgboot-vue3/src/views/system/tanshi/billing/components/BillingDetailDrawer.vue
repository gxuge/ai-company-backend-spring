<template>
  <BasicDrawer v-bind="$attrs" @register="registerDrawer" title="平台账单详情" :width="560">
    <a-spin :spinning="loading">
      <a-descriptions v-if="detail" :column="1" bordered size="small">
        <a-descriptions-item label="账单类型">{{ getOptionLabel(categoryOptions, detail.recordType) }}</a-descriptions-item>
        <a-descriptions-item label="账单名称">{{ detail.title || '-' }}</a-descriptions-item>
        <a-descriptions-item label="用户">{{ detail.nickname || '-' }}</a-descriptions-item>
        <a-descriptions-item label="用户ID">{{ detail.userId || '-' }}</a-descriptions-item>
        <a-descriptions-item label="订单/流水号">{{ detail.orderNo || '-' }}</a-descriptions-item>
        <a-descriptions-item label="现金变化">
          {{ formatDirectionAmount(detail.moneyAmount, detail.moneyDirection) }}
        </a-descriptions-item>
        <a-descriptions-item label="积分变化">
          {{ formatDirectionPoints(detail.pointsAmount, detail.pointsDirection) }}
        </a-descriptions-item>
        <a-descriptions-item label="状态">{{ detail.status || '-' }}</a-descriptions-item>
        <a-descriptions-item label="支付渠道">{{ detail.paymentChannel || '-' }}</a-descriptions-item>
        <a-descriptions-item label="原价">{{ formatAmount(detail.originalAmount) }}</a-descriptions-item>
        <a-descriptions-item label="优惠金额">{{ formatAmount(detail.discountAmount) }}</a-descriptions-item>
        <a-descriptions-item label="实付金额">{{ formatAmount(detail.actualAmount) }}</a-descriptions-item>
        <a-descriptions-item label="变动前积分">{{ formatPoints(detail.beforeBalance) }}</a-descriptions-item>
        <a-descriptions-item label="变动后积分">{{ formatPoints(detail.afterBalance) }}</a-descriptions-item>
        <a-descriptions-item label="关联业务ID">{{ detail.relatedBizId || '-' }}</a-descriptions-item>
        <a-descriptions-item label="描述">{{ detail.description || '-' }}</a-descriptions-item>
        <a-descriptions-item label="支付时间">{{ detail.payTime || '-' }}</a-descriptions-item>
        <a-descriptions-item label="创建时间">{{ detail.createdAt || '-' }}</a-descriptions-item>
      </a-descriptions>
      <a-empty v-else description="暂无账单详情" />
    </a-spin>
  </BasicDrawer>
</template>

<script lang="ts" setup>
  import { ref } from 'vue';
  import { BasicDrawer, useDrawerInner } from '/@/components/Drawer';
  import { getPlatformBill, type BillingDetail } from '../billing.api';
  import { categoryOptions, getOptionLabel } from '../billing.data';

  const loading = ref(false);
  const detail = ref<BillingDetail>();

  const [registerDrawer] = useDrawerInner(async (data) => {
    detail.value = undefined;
    if (!data?.recordType || !data?.recordId) {
      return;
    }
    loading.value = true;
    try {
      detail.value = await getPlatformBill({
        recordType: data.recordType,
        recordId: data.recordId,
      });
    } finally {
      loading.value = false;
    }
  });

  function formatPoints(value?: number) {
    return value === null || value === undefined ? '-' : Number(value).toLocaleString();
  }

  function formatAmount(value?: number) {
    if (value === null || value === undefined) {
      return '-';
    }
    return Number(value).toFixed(2);
  }

  function formatDirectionAmount(value?: number, direction?: string) {
    if (value === null || value === undefined || direction === 'NONE') {
      return '-';
    }
    return `${direction === 'INCOME' ? '+' : '-'}${formatAmount(value)}`;
  }

  function formatDirectionPoints(value?: number, direction?: string) {
    if (value === null || value === undefined || direction === 'NONE') {
      return '-';
    }
    return `${direction === 'INCOME' ? '+' : '-'}${formatPoints(value)}`;
  }
</script>
