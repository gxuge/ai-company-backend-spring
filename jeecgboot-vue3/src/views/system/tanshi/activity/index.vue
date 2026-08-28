<template>
  <div class="activity-page">
    <a-tabs v-model:activeKey="activeTab" destroyInactiveTabPane @change="handleTabChange">
      <a-tab-pane key="tasks" tab="活动任务">
        <BasicTable v-if="activeTab === 'tasks'" @register="registerTaskTable">
          <template #tableTitle>
            <a-button type="primary" @click="openTaskEditor()">新增任务</a-button>
          </template>
          <template #taskType="{ text }">{{ getOptionLabel(taskTypeOptions, text) }}</template>
          <template #category="{ text }">{{ getOptionLabel(categoryOptions, text) }}</template>
          <template #conditionType="{ text }">{{ getOptionLabel(conditionTypeOptions, text) }}</template>
          <template #reward="{ record }">{{ record.rewardValue }} 星钻</template>
          <template #rewardClaimMode="{ text }">{{ getOptionLabel(rewardClaimModeOptions, text) }}</template>
          <template #status="{ text }">
            <a-tag :color="text === 'ENABLED' ? 'success' : 'default'">{{ getOptionLabel(taskStatusOptions, text) }}</a-tag>
          </template>
          <template #action="{ record }">
            <TableAction :actions="[{ label: '编辑', onClick: () => openTaskEditor(record) }]" />
          </template>
        </BasicTable>
      </a-tab-pane>

      <a-tab-pane key="userTasks" tab="用户任务进度">
        <BasicTable v-if="activeTab === 'userTasks'" @register="registerUserTaskTable">
          <template #progress="{ record }">{{ record.currentValue || 0 }} / {{ record.targetValue || 0 }}</template>
          <template #status="{ text }">
            <a-tag :color="text === 'COMPLETED' ? 'success' : 'processing'">{{ getOptionLabel(userTaskStatusOptions, text) }}</a-tag>
          </template>
          <template #rewardStatus="{ text }">
            <a-tag :color="text === 'CLAIMED' ? 'success' : text === 'GRANTING' ? 'processing' : 'default'">
              {{ getOptionLabel(rewardStatusOptions, text) }}
            </a-tag>
          </template>
        </BasicTable>
      </a-tab-pane>

      <a-tab-pane key="rewards" tab="奖励记录">
        <BasicTable v-if="activeTab === 'rewards'" @register="registerRewardTable">
          <template #sourceType="{ text }">{{ getOptionLabel(sourceTypeOptions, text) }}</template>
          <template #memberLevel="{ text }">{{ getOptionLabel(memberLevelOptions, text) }}</template>
        </BasicTable>
      </a-tab-pane>

      <a-tab-pane key="rules" tab="会员奖励加成">
        <div class="rule-toolbar">
          <a-button @click="loadRewardRules">刷新</a-button>
          <a-button type="primary" @click="openRuleEditor()">新增规则</a-button>
        </div>
        <a-spin :spinning="ruleLoading">
          <a-table :columns="ruleColumns" :data-source="ruleTableData" :pagination="false" :scroll="{ x: 900 }" row-key="id" bordered>
            <template #bodyCell="{ column, record }">
              <template v-if="column.dataIndex === 'memberLevel'">
                {{ getOptionLabel(memberLevelOptions, record.memberLevel) }}
              </template>
              <template v-else-if="column.dataIndex === 'extraRewardType'">
                {{ getOptionLabel(rewardTypeOptions, record.extraRewardType) }}
              </template>
              <template v-else-if="column.dataIndex === 'status'">
                <a-tag :color="record.status === 1 ? 'success' : 'default'">{{ record.status === 1 ? '启用' : '停用' }}</a-tag>
              </template>
              <template v-else-if="column.dataIndex === 'taskName'">
                {{ getTaskName(record.taskId) }}
              </template>
              <template v-else-if="column.dataIndex === 'action'">
                <a @click="openRuleEditor(record)">编辑</a>
              </template>
              <template v-else>{{ record[column.dataIndex] || '-' }}</template>
            </template>
          </a-table>
        </a-spin>
      </a-tab-pane>

      <a-tab-pane key="signMilestones" tab="签到里程碑">
        <div class="rule-toolbar">
          <a-button @click="loadSignMilestoneRules">刷新</a-button>
          <a-button type="primary" @click="openSignMilestoneEditor()">新增规则</a-button>
        </div>
        <a-spin :spinning="signMilestoneLoading">
          <a-table
            :columns="signMilestoneTableColumns"
            :data-source="signMilestoneTableData"
            :pagination="false"
            :scroll="{ x: 900 }"
            row-key="id"
            bordered
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.dataIndex === 'rewardType'">
                {{ getOptionLabel(rewardTypeOptions, record.rewardType) }}
              </template>
              <template v-else-if="column.dataIndex === 'status'">
                <a-tag :color="record.status === 1 ? 'success' : 'default'">{{ record.status === 1 ? '启用' : '停用' }}</a-tag>
              </template>
              <template v-else-if="column.dataIndex === 'action'">
                <a @click="openSignMilestoneEditor(record)">编辑</a>
              </template>
              <template v-else>{{ record[column.dataIndex] ?? '-' }}</template>
            </template>
          </a-table>
        </a-spin>
      </a-tab-pane>
    </a-tabs>

    <ActivityTaskModal @register="registerTaskModal" @success="handleTaskSuccess" />
    <ActivityRewardRuleModal @register="registerRuleModal" @success="loadRewardRules" />
    <ActivitySignMilestoneModal @register="registerSignMilestoneModal" @success="loadSignMilestoneRules" />
  </div>
