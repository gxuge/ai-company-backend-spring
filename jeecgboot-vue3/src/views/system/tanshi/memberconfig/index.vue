<template>
  <div class="member-config-page">
    <a-tabs v-model:activeKey="mainTab">
      <a-tab-pane key="plans" tab="会员等级配置">
        <div class="config-layout">
          <aside class="plan-panel">
            <div class="panel-title">
              <span>会员等级</span>
              <a-button type="primary" size="small" @click="openEditor('plan')">新增</a-button>
            </div>
            <a-spin :spinning="loading">
              <button
                v-for="plan in config.plans"
                :key="plan.id"
                class="plan-item"
                :class="{ active: selectedPlanId === plan.id }"
                @click="selectedPlanId = plan.id"
              >
                <span class="plan-dot" :style="{ background: plan.themeColor || '#1677ff' }"></span>
                <span class="plan-copy"
                  ><strong>{{ plan.name }}</strong
                  ><small>{{ plan.code }}</small></span
                >
                <a-tag :color="plan.status === 1 ? 'success' : 'default'">{{ plan.status === 1 ? '启用' : '停用' }}</a-tag>
              </button>
            </a-spin>
          </aside>
          <main class="detail-panel" v-if="selectedPlan">
            <header class="detail-header">
              <div>
                <h2>{{ selectedPlan.name }}</h2>
                <p>{{ selectedPlan.description || '暂无会员说明' }}</p>
              </div>
              <a-space>
                <a-button @click="openEditor('plan', selectedPlan)">编辑等级</a-button>
                <a-popconfirm title="确认删除该会员等级？" @confirm="removeItem('plan', selectedPlan.id)">
                  <a-button danger>删除</a-button>
                </a-popconfirm>
              </a-space>
            </header>
            <a-tabs v-model:activeKey="detailTab">
              <a-tab-pane key="products" tab="套餐配置">
                <div class="table-toolbar"><a-button type="primary" @click="openEditor('product')">新增套餐</a-button></div>
                <a-table :columns="productColumns" :data-source="currentProducts" row-key="id" :pagination="false">
                  <template #bodyCell="{ column, record }"><TableCell :column="column" :record="record" type="product" /></template>
                </a-table>
              </a-tab-pane>
              <a-tab-pane key="benefits" tab="权益配置">
                <div class="table-toolbar"><a-button type="primary" @click="openEditor('planBenefit')">添加权益</a-button></div>
                <a-table :columns="relationColumns" :data-source="currentRelations" row-key="id" :pagination="false">
                  <template #bodyCell="{ column, record }"><TableCell :column="column" :record="record" type="planBenefit" /></template>
                </a-table>
              </a-tab-pane>
              <a-tab-pane key="gifts" tab="开通赠礼">
                <div class="table-toolbar"><a-button type="primary" @click="openEditor('gift')">新增赠礼</a-button></div>
                <a-table :columns="giftColumns" :data-source="currentGifts" row-key="id" :pagination="false">
                  <template #bodyCell="{ column, record }"><TableCell :column="column" :record="record" type="gift" /></template>
                </a-table>
              </a-tab-pane>
            </a-tabs>
          </main>
          <a-empty v-else class="empty-panel" description="请先新增会员等级" />
        </div>
      </a-tab-pane>
      <a-tab-pane key="benefitLibrary" tab="权益库">
        <div class="library-toolbar"><a-button type="primary" @click="openEditor('benefit')">新增权益</a-button></div>
        <a-table :columns="benefitColumns" :data-source="config.benefits" row-key="id">
          <template #bodyCell="{ column, record }"><TableCell :column="column" :record="record" type="benefit" /></template>
        </a-table>
      </a-tab-pane>
    </a-tabs>
    <MemberConfigModal @register="registerModal" @success="loadConfig" />
  </div>
</template>

