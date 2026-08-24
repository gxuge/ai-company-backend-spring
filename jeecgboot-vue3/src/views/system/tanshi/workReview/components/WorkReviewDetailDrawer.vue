<template>
  <BasicDrawer v-bind="$attrs" @register="registerDrawer" :title="drawerTitle" width="920" destroyOnClose showFooter>
    <a-spin :spinning="loading">
      <template v-if="detail">
        <a-alert class="review-alert" type="info" show-icon message="审核结论作用于当前完整作品版本，包含该版本的文本与图片。" />

        <a-descriptions bordered :column="2" size="small">
          <a-descriptions-item label="作品名称">{{ detail.workTitle || '-' }}</a-descriptions-item>
          <a-descriptions-item label="作品ID">{{ detail.workId }}</a-descriptions-item>
          <a-descriptions-item label="审核单号">{{ detail.reviewNo }}</a-descriptions-item>
          <a-descriptions-item label="内容版本">V{{ detail.workVersion }}</a-descriptions-item>
          <a-descriptions-item label="所属用户ID">{{ detail.ownerUserId }}</a-descriptions-item>
          <a-descriptions-item label="申请公开">{{ detail.requestedPublic === 1 ? '是' : '否' }}</a-descriptions-item>
          <a-descriptions-item label="审核状态">
            <a-tag :color="reviewStatusColorMap[detail.status] || 'default'">
              {{ getOptionLabel(reviewStatusOptions, detail.status) }}
            </a-tag>
          </a-descriptions-item>
          <a-descriptions-item label="提交时间">{{ detail.submittedAt || '-' }}</a-descriptions-item>
        </a-descriptions>

        <a-tabs class="detail-tabs">
          <a-tab-pane key="material" :tab="materialTabTitle">
            <a-empty v-if="!filteredItems.length" description="该版本暂无此类审核材料" />

            <div v-else-if="itemType === 'TEXT'" class="text-material-list">
              <section v-for="item in filteredItems" :key="item.id" class="text-material">
                <div class="material-title">{{ getFieldLabel(item.fieldCode) }}</div>
                <pre>{{ item.contentText || '-' }}</pre>
              </section>
            </div>

            <div v-else class="image-grid">
              <div v-for="item in filteredItems" :key="item.id" class="image-item">
                <div class="material-title">{{ getFieldLabel(item.fieldCode) }}</div>
                <a-image :src="getImageUrl(item.assetUrl)" :alt="getFieldLabel(item.fieldCode)" />
              </div>
            </div>
          </a-tab-pane>

          <a-tab-pane key="ai" tab="AI 初审">
            <a-descriptions bordered :column="2" size="small">
              <a-descriptions-item label="AI 结论">
                <a-tag v-if="detail.aiDecision" :color="aiDecisionColorMap[detail.aiDecision] || 'default'">
                  {{ getOptionLabel(aiDecisionOptions, detail.aiDecision) }}
                </a-tag>
                <span v-else>-</span>
              </a-descriptions-item>
              <a-descriptions-item label="风险等级">
                <a-tag v-if="detail.aiRiskLevel" :color="aiRiskColorMap[detail.aiRiskLevel] || 'default'">
                  {{ getOptionLabel(aiRiskOptions, detail.aiRiskLevel) }}
                </a-tag>
                <span v-else>-</span>
              </a-descriptions-item>
              <a-descriptions-item label="审核时间">{{ detail.aiReviewedAt || '-' }}</a-descriptions-item>
              <a-descriptions-item label="AI 原因">{{ detail.aiReason || '-' }}</a-descriptions-item>
            </a-descriptions>
          </a-tab-pane>

          <a-tab-pane key="admin" tab="管理员终审">
            <a-descriptions bordered :column="2" size="small">
              <a-descriptions-item label="审核人ID">{{ detail.adminReviewerId || '-' }}</a-descriptions-item>
              <a-descriptions-item label="审核时间">{{ detail.adminReviewedAt || '-' }}</a-descriptions-item>
              <a-descriptions-item label="审核意见" :span="2">{{ detail.adminReason || '-' }}</a-descriptions-item>
            </a-descriptions>
          </a-tab-pane>

          <a-tab-pane key="logs" tab="审核日志">
            <a-table :columns="logColumns" :data-source="detail.logs || []" row-key="id" size="small" :pagination="false">
              <template #bodyCell="{ column, record }">
                <template v-if="column.dataIndex === 'actionType'">
                  {{ reviewActionLabelMap[record.actionType] || record.actionType || '-' }}
                </template>
                <template v-else-if="column.dataIndex === 'statusChange'">
                  {{ formatStatusChange(record.beforeStatus, record.afterStatus) }}
                </template>
                <template v-else-if="column.dataIndex === 'operator'">
                  {{ record.operatorType || '-' }}<span v-if="record.operatorId"> / {{ record.operatorId }}</span>
                </template>
              </template>
            </a-table>
          </a-tab-pane>
        </a-tabs>
      </template>
      <a-empty v-else-if="!loading" description="暂无审核详情" />
    </a-spin>

    <template #footer>
      <a-button @click="closeDrawer">关闭</a-button>
      <a-button v-if="detail?.status === 'PENDING_AI'" :loading="submitting" @click="handleRetryAi">重试 AI</a-button>
      <a-button v-if="showReviewActions" danger :loading="submitting" @click="openActionModal('reject')">驳回</a-button>
      <a-button v-if="showReviewActions" type="primary" :loading="submitting" @click="openActionModal('approve')">通过</a-button>
    </template>

    <a-modal
      v-model:open="actionVisible"
      :title="actionMode === 'reject' ? '驳回作品审核' : '通过作品审核'"
      :confirm-loading="submitting"
      @ok="submitReviewAction"
    >
      <a-form layout="vertical">
        <a-form-item :label="actionMode === 'reject' ? '驳回原因' : '审核意见'" :required="actionMode === 'reject'">
          <a-textarea
            v-model:value="actionReason"
            :rows="4"
            :maxlength="1000"
            show-count
            :placeholder="actionMode === 'reject' ? '请输入驳回原因' : '可填写审核意见'"
          />
        </a-form-item>
      </a-form>
    </a-modal>
  </BasicDrawer>
