<template>
  <BasicDrawer @register="registerDrawer" title="用户会员详情" width="860">
    <a-spin :spinning="loading">
      <a-descriptions v-if="detail.membership" bordered :column="2" size="small">
        <a-descriptions-item label="用户"
          >{{ detail.membership.realname || '-' }}（{{ detail.membership.username || detail.membership.userId }}）</a-descriptions-item
        >
        <a-descriptions-item label="会员">{{ detail.membership.planName }} / {{ detail.membership.cycleType }}</a-descriptions-item>
        <a-descriptions-item label="生效时间">{{ detail.membership.startTime }}</a-descriptions-item>
        <a-descriptions-item label="到期时间">{{ detail.membership.endTime }}</a-descriptions-item>
      </a-descriptions>
      <a-tabs class="detail-tabs">
        <a-tab-pane key="quota" tab="权益额度">
          <div class="toolbar"><a-button type="primary" @click="openQuota()">新增额度</a-button></div>
          <a-table :columns="quotaColumns" :data-source="detail.quotas" row-key="id" size="small" :pagination="false">
            <template #bodyCell="{ column, record }">
              <a-space v-if="column.key === 'action'">
                <a @click="openQuota(record)">编辑</a>
                <a-popconfirm title="确认删除该额度？" @confirm="removeQuota(record.id)"><a class="danger-link">删除</a></a-popconfirm>
              </a-space>
            </template>
          </a-table>
        </a-tab-pane>
        <a-tab-pane key="orders" tab="订单记录">
          <a-table :columns="orderColumns" :data-source="detail.orders" row-key="id" size="small" />
        </a-tab-pane>
        <a-tab-pane key="usage" tab="使用记录">
          <a-table :columns="usageColumns" :data-source="detail.usageLogs" row-key="id" size="small" />
        </a-tab-pane>
      </a-tabs>
    </a-spin>
    <a-modal v-model:open="quotaVisible" title="编辑权益额度" @ok="submitQuota">
      <a-form :model="quotaForm" :label-col="{ span: 6 }">
        <a-form-item label="权益编码"><a-input v-model:value="quotaForm.benefitCode" :disabled="!!quotaForm.id" /></a-form-item>
        <a-form-item label="总额度"><a-input-number v-model:value="quotaForm.totalAmount" /></a-form-item>
        <a-form-item label="已使用"><a-input-number v-model:value="quotaForm.usedAmount" :min="0" /></a-form-item>
        <a-form-item label="到期时间"><a-date-picker v-model:value="quotaForm.expireTime" show-time /></a-form-item>
      </a-form>
    </a-modal>
  </BasicDrawer>
</template>

<script lang="ts" setup>
  import { reactive, ref } from 'vue';
  import dayjs from 'dayjs';
  import { BasicDrawer, useDrawerInner } from '/@/components/Drawer';
  import { deleteQuota, getMembershipDetail, saveQuota } from '../userMembership.api';

  const detail = reactive<any>({ membership: null, quotas: [], orders: [], usageLogs: [] });
  const loading = ref(false);
  const membershipId = ref<number>();
  const quotaVisible = ref(false);
  const quotaForm = reactive<any>({});
  const quotaColumns = [
    { title: '权益编码', dataIndex: 'benefitCode' },
    { title: '总额度', dataIndex: 'totalAmount' },
    { title: '已使用', dataIndex: 'usedAmount' },
    { title: '到期时间', dataIndex: 'expireTime' },
    { title: '操作', key: 'action', width: 120 },
  ];
  const orderColumns = [
    { title: '订单号', dataIndex: 'orderNo' },
    { title: '金额', dataIndex: 'amount' },
    { title: '状态', dataIndex: 'status' },
    { title: '支付渠道', dataIndex: 'paymentChannel' },
    { title: '支付时间', dataIndex: 'payTime' },
  ];
  const usageColumns = [
    { title: '权益编码', dataIndex: 'benefitCode' },
    { title: '消耗数量', dataIndex: 'consumeAmount' },
    { title: '业务类型', dataIndex: 'bizType' },
    { title: '业务ID', dataIndex: 'bizId' },
    { title: '使用时间', dataIndex: 'createdAt' },
  ];
  const [registerDrawer] = useDrawerInner(async (data) => {
    membershipId.value = data.id;
    await loadDetail();
  });
  async function loadDetail() {
    loading.value = true;
    try {
      Object.assign(detail, await getMembershipDetail({ id: membershipId.value }));
    } finally {
      loading.value = false;
    }
  }
  function openQuota(record?) {
    Object.keys(quotaForm).forEach((key) => delete quotaForm[key]);
    Object.assign(quotaForm, {
      totalAmount: 0,
      usedAmount: 0,
      ...record,
      userId: detail.membership.userId,
      expireTime: record?.expireTime ? dayjs(record.expireTime) : dayjs(detail.membership.endTime),
    });
    quotaVisible.value = true;
  }
  async function submitQuota() {
    await saveQuota({ ...quotaForm, expireTime: quotaForm.expireTime?.format('YYYY-MM-DD HH:mm:ss') });
    quotaVisible.value = false;
    await loadDetail();
  }
  async function removeQuota(id) {
    await deleteQuota({ id });
    await loadDetail();
  }
</script>

<style scoped>
  .detail-tabs {
    margin-top: 16px;
  }
  .toolbar {
    display: flex;
    justify-content: flex-end;
    margin-bottom: 12px;
  }
  .danger-link {
    color: #ff4d4f;
  }
  :deep(.ant-input-number),
  :deep(.ant-picker) {
    width: 100%;
  }
</style>