<script lang="tsx" setup>
  import { computed, defineComponent, onMounted, reactive, ref } from 'vue';
  import { useModal } from '/@/components/Modal';
  import { useMessage } from '/@/hooks/web/useMessage';
  import MemberConfigModal from './components/MemberConfigModal.vue';
  import { deleteMemberConfig, getMemberConfig } from './memberConfig.api';

  const { createMessage } = useMessage();
  const [registerModal, { openModal }] = useModal();
  const loading = ref(false);
  const mainTab = ref('plans');
  const detailTab = ref('products');
  const selectedPlanId = ref<number>();
  const config = reactive<any>({ plans: [], products: [], benefits: [], planBenefits: [], gifts: [] });
  const selectedPlan = computed(() => config.plans.find((item) => item.id === selectedPlanId.value));
  const currentProducts = computed(() => config.products.filter((item) => item.planId === selectedPlanId.value));
  const currentRelations = computed(() =>
    config.planBenefits
      .filter((item) => item.planId === selectedPlanId.value)
      .map((item) => ({ ...item, benefitName: config.benefits.find((benefit) => benefit.id === item.benefitId)?.name || '-' }))
  );
  const currentGifts = computed(() => config.gifts.filter((item) => item.planId === selectedPlanId.value));
  const actionColumn = { title: '操作', key: 'action', width: 150 };
  const productColumns = [
    { title: '周期', dataIndex: 'cycleType' },
    { title: '售价', dataIndex: 'price' },
    { title: '原价', dataIndex: 'originalPrice' },
    { title: '优惠', dataIndex: 'discountText' },
    { title: '推荐', dataIndex: 'recommend' },
    { title: '状态', dataIndex: 'status' },
    actionColumn,
  ];
  const relationColumns = [
    { title: '权益', dataIndex: 'benefitName' },
    { title: '权益值', dataIndex: 'value' },
    { title: '单位', dataIndex: 'unit' },
    { title: '限制类型', dataIndex: 'limitType' },
    actionColumn,
  ];
  const giftColumns = [
    { title: '赠礼名称', dataIndex: 'name' },
    { title: '说明', dataIndex: 'description' },
    { title: '排序', dataIndex: 'sort' },
    actionColumn,
  ];
  const benefitColumns = [
    { title: '权益名称', dataIndex: 'name' },
    { title: '编码', dataIndex: 'code' },
    { title: '分类', dataIndex: 'category' },
    { title: '说明', dataIndex: 'description' },
    { title: '排序', dataIndex: 'sort' },
    actionColumn,
  ];

  const TableCell = defineComponent({
    props: ['column', 'record', 'type'],
    setup(props) {
      return () => {
        if (props.column.key === 'action')
          return (
            <a-space>
              <a onClick={() => openEditor(props.type, props.record)}>编辑</a>
              <a-popconfirm title="确认删除？" onConfirm={() => removeItem(props.type, props.record.id)}>
                <a class="danger-link">删除</a>
              </a-popconfirm>
            </a-space>
          );
        if (props.column.dataIndex === 'status')
          return <a-tag color={props.record.status === 1 ? 'success' : 'default'}>{props.record.status === 1 ? '启用' : '停用'}</a-tag>;
        if (props.column.dataIndex === 'recommend') return props.record.recommend === 1 ? '是' : '否';
        return props.record[props.column.dataIndex] ?? '-';
      };
    },
  });

  async function loadConfig() {
    loading.value = true;
    try {
      Object.assign(config, await getMemberConfig());
      if (!config.plans.some((item) => item.id === selectedPlanId.value)) selectedPlanId.value = config.plans[0]?.id;
    } finally {
      loading.value = false;
    }
  }
  function openEditor(resourceType, record?) {
    openModal(true, { resourceType, record, planId: selectedPlanId.value, benefits: config.benefits });
  }
  async function removeItem(resourceType, id) {
    await deleteMemberConfig({ resourceType, id });
    createMessage.success('删除成功');
    await loadConfig();
  }
  onMounted(loadConfig);
</script>

<style lang="less" scoped>
  .member-config-page {
    padding: 16px;
    background: #fff;
    min-height: calc(100vh - 112px);
  }
  .config-layout {
    display: grid;
    grid-template-columns: 260px minmax(0, 1fr);
    border: 1px solid #e5e7eb;
    min-height: 620px;
  }
  .plan-panel {
    border-right: 1px solid #e5e7eb;
    padding: 16px;
  }
  .panel-title,
  .detail-header,
  .table-toolbar,
  .library-toolbar {
    display: flex;
    align-items: center;
    justify-content: space-between;
  }
  .panel-title {
    margin-bottom: 12px;
    font-weight: 600;
  }
  .plan-item {
    width: 100%;
    display: flex;
    align-items: center;
    gap: 10px;
    padding: 12px;
    border: 1px solid transparent;
    background: transparent;
    cursor: pointer;
    text-align: left;
  }
  .plan-item.active {
    background: #f0f6ff;
    border-color: #91caff;
  }
  .plan-dot {
    width: 10px;
    height: 10px;
    border-radius: 50%;
    flex: none;
  }
  .plan-copy {
    min-width: 0;
    flex: 1;
    display: flex;
    flex-direction: column;
  }
  .plan-copy small {
    color: #8c8c8c;
  }
  .detail-panel {
    padding: 20px 24px;
    min-width: 0;
  }
  .detail-header {
    margin-bottom: 8px;
  }
  .detail-header h2 {
    margin: 0 0 4px;
    font-size: 20px;
  }
  .detail-header p {
    margin: 0;
    color: #8c8c8c;
  }
  .table-toolbar,
  .library-toolbar {
    justify-content: flex-end;
    margin-bottom: 12px;
  }
  .empty-panel {
    align-self: center;
  }
  .danger-link {
    color: #ff4d4f;
  }
</style>
