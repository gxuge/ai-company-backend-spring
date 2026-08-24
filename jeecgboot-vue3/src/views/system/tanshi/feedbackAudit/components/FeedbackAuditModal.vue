<template>
  <BasicModal v-bind="$attrs" @register="registerModal" :title="title" :width="560" @ok="handleSubmit" destroyOnClose>
    <a-form layout="vertical">
      <a-form-item label="待审核内容">
        <div class="content-preview">{{ record?.content || '-' }}</div>
      </a-form-item>
      <a-form-item :label="isReject ? '驳回原因' : '审核备注'" :required="isReject">
        <a-textarea v-model:value="auditReason" :maxlength="500" :rows="4" show-count :placeholder="isReject ? '请输入驳回原因' : '可填写审核备注'" />
      </a-form-item>
    </a-form>
  </BasicModal>
</template>

<script lang="ts" setup>
  import { computed, ref } from 'vue';
  import { BasicModal, useModalInner } from '/@/components/Modal';
  import { useMessage } from '/@/hooks/web/useMessage';
  import { updateFeedbackAudit, type FeedbackAuditItem, type FeedbackAuditStatus } from '../feedbackAudit.api';

  const emit = defineEmits(['register', 'success']);
  const { createMessage } = useMessage();
  const record = ref<FeedbackAuditItem>();
  const auditStatus = ref<Exclude<FeedbackAuditStatus, 'pending'>>('approved');
  const auditReason = ref('');

  const isReject = computed(() => auditStatus.value === 'rejected');
  const title = computed(() => (isReject.value ? '驳回审核内容' : '审核通过'));

  const [registerModal, { setModalProps, closeModal }] = useModalInner((data) => {
    record.value = data?.record;
    auditStatus.value = data?.auditStatus === 'rejected' ? 'rejected' : 'approved';
    auditReason.value = '';
    setModalProps({ confirmLoading: false });
  });

  async function handleSubmit() {
    if (!record.value) {
      return;
    }
    const reason = auditReason.value.trim();
    if (isReject.value && !reason) {
      createMessage.warning('请输入驳回原因');
      return;
    }
    setModalProps({ confirmLoading: true });
    try {
      await updateFeedbackAudit({
        targetType: record.value.targetType,
        targetId: record.value.targetId,
        auditStatus: auditStatus.value,
        auditReason: reason || undefined,
      });
      closeModal();
      emit('success');
    } finally {
      setModalProps({ confirmLoading: false });
    }
  }
</script>

<style lang="less" scoped>
  .content-preview {
    max-height: 180px;
    padding: 10px 12px;
    overflow: auto;
    line-height: 1.7;
    white-space: pre-wrap;
    word-break: break-word;
    background: @background-color-light;
    border: 1px solid @border-color-base;
    border-radius: 4px;
  }
</style>