</template>

<script lang="ts" setup>
  import { computed, onMounted, ref } from 'vue';
  import { BasicTable, TableAction } from '/@/components/Table';
  import { useListPage } from '/@/hooks/system/useListPage';
  import { useModal } from '/@/components/Modal';
  import { useMessage } from '/@/hooks/web/useMessage';
  import {
    listActivitySignMilestoneRules,
    listActivityRewardRules,
    pageActivityRewards,
    pageActivityTasks,
    pageUserTaskProgress,
    type ActivityRewardRule,
    type ActivitySignMilestoneRule,
    type ActivityTask,
  } from './activity.api';
  import {
    categoryOptions,
    conditionTypeOptions,
    getOptionLabel,
    memberLevelOptions,
    rewardClaimModeOptions,
    rewardColumns,
    rewardSearchFormSchema,
    rewardStatusOptions,
    rewardRuleColumns,
    rewardTypeOptions,
    sourceTypeOptions,
    signMilestoneColumns,
    taskColumns,
    taskSearchFormSchema,
    taskStatusOptions,
    taskTypeOptions,
    userTaskColumns,
    userTaskSearchFormSchema,
    userTaskStatusOptions,
  } from './activity.data';
  import ActivityTaskModal from './components/ActivityTaskModal.vue';
  import ActivityRewardRuleModal from './components/ActivityRewardRuleModal.vue';
  import ActivitySignMilestoneModal from './components/ActivitySignMilestoneModal.vue';

  defineOptions({ name: 'SystemTanshiActivity' });

  const { createMessage } = useMessage();
  const activeTab = ref('tasks');
  const ruleLoading = ref(false);
  const signMilestoneLoading = ref(false);
  const taskOptions = ref<{ label: string; value: number }[]>([]);
  const signTaskOptions = ref<{ label: string; value: number }[]>([]);
  const rewardRules = ref<ActivityRewardRule[]>([]);
  const signMilestoneRules = ref<ActivitySignMilestoneRule[]>([]);
  const [registerTaskModal, { openModal: openTaskModal }] = useModal();
  const [registerRuleModal, { openModal: openRuleModal }] = useModal();
  const [registerSignMilestoneModal, { openModal: openSignMilestoneModal }] = useModal();

  const { tableContext: taskTableContext } = useListPage({
    designScope: 'tanshi-activity-tasks',
    tableProps: {
      title: '活动任务',
      api: pageActivityTasks,
      columns: taskColumns,
      formConfig: { labelWidth: 90, schemas: taskSearchFormSchema },
      actionColumn: { width: 90, title: '操作', dataIndex: 'action', slots: { customRender: 'action' } },
      beforeFetch: stripTableSort,
    },
  });
  const [registerTaskTable, { reload: reloadTaskTable }] = taskTableContext;

  const { tableContext: userTaskTableContext } = useListPage({
    designScope: 'tanshi-activity-user-tasks',
    tableProps: {
      title: '用户任务进度',
      api: pageUserTaskProgress,
      columns: userTaskColumns,
      formConfig: { labelWidth: 90, schemas: userTaskSearchFormSchema },
      showActionColumn: false,
      beforeFetch: stripTableSort,
    },
  });
  const [registerUserTaskTable] = userTaskTableContext;

  const { tableContext: rewardTableContext } = useListPage({
    designScope: 'tanshi-activity-rewards',
    tableProps: {
      title: '活动奖励记录',
      api: pageActivityRewards,
      columns: rewardColumns,
      formConfig: {
        labelWidth: 90,
        schemas: rewardSearchFormSchema,
        fieldMapToTime: [['timeRange', ['startTime', 'endTime'], 'YYYY-MM-DD HH:mm:ss']],
      },
      showActionColumn: false,
      beforeFetch: stripTableSort,
    },
  });
  const [registerRewardTable] = rewardTableContext;

  const ruleColumns = computed(() => [...rewardRuleColumns, { title: '操作', dataIndex: 'action', width: 90, fixed: 'right' }]);
  const ruleTableData = computed(() =>
    rewardRules.value.map((item) => ({
      ...item,
      taskName: getTaskName(item.taskId),
    }))
  );
  const signMilestoneTableColumns = computed(() => [
    ...signMilestoneColumns,
    { title: '操作', dataIndex: 'action', width: 90, fixed: 'right' },
  ]);
  const signMilestoneTableData = computed(() =>
    signMilestoneRules.value.map((item) => ({
      ...item,
      taskName: getTaskName(item.taskId),
    }))
  );

  function stripTableSort(params: Recordable) {
    delete params.column;
    delete params.order;
    return params;
  }

  function getTaskName(taskId?: number) {
    return taskOptions.value.find((item) => item.value === taskId)?.label || `任务 ${taskId || '-'}`;
  }

  function openTaskEditor(record?: ActivityTask) {
    openTaskModal(true, { record });
  }

  function openRuleEditor(record?: ActivityRewardRule) {
    if (!taskOptions.value.length) {
      createMessage.warning('暂无可配置的活动任务');
      return;
    }
    openRuleModal(true, { record, taskOptions: taskOptions.value });
  }

  function openSignMilestoneEditor(record?: ActivitySignMilestoneRule) {
    if (!signTaskOptions.value.length) {
      createMessage.warning('暂无可配置的签到任务');
      return;
    }
    openSignMilestoneModal(true, { record, taskOptions: signTaskOptions.value });
  }

  async function loadTaskOptions() {
    const result: any = await pageActivityTasks({ pageNo: 1, pageSize: 100 });
    const page = result?.records ? result : result?.result;
    taskOptions.value = (page?.records || [])
      .filter((item) => typeof item.id === 'number')
      .map((item) => ({
        label: `${item.taskName || '-'}（ID：${item.id}）`,
        value: item.id,
      }));
    signTaskOptions.value = (page?.records || [])
      .filter((item) => typeof item.id === 'number' && item.taskType === 'SIGN')
      .map((item) => ({
        label: `${item.taskName || '-'}（ID：${item.id}）`,
        value: item.id,
      }));
  }

  async function loadRewardRules() {
    ruleLoading.value = true;
    try {
      const result: any = await listActivityRewardRules();
      rewardRules.value = Array.isArray(result) ? result : result?.result || [];
    } finally {
      ruleLoading.value = false;
    }
  }

  async function loadSignMilestoneRules() {
    signMilestoneLoading.value = true;
    try {
      const result: any = await listActivitySignMilestoneRules();
      signMilestoneRules.value = Array.isArray(result) ? result : result?.result || [];
    } finally {
      signMilestoneLoading.value = false;
    }
  }

  function handleTabChange(key: string) {
    if (key === 'rules') {
      loadRewardRules();
    } else if (key === 'signMilestones') {
      loadSignMilestoneRules();
    }
  }

  async function handleTaskSuccess() {
    await reloadTaskTable();
    await loadTaskOptions();
  }

  onMounted(loadTaskOptions);
</script>

<style lang="less" scoped>
  .activity-page {
    padding: 16px;
    background: @component-background;
    min-height: calc(100vh - 112px);
  }

  .rule-toolbar {
    display: flex;
    justify-content: flex-end;
    gap: 8px;
    margin-bottom: 12px;
  }
</style>
