<template>
  <div class="billing-page">
    <div class="summary-grid">
      <a-card title="平台现金收入" size="small">
        <div class="summary-value income">{{ formatMoney(summary.moneyIncome) }}</div>
      </a-card>
      <a-card title="平台现金支出" size="small">
        <div class="summary-value expense">{{ formatMoney(summary.moneyExpense) }}</div>
      </a-card>
      <a-card title="平台积分收入" size="small">
        <div class="summary-value income">{{ formatPoints(summary.pointsIncome) }}</div>
      </a-card>
      <a-card title="平台积分支出" size="small">
        <div class="summary-value expense">{{ formatPoints(summary.pointsExpense) }}</div>
      </a-card>
      <a-card title="账单记录数" size="small">
        <div class="summary-value">{{ formatPoints(summary.recordCount) }}</div>
      </a-card>
    </div>

    <BasicTable @register="registerTable">
      <template #recordType="{ text }">{{ getOptionLabel(categoryOptions, text) }}</template>
      <template #money="{ record }">
        <span v-if="record.moneyDirection !== 'NONE'" :class="record.moneyDirection === 'INCOME' ? 'income' : 'expense'">
          {{ record.moneyDirection === 'INCOME' ? '+' : '-' }}{{ formatMoney(record.moneyAmount) }}
        </span>
        <span v-else>-</span>
      </template>
      <template #points="{ record }">
        <span v-if="record.pointsDirection !== 'NONE'" :class="record.pointsDirection === 'INCOME' ? 'income' : 'expense'">
          {{ record.pointsDirection === 'INCOME' ? '+' : '-' }}{{ formatPoints(record.pointsAmount) }}
        </span>
        <span v-else>-</span>
      </template>
      <template #status="{ text }">
        <a-tag :color="statusColorMap[text] || 'default'">{{ text || '-' }}</a-tag>
      </template>
      <template #action="{ record }">
        <TableAction
          :actions="[
            {
              label: '详情',
              onClick: () => openBillDetail(record),
            },
          ]"
        />
      </template>
    </BasicTable>

    <BillingDetailDrawer @register="registerDrawer" />
  </div>
</template>

<script lang="ts" setup>
  import { onMounted, reactive } from 'vue';
  import { BasicTable, TableAction } from '/@/components/Table';
  import { useListPage } from '/@/hooks/system/useListPage';
  import { useDrawer } from '/@/components/Drawer';
  import { pagePlatformBills, summarizePlatformBills, type BillingQuery, type BillingSummary } from './billing.api';
  import { billingColumns, billingSearchFormSchema, categoryOptions, getOptionLabel } from './billing.data';
  import BillingDetailDrawer from './components/BillingDetailDrawer.vue';

  defineOptions({ name: 'SystemTanshiBilling' });

  const summary = reactive<BillingSummary>({
    moneyIncome: 0,
    moneyExpense: 0,
    pointsIncome: 0,
    pointsExpense: 0,
    recordCount: 0,
  });
  const statusColorMap: Record<string, string> = {
    CREATING: 'processing',
    PENDING: 'warning',
    SUCCEEDED: 'success',
    SUCCESS: 'success',
    FAILED: 'error',
    REFUNDED: 'warning',
    CANCELED: 'default',
  };

  const [registerDrawer, { openDrawer }] = useDrawer();
  const { tableContext } = useListPage({
    designScope: 'tanshi-billing',
    tableProps: {
      title: '平台统一账单',
      api: pagePlatformBills,
      columns: billingColumns,
      formConfig: {
        labelWidth: 90,
        schemas: billingSearchFormSchema,
        fieldMapToTime: [['timeRange', ['startTime', 'endTime'], 'YYYY-MM-DD HH:mm:ss']],
      },
      actionColumn: { width: 80, title: '操作', dataIndex: 'action', slots: { customRender: 'action' } },
      showIndexColumn: true,
      beforeFetch: loadSummary,
    },
  });
  const [registerTable] = tableContext;

  function formatMoney(value?: number) {
    return value === null || value === undefined ? '-' : Number(value).toFixed(2);
  }

  function formatPoints(value?: number) {
    return value === null || value === undefined ? '-' : Number(value).toLocaleString();
  }

  function openBillDetail(record) {
    openDrawer(true, {
      recordType: record.recordType,
      recordId: record.recordId,
    });
  }

  async function loadSummary(params: BillingQuery) {
    const result: any = await summarizePlatformBills(params);
    const value = result?.result ?? result;
    Object.assign(summary, value || {});
    return params;
  }

  onMounted(() => {
    loadSummary({ pageNo: 1, pageSize: 10 });
  });
</script>

<style lang="less" scoped>
  .billing-page {
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

  .income {
    color: #389e0d;
  }

  .expense {
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
