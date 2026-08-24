<template>
  <div class="ad-center-page">
    <a-tabs v-model:activeKey="activeTab" destroyInactiveTabPane @change="handleTabChange">
      <a-tab-pane key="slots" tab="广告位">
        <BasicTable @register="registerSlotTable">
          <template #tableTitle>
            <a-button type="primary" @click="handleAddSlot">
              <Icon icon="ant-design:plus-outlined" />
              新增广告位
            </a-button>
          </template>

          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'slotType'">
              {{ getOptionLabel(slotTypeOptions, record.slotType) }}
            </template>
            <template v-else-if="column.key === 'dimensions'"> {{ record.width }} × {{ record.height }} </template>
            <template v-else-if="column.key === 'status'">
              <a-tag :color="record.status === 'ENABLED' ? 'success' : 'default'">
                {{ getOptionLabel(slotStatusOptions, record.status) }}
              </a-tag>
            </template>
            <template v-else-if="column.key === 'action'">
              <TableAction
                :actions="[
                  {
                    label: '编辑',
                    onClick: handleEditSlot.bind(null, record),
                  },
                  {
                    label: record.status === 'ENABLED' ? '停用' : '启用',
                    popConfirm: {
                      title: `确认${record.status === 'ENABLED' ? '停用' : '启用'}该广告位？`,
                      confirm: handleToggleSlot.bind(null, record),
                    },
                  },
                  {
                    label: '删除',
                    color: 'error',
                    popConfirm: {
                      title: '确认删除该广告位？',
                      confirm: handleDeleteSlot.bind(null, record),
                    },
                  },
                ]"
              />
            </template>
          </template>
        </BasicTable>
      </a-tab-pane>

      <a-tab-pane key="contents" tab="广告内容">
        <BasicTable @register="registerContentTable">
          <template #tableTitle>
            <a-button type="primary" @click="handleAddContent">
              <Icon icon="ant-design:plus-outlined" />
              新增广告内容
            </a-button>
          </template>

          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'mediaUrl'">
              <a-image
                v-if="(record.mediaType || 'IMAGE') === 'IMAGE' && (record.mediaUrl || record.imageUrl)"
                class="ad-image"
                :src="resolveMediaUrl(record.mediaUrl || record.imageUrl, record.sourceType)"
                :width="72"
                :height="48"
                :preview="{ src: resolveMediaUrl(record.mediaUrl || record.imageUrl, record.sourceType) }"
              />
              <video
                v-else-if="record.mediaType === 'VIDEO' && (record.mediaUrl || record.imageUrl)"
                class="ad-video"
                controls
                muted
                preload="metadata"
                :src="resolveMediaUrl(record.mediaUrl || record.imageUrl, record.sourceType)"
                :poster="resolveMediaUrl(record.posterUrl, record.sourceType) || undefined"
              />
              <a-tag v-else-if="record.mediaType === 'CARD'" color="blue">卡片</a-tag>
              <span v-else>-</span>
            </template>
            <template v-else-if="column.key === 'sourceType'">
              {{ getOptionLabel(sourceTypeOptions, record.sourceType || 'SELF') }}
            </template>
            <template v-else-if="column.key === 'mediaType'">
              {{ getOptionLabel(mediaTypeOptions, record.mediaType || 'IMAGE') }}
            </template>
            <template v-else-if="column.key === 'actionType'">
              {{ getOptionLabel(linkTypeOptions, record.actionType || record.linkType || 'NONE') }}
            </template>
            <template v-else-if="column.key === 'status'">
              <a-tag :color="contentStatusColors[record.status] || 'default'">
                {{ getOptionLabel(contentStatusOptions, record.status) }}
              </a-tag>
            </template>
            <template v-else-if="column.key === 'deliveryTime'">
              <div>{{ record.startTime || '立即开始' }}</div>
              <div class="secondary-text">{{ record.endTime || '长期有效' }}</div>
            </template>
            <template v-else-if="column.key === 'action'">
              <TableAction :actions="getContentActions(record)" :drop-down-actions="getContentMoreActions(record)" />
            </template>
          </template>
        </BasicTable>
      </a-tab-pane>

      <a-tab-pane key="stats" tab="投放数据">
        <div class="stats-panel">
          <div class="stats-toolbar">
            <a-select
              v-model:value="statsSlotCode"
              allowClear
              showSearch
              optionFilterProp="label"
              placeholder="全部广告位"
              :options="slotCodeOptions"
              class="stats-control"
            />
            <a-select
              v-model:value="statsContentId"
              allowClear
              showSearch
              optionFilterProp="label"
              placeholder="全部广告内容"
              :options="contentOptions"
              class="stats-control"
            />
            <a-range-picker v-model:value="statsRange" show-time value-format="YYYY-MM-DD HH:mm:ss" class="stats-range" />
            <a-button type="primary" @click="loadStats">查询</a-button>
            <a-button @click="resetStats">重置</a-button>
          </div>

          <a-spin :spinning="statsLoading">
            <a-descriptions bordered :column="{ xs: 1, sm: 3 }">
              <a-descriptions-item label="曝光次数">{{ stats.impressions }}</a-descriptions-item>
              <a-descriptions-item label="点击次数">{{ stats.clicks }}</a-descriptions-item>
              <a-descriptions-item label="点击率">{{ formatCtr(stats.clickThroughRate) }}</a-descriptions-item>
            </a-descriptions>
          </a-spin>
        </div>
      </a-tab-pane>
    </a-tabs>

    <AdSlotModal @register="registerSlotModal" @success="handleSlotSuccess" />
    <AdContentModal @register="registerContentModal" @success="handleContentSuccess" />
    <AdContentPreviewModal @register="registerPreviewModal" />
    <DeliveryRuleDrawer @register="registerRuleDrawer" @success="handleRuleSuccess" />
  </div>
