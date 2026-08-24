<template>
  <div class="points-page">
    <a-tabs v-model:activeKey="activeTab" destroyInactiveTabPane @change="handleTabChange">
      <a-tab-pane key="accounts" tab="积分账户">
        <BasicTable v-if="activeTab === 'accounts'" @register="registerAccountTable">
          <template #balance="{ text }">{{ formatPoints(text) }}</template>
          <template #totalIncome="{ text }">{{ formatPoints(text) }}</template>
          <template #totalExpense="{ text }">{{ formatPoints(text) }}</template>
          <template #action="{ record }">
            <TableAction :actions="[{ label: '调整积分', onClick: () => openAdjustModal(record) }]" />
          </template>
        </BasicTable>
      </a-tab-pane>

      <a-tab-pane key="transactions" tab="积分流水">
        <BasicTable v-if="activeTab === 'transactions'" @register="registerTransactionTable">
          <template #bizType="{ text }">{{ getOptionLabel(pointsBizTypeOptions, text) }}</template>
          <template #direction="{ text }">
            <a-tag :color="text === 'INCOME' ? 'success' : 'error'">
              {{ getOptionLabel(directionOptions, text) }}
            </a-tag>
          </template>
          <template #amount="{ record }">
            <span :class="record.direction === 'INCOME' ? 'points-income' : 'points-expense'">
              {{ record.direction === 'INCOME' ? '+' : '-' }}{{ formatPoints(record.amount) }}
            </span>
          </template>
          <template #status="{ text }">
            <a-tag :color="transactionStatusColorMap[text] || 'default'">
              {{ getOptionLabel(pointsStatusOptions, text) }}
            </a-tag>
          </template>
        </BasicTable>
      </a-tab-pane>

      <a-tab-pane key="recharge" tab="充值订单">
        <BasicTable v-if="activeTab === 'recharge'" @register="registerRechargeTable">
          <template #amount="{ record }">{{ formatAmount(record.actualAmount, record.currency) }}</template>
          <template #channel="{ text }">{{ getOptionLabel(paymentChannelOptions, text) }}</template>
          <template #status="{ text }">
            <a-tag :color="rechargeStatusColorMap[text] || 'default'">
              {{ getOptionLabel(rechargeStatusOptions, text) }}
            </a-tag>
          </template>
        </BasicTable>
      </a-tab-pane>

      <a-tab-pane key="products" tab="充值商品">
        <div class="config-toolbar">
          <a-button @click="loadPointsProducts">刷新</a-button>
          <a-button type="primary" @click="openProductModal()">新增商品</a-button>
        </div>
        <a-spin :spinning="productLoading">
          <a-table :columns="productColumns" :data-source="pointsProducts" :pagination="false" :scroll="{ x: 1200 }" row-key="id" bordered>
            <template #bodyCell="{ column, record }">
              <template v-if="column.dataIndex === 'originalAmount'">
                {{ formatAmount(record.originalAmount, record.currency) }}
              </template>
              <template v-else-if="column.dataIndex === 'actualAmount'">
                {{ formatAmount(record.actualAmount, record.currency) }}
              </template>
              <template v-else-if="column.dataIndex === 'status'">
                <a-tag :color="record.status === 1 ? 'success' : 'default'">
                  {{ record.status === 1 ? '启用' : '停用' }}
                </a-tag>
              </template>
              <template v-else-if="column.dataIndex === 'action'">
                <a @click="openProductModal(record)">编辑</a>
              </template>
              <template v-else>{{ record[column.dataIndex] ?? '-' }}</template>
            </template>
          </a-table>
        </a-spin>
      </a-tab-pane>

      <a-tab-pane key="giftRules" tab="会员赠送规则">
        <div class="config-toolbar">
          <a-button @click="loadGiftRules">刷新</a-button>
          <a-button type="primary" @click="openGiftRuleModal()">新增规则</a-button>
        </div>
        <a-spin :spinning="giftRuleLoading">
          <a-table :columns="giftRuleColumns" :data-source="giftRuleTableData" :pagination="false" :scroll="{ x: 820 }" row-key="id" bordered>
            <template #bodyCell="{ column, record }">
              <template v-if="column.dataIndex === 'status'">
                <a-tag :color="record.status === 1 ? 'success' : 'default'">
                  {{ record.status === 1 ? '启用' : '停用' }}
                </a-tag>
              </template>
              <template v-else-if="column.dataIndex === 'action'">
                <a @click="openGiftRuleModal(record)">编辑</a>
              </template>
              <template v-else>{{ record[column.dataIndex] ?? '-' }}</template>
            </template>
          </a-table>
        </a-spin>
      </a-tab-pane>
    </a-tabs>

    <PointsAdjustModal @register="registerAdjustModal" @success="handleAdjustSuccess" />
    <PointsProductModal @register="registerProductModal" @success="handleProductSuccess" />
    <MemberGiftRuleModal @register="registerGiftRuleModal" @success="handleGiftRuleSuccess" />
  </div>
