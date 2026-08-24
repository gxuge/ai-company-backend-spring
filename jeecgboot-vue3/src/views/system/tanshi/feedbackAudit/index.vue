<template>
  <div class="feedback-audit-page">
    <BasicTable @register="registerTable">
      <template #targetType="{ text, record }">
        <span>{{ getOptionLabel(targetTypeOptions, text) }}</span>
        <a-tag v-if="record.official" color="blue" class="official-tag">官方</a-tag>
      </template>
      <template #user="{ record }">
        <div>{{ record.userName || '-' }}</div>
        <div class="secondary-text">{{ record.userId || '-' }}</div>
      </template>
      <template #content="{ record }">
        <div v-if="record.title" class="content-title">{{ record.title }}</div>
        <a-typography-paragraph :ellipsis="{ rows: 2, expandable: false }" class="content-summary">
          {{ record.content || '-' }}
        </a-typography-paragraph>
      </template>
      <template #auditStatus="{ text }">
        <a-tag :color="auditStatusColorMap[text] || 'default'">{{ getOptionLabel(auditStatusOptions, text) }}</a-tag>
      </template>
      <template #action="{ record }">
        <TableAction :actions="getActions(record)" :dropDownActions="getDropDownActions(record)" />
      </template>
    </BasicTable>

    <FeedbackAuditModal @register="registerAuditModal" @success="handleSuccess" />
    <FeedbackAuditDetailDrawer @register="registerDetailDrawer" @success="handleSuccess" />
  </div>
</template>

<script lang="ts" setup>
  import { BasicTable, TableAction } from '/@/components/Table';
  import { useListPage } from '/@/hooks/system/useListPage';
  import { useModal } from '/@/components/Modal';
  import { useDrawer } from '/@/components/Drawer';
  import { usePermission } from '/@/hooks/web/usePermission';
  import { pageFeedbackAudits, type FeedbackAuditItem } from './feedbackAudit.api';
  import { auditColumns, auditSearchFormSchema, auditStatusOptions, getOptionLabel, targetTypeOptions } from './feedbackAudit.data';
  import FeedbackAuditModal from './components/FeedbackAuditModal.vue';
  import FeedbackAuditDetailDrawer from './components/FeedbackAuditDetailDrawer.vue';

  defineOptions({ name: 'SystemTanshiFeedbackAudit' });

  const { hasPermission } = usePermission();
  const auditStatusColorMap: Record<string, string> = {
    pending: 'processing',
    approved: 'success',
    rejected: 'error',
  };

  const [registerAuditModal, { openModal: openAuditModal }] = useModal();
  const [registerDetailDrawer, { openDrawer: openDetailDrawer }] = useDrawer();
  const { tableContext } = useListPage({
    designScope: 'tanshi-feedback-audit',
    tableProps: {
      title: '反馈与评论审核',
      api: pageFeedbackAudits,
      columns: auditColumns,
      formConfig: {
        labelWidth: 90,
        schemas: auditSearchFormSchema,
      },
      actionColumn: {
        width: 180,
        title: '操作',
        dataIndex: 'action',
        slots: { customRender: 'action' },
      },
      showIndexColumn: true,
      beforeFetch: stripTableSort,
    },
  });
  const [registerTable, { reload }] = tableContext;

  function stripTableSort(params: Recordable) {
    delete params.column;
    delete params.order;
    return params;
  }

  function openDetail(record: FeedbackAuditItem) {
    openDetailDrawer(true, {
      record,
      canUpdateStatus: hasPermission('feedback:admin:status'),
      canReply: hasPermission('feedback:admin:reply'),
    });
  }

  function openAudit(record: FeedbackAuditItem, auditStatus: 'approved' | 'rejected') {
    openAuditModal(true, { record, auditStatus });
  }

  function getActions(record: FeedbackAuditItem) {
    const actions: Recordable[] = [{ label: '详情', onClick: () => openDetail(record) }];
    if (hasPermission('feedback:admin:audit') && record.auditStatus !== 'approved') {
      actions.push({ label: '通过', onClick: () => openAudit(record, 'approved') });
    }
    return actions;
  }

  function getDropDownActions(record: FeedbackAuditItem) {
    if (!hasPermission('feedback:admin:audit') || record.auditStatus === 'rejected') {
      return [];
    }
    return [{ label: '驳回', color: 'error', onClick: () => openAudit(record, 'rejected') }];
  }

  async function handleSuccess() {
    await reload();
  }
</script>

<style lang="less" scoped>
  .feedback-audit-page {
    min-height: calc(100vh - 112px);
    padding: 16px;
    background: @component-background;
  }

  .official-tag {
    margin-left: 6px;
  }

  .secondary-text {
    margin-top: 2px;
    overflow: hidden;
    color: @text-color-secondary;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .content-title {
    margin-bottom: 4px;
    overflow: hidden;
    font-weight: 500;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .content-summary {
    margin-bottom: 0;
    color: @text-color-secondary;
    white-space: pre-wrap;
    word-break: break-word;
  }
</style>