</template>

<script lang="ts" setup>
  import { computed, nextTick, onMounted, ref } from 'vue';
  import { BasicTable, TableAction, type ActionItem } from '/@/components/Table';
  import { Icon } from '/@/components/Icon';
  import { useModal } from '/@/components/Modal';
  import { useDrawer } from '/@/components/Drawer';
  import { useListPage } from '/@/hooks/system/useListPage';
  import { useMessage } from '/@/hooks/web/useMessage';
  import { getFileAccessHttpUrl } from '/@/utils/common/compUtils';
  import AdSlotModal from './components/AdSlotModal.vue';
  import AdContentModal from './components/AdContentModal.vue';
  import AdContentPreviewModal from './components/AdContentPreviewModal.vue';
  import DeliveryRuleDrawer from './components/DeliveryRuleDrawer.vue';
  import {
    contentColumns,
    contentStatusOptions,
    getContentSearchFormSchema,
    getOptionLabel,
    linkTypeOptions,
    mediaTypeOptions,
    slotColumns,
    slotSearchFormSchema,
    slotStatusOptions,
    slotTypeOptions,
    sourceTypeOptions,
  } from './adCenter.data';
  import {
    deleteAdContent,
    deleteAdSlot,
    getAdStats,
    offlineAdContent,
    pageAdContents,
    pageAdSlots,
    publishAdContent,
    updateAdSlotStatus,
    type AdContent,
    type AdPage,
    type AdSlot,
    type AdStats,
  } from './adCenter.api';

  defineOptions({ name: 'SystemTanshiAdCenter' });

  const { createMessage } = useMessage();
  const activeTab = ref('slots');
  const slots = ref<AdSlot[]>([]);
  const contents = ref<AdContent[]>([]);
  const statsLoading = ref(false);
  const statsSlotCode = ref<string>();
  const statsContentId = ref<number>();
  const statsRange = ref<string[]>([]);
  const stats = ref<AdStats>({
    impressions: 0,
    clicks: 0,
    clickThroughRate: 0,
  });

  const contentStatusColors: Record<string, string> = {
    DRAFT: 'default',
    PUBLISHED: 'success',
    OFFLINE: 'warning',
  };

  const slotOptions = computed(() =>
    slots.value.map((item) => ({
      label: `${item.slotName}（${item.slotCode}）`,
      value: item.id,
    }))
  );
  const slotCodeOptions = computed(() =>
    slots.value.map((item) => ({
      label: `${item.slotName}（${item.slotCode}）`,
      value: item.slotCode,
    }))
  );
  const contentOptions = computed(() =>
    contents.value.map((item) => ({
      label: `${item.title}（${item.contentCode}）`,
      value: item.id,
    }))
  );

  const [registerSlotModal, { openModal: openSlotModal }] = useModal();
  const [registerContentModal, { openModal: openContentModal }] = useModal();
  const [registerPreviewModal, { openModal: openPreviewModal }] = useModal();
  const [registerRuleDrawer, { openDrawer: openRuleDrawer }] = useDrawer();

  const {
    tableContext: [registerSlotTable, { reload: reloadSlots }],
  } = useListPage({
    tableProps: {
      title: '广告位列表',
      api: pageAdSlots,
      columns: slotColumns,
      formConfig: {
        labelWidth: 80,
        schemas: slotSearchFormSchema,
        autoSubmitOnEnter: true,
      },
      useSearchForm: true,
      showTableSetting: true,
      bordered: true,
      actionColumn: {
        width: 190,
        title: '操作',
        dataIndex: 'action',
        fixed: 'right',
      },
    },
  });

  const {
    tableContext: [registerContentTable, { reload: reloadContents, getForm: getContentSearchForm }],
  } = useListPage({
    tableProps: {
      title: '广告内容列表',
      api: pageAdContents,
      columns: contentColumns,
      formConfig: {
        labelWidth: 80,
        schemas: getContentSearchFormSchema([]),
        autoSubmitOnEnter: true,
      },
      useSearchForm: true,
      showTableSetting: true,
      bordered: true,
      actionColumn: {
        width: 260,
        title: '操作',
        dataIndex: 'action',
        fixed: 'right',
      },
    },
  });

  function unwrapPage<T>(value: AdPage<T> | { result?: AdPage<T> } | undefined): AdPage<T> {
    if (value && 'records' in value) {
      return value;
    }
    return value?.result || { records: [], total: 0 };
  }

  async function loadOptions() {
    const [slotResult, contentResult] = await Promise.all([pageAdSlots({ pageNo: 1, pageSize: 100 }), pageAdContents({ pageNo: 1, pageSize: 100 })]);
    slots.value = unwrapPage(slotResult).records;
    contents.value = unwrapPage(contentResult).records;
    if (activeTab.value !== 'contents') {
      return;
    }
    await nextTick();
    await getContentSearchForm()?.updateSchema({
      field: 'slotId',
      componentProps: {
        options: slotOptions.value,
        showSearch: true,
        optionFilterProp: 'label',
      },
    });
  }

  function handleAddSlot() {
    openSlotModal(true, { isUpdate: false });
  }

  function handleEditSlot(record: AdSlot) {
    openSlotModal(true, { isUpdate: true, record });
  }

  async function handleToggleSlot(record: AdSlot) {
    await updateAdSlotStatus({
      id: record.id!,
      status: record.status === 'ENABLED' ? 'DISABLED' : 'ENABLED',
    });
    createMessage.success('广告位状态已更新');
    await Promise.all([reloadSlots(), loadOptions()]);
  }

  async function handleDeleteSlot(record: AdSlot) {
    await deleteAdSlot({ id: record.id! });
    createMessage.success('广告位已删除');
    await Promise.all([reloadSlots(), loadOptions()]);
  }

  async function handleSlotSuccess() {
    await Promise.all([reloadSlots(), loadOptions()]);
  }

  function handleAddContent() {
    openContentModal(true, {
      isUpdate: false,
      slotOptions: slotOptions.value,
    });
  }

  function handleEditContent(record: AdContent) {
    openContentModal(true, {
      isUpdate: true,
      record,
      slotOptions: slotOptions.value,
    });
  }

  function handleRule(record: AdContent) {
    openRuleDrawer(true, {
      record,
    });
  }

  function handlePreview(record: AdContent) {
    openPreviewModal(true, { record });
  }

  async function handlePublish(record: AdContent) {
    await publishAdContent({ id: record.id! });
    createMessage.success('广告内容已发布');
    await handleContentSuccess();
  }

  async function handleOffline(record: AdContent) {
    await offlineAdContent({ id: record.id! });
    createMessage.success('广告内容已下线');
    await handleContentSuccess();
  }

  async function handleDeleteContent(record: AdContent) {
    await deleteAdContent({ id: record.id! });
    createMessage.success('广告内容已删除');
    await handleContentSuccess();
  }

  function getContentActions(record: AdContent): ActionItem[] {
    return [
      {
        label: '预览',
        onClick: handlePreview.bind(null, record),
      },
      {
        label: '编辑',
        onClick: handleEditContent.bind(null, record),
      },
      {
        label: '投放规则',
        onClick: handleRule.bind(null, record),
      },
    ];
  }

  function getContentMoreActions(record: AdContent): ActionItem[] {
    const actions: ActionItem[] = [];
    if (record.status !== 'PUBLISHED') {
      actions.push({
        label: '发布',
        popConfirm: {
          title: '确认发布该广告内容？',
          confirm: handlePublish.bind(null, record),
        },
      });
    }
    if (record.status === 'PUBLISHED') {
      actions.push({
        label: '下线',
        popConfirm: {
          title: '确认下线该广告内容？',
          confirm: handleOffline.bind(null, record),
        },
      });
    }
    actions.push({
      label: '删除',
      color: 'error',
      popConfirm: {
        title: '确认删除该广告内容？',
        confirm: handleDeleteContent.bind(null, record),
      },
    });
    return actions;
  }

  async function handleContentSuccess() {
    await Promise.all([reloadContents(), loadOptions()]);
  }

  async function handleRuleSuccess() {
    await reloadContents();
  }

  function resolveMediaUrl(value?: string, sourceType?: string) {
    const url = value?.split(',')[0]?.trim();
    if (!url) {
      return '';
    }
    return sourceType === 'EXTERNAL' || /^https?:\/\//i.test(url) ? url : getFileAccessHttpUrl(url);
  }

  async function loadStats() {
    statsLoading.value = true;
    try {
      stats.value = await getAdStats({
        slotCode: statsSlotCode.value,
        contentId: statsContentId.value,
        startTime: statsRange.value?.[0],
        endTime: statsRange.value?.[1],
      });
    } finally {
      statsLoading.value = false;
    }
  }

  async function resetStats() {
    statsSlotCode.value = undefined;
    statsContentId.value = undefined;
    statsRange.value = [];
    await loadStats();
  }

  function formatCtr(value?: number) {
    return `${((value || 0) * 100).toFixed(2)}%`;
  }

  async function handleTabChange(key: string) {
    if (key === 'contents') {
      await nextTick();
      await loadOptions();
      await reloadContents();
    }
    if (key === 'stats') {
      await loadOptions();
      await loadStats();
    }
  }

  onMounted(loadOptions);
</script>

<style lang="less" scoped>
  .ad-center-page {
    min-height: calc(100vh - 120px);
    padding: 16px;
    background: @component-background;
  }

  .ad-image {
    display: block;

    :deep(.ant-image-img) {
      object-fit: cover;
      background: @background-color-light;
      border: 1px solid @border-color-base;
      border-radius: 4px;
    }
  }

  .ad-video {
    display: block;
    width: 72px;
    height: 48px;
    object-fit: cover;
    background: #000;
    border: 1px solid @border-color-base;
    border-radius: 4px;
  }

  .secondary-text {
    margin-top: 4px;
    color: @text-color-secondary;
  }

  .stats-panel {
    min-height: 320px;
  }

  .stats-toolbar {
    display: flex;
    flex-wrap: wrap;
    gap: 12px;
    align-items: center;
    margin-bottom: 16px;
    padding-bottom: 16px;
    border-bottom: 1px solid @border-color-base;
  }

  .stats-control {
    width: 240px;
  }

  .stats-range {
    width: 360px;
  }

  @media (max-width: 768px) {
    .ad-center-page {
      padding: 12px;
    }

    .stats-control,
    .stats-range {
      width: 100%;
    }
  }
</style>