</template>

<script lang="ts" setup>
  import { computed, ref } from 'vue';
  import { BasicDrawer, useDrawerInner } from '/@/components/Drawer';
  import { useMessage } from '/@/hooks/web/useMessage';
  import { getFileAccessHttpUrl } from '/@/utils/common/compUtils';
  import {
    approveWorkReview,
    getWorkReviewDetail,
    rejectWorkReview,
    retryWorkReviewAi,
    type ReviewItemType,
    type WorkReview,
  } from '../workReview.api';
  import {
    aiDecisionColorMap,
    aiDecisionOptions,
    aiRiskColorMap,
    aiRiskOptions,
    fieldLabelMap,
    getOptionLabel,
    reviewActionLabelMap,
    reviewStatusColorMap,
    reviewStatusOptions,
  } from '../workReview.data';

  const emit = defineEmits(['register', 'success']);
  const { createMessage } = useMessage();
  const loading = ref(false);
  const submitting = ref(false);
  const detail = ref<WorkReview>();
  const itemType = ref<ReviewItemType>('TEXT');
  const title = ref('作品内容审核');
  const actionVisible = ref(false);
  const actionMode = ref<'approve' | 'reject'>('approve');
  const actionReason = ref('');

  const filteredItems = computed(() => (detail.value?.items || []).filter((item) => item.itemType === itemType.value));
  const showReviewActions = computed(() => detail.value?.status === 'PENDING_ADMIN');
  const drawerTitle = computed(() => `${title.value}详情`);
  const materialTabTitle = computed(() => (itemType.value === 'TEXT' ? '内容材料' : '图片材料'));

  const logColumns = [
    { title: '动作', dataIndex: 'actionType', width: 130 },
    { title: '状态变化', dataIndex: 'statusChange', width: 210 },
    { title: '操作方', dataIndex: 'operator', width: 220 },
    { title: '原因', dataIndex: 'reason', ellipsis: true },
    { title: '时间', dataIndex: 'createdAt', width: 180 },
  ];

  const [registerDrawer, { setDrawerProps, closeDrawer }] = useDrawerInner(async (data) => {
    detail.value = undefined;
    itemType.value = data?.itemType || 'TEXT';
    title.value = data?.title || '作品内容审核';
    actionVisible.value = false;
    actionReason.value = '';
    if (!data?.id) {
      return;
    }
    loading.value = true;
    setDrawerProps({ loading: true });
    try {
      detail.value = await getWorkReviewDetail({ id: data.id });
    } finally {
      loading.value = false;
      setDrawerProps({ loading: false });
    }
  });

  function getFieldLabel(fieldCode?: string) {
    return (fieldCode && fieldLabelMap[fieldCode]) || fieldCode || '审核材料';
  }

  function getImageUrl(url?: string) {
    return url ? getFileAccessHttpUrl(url) : '';
  }

  function formatStatusChange(beforeStatus?: string, afterStatus?: string) {
    const before = beforeStatus ? getOptionLabel(reviewStatusOptions, beforeStatus) : '无';
    const after = afterStatus ? getOptionLabel(reviewStatusOptions, afterStatus) : '无';
    return `${before} -> ${after}`;
  }

  function openActionModal(mode: 'approve' | 'reject') {
    actionMode.value = mode;
    actionReason.value = '';
    actionVisible.value = true;
  }

  async function submitReviewAction() {
    if (!detail.value) {
      return;
    }
    const reason = actionReason.value.trim();
    if (actionMode.value === 'reject' && !reason) {
      createMessage.warning('请输入驳回原因');
      return;
    }
    submitting.value = true;
    try {
      if (actionMode.value === 'reject') {
        await rejectWorkReview({ id: detail.value.id, reason });
      } else {
        await approveWorkReview({ id: detail.value.id, reason: reason || undefined });
      }
      actionVisible.value = false;
      closeDrawer();
      emit('success');
    } finally {
      submitting.value = false;
    }
  }

  async function handleRetryAi() {
    if (!detail.value) {
      return;
    }
    submitting.value = true;
    try {
      await retryWorkReviewAi({ id: detail.value.id });
      closeDrawer();
      emit('success');
    } finally {
      submitting.value = false;
    }
  }
</script>

<style lang="less" scoped>
  .review-alert {
    margin-bottom: 16px;
  }

  .detail-tabs {
    margin-top: 16px;
  }

  .text-material-list {
    display: grid;
    gap: 12px;
  }

  .text-material,
  .image-item {
    border: 1px solid #f0f0f0;
    border-radius: 4px;
    background: @component-background;
  }

  .material-title {
    padding: 10px 12px;
    border-bottom: 1px solid #f0f0f0;
    font-weight: 600;
  }

  .text-material pre {
    margin: 0;
    padding: 12px;
    overflow-wrap: anywhere;
    white-space: pre-wrap;
    font-family: inherit;
    line-height: 1.7;
  }

  .image-grid {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
    gap: 16px;
  }

  .image-item :deep(.ant-image) {
    display: block;
    width: 100%;
    padding: 12px;
  }

  .image-item :deep(.ant-image-img) {
    width: 100%;
    height: 260px;
    object-fit: contain;
    background: @background-color-light;
  }
</style>