</template>

<script lang="ts" setup>
  import { computed, onMounted, ref } from 'vue';
  import { BasicTable, TableAction } from '/@/components/Table';
  import { useListPage } from '/@/hooks/system/useListPage';
  import { useMessage } from '/@/hooks/web/useMessage';
  import { useModal } from '/@/components/Modal';
  import {
    getMemberConfig,
    listMemberGiftRules,
    listPointsProducts,
    pagePointsAccounts,
    pagePointsRechargeOrders,
    pagePointsTransactions,
    type MemberConfig,
    type MemberGiftRule,
    type PointsProduct,
  } from './points.api';
  import {
    accountColumns,
    accountSearchFormSchema,
    directionOptions,
    getOptionLabel,
    giftRuleColumns,
    paymentChannelOptions,
    pointsBizTypeOptions,
    pointsStatusOptions,
    productColumns,
    rechargeColumns,
    rechargeSearchFormSchema,
    rechargeStatusOptions,
    transactionColumns,
    transactionSearchFormSchema,
  } from './points.data';
  import PointsAdjustModal from './components/PointsAdjustModal.vue';
  import PointsProductModal from './components/PointsProductModal.vue';
  import MemberGiftRuleModal from './components/MemberGiftRuleModal.vue';

  defineOptions({ name: 'SystemTanshiPoints' });

  const { createMessage } = useMessage();
  const activeTab = ref('accounts');
  const productLoading = ref(false);
  const giftRuleLoading = ref(false);
  const pointsProducts = ref<PointsProduct[]>([]);
  const giftRules = ref<MemberGiftRule[]>([]);
  const memberConfig = ref<MemberConfig>({});

  const transactionStatusColorMap: Record<string, string> = {
    PENDING: 'processing',
    SUCCESS: 'success',
    FAILED: 'error',
    REFUNDED: 'warning',
    CANCELED: 'default',
  };
  const rechargeStatusColorMap: Record<string, string> = {
    CREATING: 'processing',
    PENDING: 'warning',
    SUCCEEDED: 'success',
    FAILED: 'error',
    CANCELED: 'default',
  };

  const [registerAdjustModal, { openModal: openAdjust }] = useModal();
  const [registerProductModal, { openModal: openProduct }] = useModal();
  const [registerGiftRuleModal, { openModal: openGiftRule }] = useModal();

  const { tableContext: accountTableContext } = useListPage({
    designScope: 'tanshi-points-accounts',
    tableProps: {
      title: '积分账户',
      api: pagePointsAccounts,
      columns: accountColumns,
      formConfig: { labelWidth: 90, schemas: accountSearchFormSchema },
      actionColumn: { width: 100, title: '操作', dataIndex: 'action', slots: { customRender: 'action' } },
      showIndexColumn: true,
      beforeFetch: stripTableSort,
    },
  });
  const [registerAccountTable, { reload: reloadAccountTable }] = accountTableContext;

  const { tableContext: transactionTableContext } = useListPage({
    designScope: 'tanshi-points-transactions',
    tableProps: {
      title: '积分流水',
      api: pagePointsTransactions,
      columns: transactionColumns,
      formConfig: {
        labelWidth: 90,
        schemas: transactionSearchFormSchema,
        fieldMapToTime: [['timeRange', ['startTime', 'endTime'], 'YYYY-MM-DD HH:mm:ss']],
      },
      showActionColumn: false,
      beforeFetch: stripTableSort,
    },
  });
  const [registerTransactionTable, { reload: reloadTransactionTable }] = transactionTableContext;

  const { tableContext: rechargeTableContext } = useListPage({
    designScope: 'tanshi-points-recharge',
    tableProps: {
      title: '积分充值订单',
      api: pagePointsRechargeOrders,
      columns: rechargeColumns,
      formConfig: {
        labelWidth: 90,
        schemas: rechargeSearchFormSchema,
        fieldMapToTime: [['timeRange', ['startTime', 'endTime'], 'YYYY-MM-DD HH:mm:ss']],
      },
      showActionColumn: false,
      beforeFetch: stripTableSort,
    },
  });
  const [registerRechargeTable] = rechargeTableContext;

  const planOptions = computed(() =>
    (memberConfig.value.plans || [])
      .filter((item) => typeof item.id === 'number')
      .map((item) => ({
        label: `${item.name || item.code || '-'}（ID：${item.id}）`,
        value: item.id as number,
      }))
  );

  const memberProductOptions = computed(() =>
    (memberConfig.value.products || [])
      .filter((item) => typeof item.id === 'number')
      .map((item) => ({
        label: `${getPlanName(item.planId)} / ${item.cycleType || '-'}（ID：${item.id}）`,
        value: item.id as number,
      }))
  );

  const giftRuleTableData = computed(() =>
    giftRules.value.map((item) => ({
      ...item,
      planName: getPlanName(item.planId),
      productName: item.productId && item.productId > 0 ? getProductName(item.productId) : '等级默认规则',
    }))
  );

  function unwrapResult<T>(value: any): T {
    return value && Object.prototype.hasOwnProperty.call(value, 'result') ? value.result : value;
  }

  function formatPoints(value?: number) {
    return value === null || value === undefined ? '-' : Number(value).toLocaleString();
  }

  function formatAmount(value?: number, currency?: string) {
    if (value === null || value === undefined) {
      return '-';
    }
    return `${value} ${currency || ''}`.trim();
  }

  function getPlanName(planId?: number) {
    const plan = memberConfig.value.plans?.find((item) => item.id === planId);
    return plan ? `${plan.name || plan.code || '-'}（ID：${plan.id}）` : planId ? `等级 ${planId}` : '-';
  }

  function getProductName(productId?: number) {
    const product = memberConfig.value.products?.find((item) => item.id === productId);
    return product ? `${getPlanName(product.planId)} / ${product.cycleType || '-'}（ID：${product.id}）` : `套餐 ${productId}`;
  }

  function stripTableSort(params: Recordable) {
    delete params.column;
    delete params.order;
    return params;
  }

  function openAdjustModal(record) {
    openAdjust(true, { record });
  }

  function openProductModal(record?: PointsProduct) {
    openProduct(true, { record });
  }

  function openGiftRuleModal(record?: MemberGiftRule) {
    if (!planOptions.value.length) {
      createMessage.warning('暂无可配置的会员等级');
      return;
    }
    openGiftRule(true, {
      record,
      planOptions: planOptions.value,
      productOptions: memberProductOptions.value,
    });
  }

  async function loadMemberOptions() {
    memberConfig.value = unwrapResult<MemberConfig>(await getMemberConfig()) || {};
  }

  async function loadPointsProducts() {
    productLoading.value = true;
    try {
      pointsProducts.value = unwrapResult<PointsProduct[]>(await listPointsProducts()) || [];
    } finally {
      productLoading.value = false;
    }
  }

  async function loadGiftRules() {
    giftRuleLoading.value = true;
    try {
      giftRules.value = unwrapResult<MemberGiftRule[]>(await listMemberGiftRules()) || [];
    } finally {
      giftRuleLoading.value = false;
    }
  }

  async function handleAdjustSuccess() {
    createMessage.success('积分调整成功');
    await Promise.all([reloadAccountTable(), reloadTransactionTable()]);
  }

  async function handleProductSuccess() {
    createMessage.success('积分商品保存成功');
    await loadPointsProducts();
  }

  async function handleGiftRuleSuccess() {
    createMessage.success('会员赠送规则保存成功');
    await loadGiftRules();
  }

  function handleTabChange(key: string) {
    if (key === 'products' && !pointsProducts.value.length) {
      loadPointsProducts();
    }
    if (key === 'giftRules' && !giftRules.value.length) {
      loadGiftRules();
    }
  }

  onMounted(async () => {
    await Promise.all([loadMemberOptions(), loadPointsProducts(), loadGiftRules()]);
  });
</script>

<style lang="less" scoped>
  .points-page {
    min-height: calc(100vh - 112px);
    padding: 16px;
    background: @component-background;
  }

  .config-toolbar {
    display: flex;
    justify-content: flex-end;
    gap: 8px;
    margin-bottom: 12px;
  }

  .points-income {
    color: #389e0d;
  }

  .points-expense {
    color: #cf1322;
  }
</style>
