<template>
  <BasicTable @register="registerTable">
    <template #status="{ text }">
      <a-tag :color="reviewStatusColorMap[text] || 'default'">{{ getOptionLabel(reviewStatusOptions, text) }}</a-tag>
    </template>
    <template #aiDecision="{ text }">
      <a-tag v-if="text" :color="aiDecisionColorMap[text] || 'default'">{{ getOptionLabel(aiDecisionOptions, text) }}</a-tag>
      <span v-else>-</span>
    </template>
    <template #aiRiskLevel="{ text }">
      <a-tag v-if="text" :color="aiRiskColorMap[text] || 'default'">{{ getOptionLabel(aiRiskOptions, text) }}</a-tag>
      <span v-else>-</span>
    </template>
    <template #requestedPublic="{ text }">{{ Number(text) === 1 ? '是' : '否' }}</template>
    <template #action="{ record }">
      <TableAction :actions="getActions(record)" />
    </template>
  </BasicTable>
  <WorkReviewDetailDrawer @register="registerDrawer" @success="reload" />
</template>

<script lang="ts" setup>
  import { BasicTable, TableAction } from '/@/components/Table';
  import { useDrawer } from '/@/components/Drawer';
  import { useListPage } from '/@/hooks/system/useListPage';
  import { retryWorkReviewAi, getWorkReviewList, type ReviewItemType, type WorkReview, type WorkType } from '../workReview.api';
  import {
    aiDecisionColorMap,
    aiDecisionOptions,
    aiRiskColorMap,
    aiRiskOptions,
    columns,
    getOptionLabel,
    reviewStatusColorMap,
    reviewStatusOptions,
    searchFormSchema,
  } from '../workReview.data';
  import WorkReviewDetailDrawer from './WorkReviewDetailDrawer.vue';

  const props = defineProps<{
    workType: WorkType;
    itemType: ReviewItemType;
    title: string;
  }>();

  const [registerDrawer, { openDrawer }] = useDrawer();

  const { tableContext } = useListPage({
    designScope: `tanshi-work-review-${props.workType.toLowerCase()}-${props.itemType.toLowerCase()}`,
    tableProps: {
      title: props.title,
      api: getWorkReviewList,
      columns,
      formConfig: {
        labelWidth: 100,
        schemas: searchFormSchema,
      },
      actionColumn: {
        width: 150,
        title: '操作',
        dataIndex: 'action',
        slots: { customRender: 'action' },
      },
      beforeFetch: (params) => {
        delete params.column;
        delete params.order;
        return {
          ...params,
          workType: props.workType,
        };
      },
    },
  });

  const [registerTable, { reload }] = tableContext;

  function openDetail(record: WorkReview) {
    openDrawer(true, {
      id: record.id,
      itemType: props.itemType,
      title: props.title,
    });
  }

  function getActions(record: WorkReview) {
    const actions: Recordable[] = [
      {
        label: '审核详情',
        onClick: () => openDetail(record),
      },
    ];
    if (record.status === 'PENDING_AI') {
      actions.push({
        label: '重试 AI',
        popConfirm: {
          title: '确认重新提交 AI 初审？',
          confirm: async () => {
            await retryWorkReviewAi({ id: record.id });
            reload();
          },
        },
      });
    }
    return actions;
  }
</script>
