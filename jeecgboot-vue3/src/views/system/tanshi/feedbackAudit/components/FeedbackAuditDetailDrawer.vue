<template>
  <BasicDrawer v-bind="$attrs" @register="registerDrawer" title="审核内容详情" :width="720">
    <a-descriptions v-if="record" :column="2" bordered size="small">
      <a-descriptions-item label="内容类型">{{ getOptionLabel(targetTypeOptions, record.targetType) }}</a-descriptions-item>
      <a-descriptions-item label="审核状态">
        <a-tag :color="auditStatusColorMap[record.auditStatus]">{{ getOptionLabel(auditStatusOptions, record.auditStatus) }}</a-tag>
      </a-descriptions-item>
      <a-descriptions-item label="反馈ID">{{ record.feedbackId }}</a-descriptions-item>
      <a-descriptions-item label="内容ID">{{ record.targetId }}</a-descriptions-item>
      <a-descriptions-item label="发布用户">{{ record.userName || '-' }}</a-descriptions-item>
      <a-descriptions-item label="用户ID">{{ record.userId || '-' }}</a-descriptions-item>
      <a-descriptions-item v-if="record.title" label="反馈标题" :span="2">{{ record.title }}</a-descriptions-item>
      <a-descriptions-item label="提交时间">{{ record.createdAt || '-' }}</a-descriptions-item>
      <a-descriptions-item label="审核时间">{{ record.auditedAt || '-' }}</a-descriptions-item>
      <a-descriptions-item label="审核人">{{ record.auditorName || record.auditedBy || '-' }}</a-descriptions-item>
      <a-descriptions-item label="一级评论ID">{{ record.parentId || '-' }}</a-descriptions-item>
      <a-descriptions-item label="内容" :span="2">
        <div class="detail-content">{{ record.content || '-' }}</div>
      </a-descriptions-item>
      <a-descriptions-item v-if="record.auditReason" label="审核原因" :span="2">{{ record.auditReason }}</a-descriptions-item>
    </a-descriptions>

    <template v-if="record && (canUpdateStatus || canReply)">
      <a-divider orientation="left">反馈运营处理</a-divider>
      <div v-if="canUpdateStatus" class="operation-row">
        <div class="operation-label">处理状态</div>
        <a-select v-model:value="processStatus" :options="processStatusOptions" placeholder="请选择处理状态" />
        <a-button type="primary" :loading="statusLoading" @click="handleStatusUpdate">更新状态</a-button>
      </div>
      <div v-if="canReply" class="reply-section">
        <div class="operation-label">官方回复</div>
        <a-textarea v-model:value="replyContent" :maxlength="2000" :rows="5" show-count placeholder="请输入官方回复内容" />
        <div class="reply-action">
          <a-button type="primary" :loading="replyLoading" @click="handleReply">发布回复</a-button>
        </div>
      </div>
    </template>
  </BasicDrawer>
</template>

<script lang="ts" setup>
  import { ref } from 'vue';
  import { BasicDrawer, useDrawerInner } from '/@/components/Drawer';
  import { useMessage } from '/@/hooks/web/useMessage';
  import { createOfficialReply, updateFeedbackStatus, type FeedbackAuditItem, type FeedbackProcessStatus } from '../feedbackAudit.api';
  import { auditStatusOptions, getOptionLabel, processStatusOptions, targetTypeOptions } from '../feedbackAudit.data';

  const emit = defineEmits(['register', 'success']);
  const { createMessage } = useMessage();
  const record = ref<FeedbackAuditItem>();
  const canUpdateStatus = ref(false);
  const canReply = ref(false);
  const processStatus = ref<FeedbackProcessStatus>();
  const replyContent = ref('');
  const statusLoading = ref(false);
  const replyLoading = ref(false);
  const auditStatusColorMap: Record<string, string> = {
    pending: 'processing',
    approved: 'success',
    rejected: 'error',
  };

  const [registerDrawer] = useDrawerInner((data) => {
    record.value = data?.record;
    canUpdateStatus.value = !!data?.canUpdateStatus;
    canReply.value = !!data?.canReply;
    processStatus.value = undefined;
    replyContent.value = '';
  });

  async function handleStatusUpdate() {
    if (!record.value || !processStatus.value) {
      createMessage.warning('请选择处理状态');
      return;
    }
    statusLoading.value = true;
    try {
      await updateFeedbackStatus({
        feedbackId: record.value.feedbackId,
        status: processStatus.value,
      });
      createMessage.success('反馈处理状态已更新');
      emit('success');
    } finally {
      statusLoading.value = false;
    }
  }

  async function handleReply() {
    if (!record.value) {
      return;
    }
    const content = replyContent.value.trim();
    if (!content) {
      createMessage.warning('请输入官方回复内容');
      return;
    }
    replyLoading.value = true;
    try {
      await createOfficialReply({
        feedbackId: record.value.feedbackId,
        content,
      });
      replyContent.value = '';
      createMessage.success('官方回复已发布');
      emit('success');
    } finally {
      replyLoading.value = false;
    }
  }
</script>

<style lang="less" scoped>
  .detail-content {
    line-height: 1.7;
    white-space: pre-wrap;
    word-break: break-word;
  }

  .operation-row {
    display: grid;
    grid-template-columns: 80px minmax(180px, 260px) auto;
    gap: 12px;
    align-items: center;
    margin-bottom: 20px;
  }

  .operation-label {
    font-weight: 500;
  }

  .reply-section {
    display: grid;
    grid-template-columns: 80px minmax(0, 1fr);
    gap: 12px;
    align-items: start;
  }

  .reply-action {
    grid-column: 2;
    text-align: right;
  }
</style>
