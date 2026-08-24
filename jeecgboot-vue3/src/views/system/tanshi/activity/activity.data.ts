import type { BasicColumn } from '/@/components/Table';
import type { FormSchema } from '/@/components/Form';

export const taskTypeOptions = [
  { label: '签到', value: 'SIGN' },
  { label: '任务', value: 'TASK' },
  { label: '成就', value: 'ACHIEVEMENT' },
  { label: '活动', value: 'EVENT' },
];

export const categoryOptions = [
  { label: '每日', value: 'DAILY' },
  { label: '每周', value: 'WEEKLY' },
  { label: '长期', value: 'LONG_TERM' },
];

export const conditionTypeOptions = [
  { label: '登录', value: 'LOGIN' },
  { label: '聊天次数', value: 'CHAT_COUNT' },
  { label: '创建角色', value: 'ROLE_CREATE' },
  { label: '创建故事', value: 'STORY_CREATE' },
  { label: '生成图片', value: 'IMAGE_GENERATE' },
  { label: '使用语音', value: 'VOICE_USE' },
];

export const rewardTypeOptions = [{ label: '星钻', value: 'STAR_DIAMOND' }];

export const taskStatusOptions = [
  { label: '启用', value: 'ENABLED' },
  { label: '停用', value: 'DISABLED' },
];

export const userTaskStatusOptions = [
  { label: '进行中', value: 'DOING' },
  { label: '已完成', value: 'COMPLETED' },
];

export const rewardStatusOptions = [
  { label: '未领取', value: 'UNCLAIMED' },
  { label: '已领取', value: 'CLAIMED' },
];

export const memberLevelOptions = [
  { label: '普通用户', value: 'NORMAL' },
  { label: 'VIP', value: 'VIP' },
  { label: 'SVIP', value: 'SVIP' },
];

export const sourceTypeOptions = [
  { label: '签到', value: 'SIGN' },
  { label: '任务', value: 'TASK' },
  { label: '成就', value: 'ACHIEVEMENT' },
  { label: '活动', value: 'EVENT' },
];

export const taskColumns: BasicColumn[] = [
  { title: '任务名称', dataIndex: 'taskName', width: 180 },
  { title: '类型', dataIndex: 'taskType', width: 100, slots: { customRender: 'taskType' } },
  { title: '周期', dataIndex: 'category', width: 90, slots: { customRender: 'category' } },
  { title: '完成条件', dataIndex: 'conditionType', width: 120, slots: { customRender: 'conditionType' } },
  { title: '目标值', dataIndex: 'conditionValue', width: 90 },
  { title: '奖励', dataIndex: 'rewardValue', width: 90, slots: { customRender: 'reward' } },
  { title: '生效时间', dataIndex: 'startTime', width: 170 },
  { title: '失效时间', dataIndex: 'endTime', width: 170 },
  { title: '状态', dataIndex: 'status', width: 90, slots: { customRender: 'status' } },
  { title: '排序', dataIndex: 'sort', width: 70 },
];

export const taskSearchFormSchema: FormSchema[] = [
  {
    field: 'keyword',
    label: '任务关键词',
    component: 'Input',
    colProps: { span: 6 },
    componentProps: { placeholder: '请输入任务名称' },
  },
  {
    field: 'taskType',
    label: '任务类型',
    component: 'Select',
    colProps: { span: 6 },
    componentProps: { options: taskTypeOptions, allowClear: true },
  },
  {
    field: 'category',
    label: '任务周期',
    component: 'Select',
    colProps: { span: 6 },
    componentProps: { options: categoryOptions, allowClear: true },
  },
  {
    field: 'status',
    label: '状态',
    component: 'Select',
    colProps: { span: 6 },
    componentProps: { options: taskStatusOptions, allowClear: true },
  },
];

export const taskFormSchema: FormSchema[] = [
  { field: 'id', label: 'ID', component: 'Input', show: false },
  {
    field: 'taskName',
    label: '任务名称',
    component: 'Input',
    required: true,
    componentProps: { placeholder: '请输入任务名称' },
  },
  {
    field: 'taskType',
    label: '任务类型',
    component: 'Select',
    required: true,
    componentProps: { options: taskTypeOptions, placeholder: '请选择任务类型' },
  },
  {
    field: 'category',
    label: '任务周期',
    component: 'Select',
    required: true,
    componentProps: { options: categoryOptions, placeholder: '请选择任务周期' },
  },
  {
    field: 'conditionType',
    label: '完成条件',
    component: 'Select',
    required: true,
    componentProps: { options: conditionTypeOptions, placeholder: '请选择完成条件' },
  },
  {
    field: 'conditionValue',
    label: '目标值',
    component: 'InputNumber',
    required: true,
    componentProps: { min: 1, precision: 0, style: { width: '100%' } },
  },
  {
    field: 'rewardType',
    label: '奖励类型',
    component: 'Select',
    required: true,
    componentProps: { options: rewardTypeOptions, placeholder: '请选择奖励类型' },
  },
  {
    field: 'rewardValue',
    label: '奖励星钻',
    component: 'InputNumber',
    required: true,
    componentProps: { min: 1, precision: 0, style: { width: '100%' } },
  },
  {
    field: 'startTime',
    label: '开始时间',
    component: 'DatePicker',
    componentProps: { showTime: true, valueFormat: 'YYYY-MM-DD HH:mm:ss', style: { width: '100%' } },
  },
  {
    field: 'endTime',
    label: '结束时间',
    component: 'DatePicker',
    componentProps: { showTime: true, valueFormat: 'YYYY-MM-DD HH:mm:ss', style: { width: '100%' } },
  },
  {
    field: 'status',
    label: '状态',
    component: 'Select',
    required: true,
    componentProps: { options: taskStatusOptions },
    defaultValue: 'ENABLED',
  },
  {
    field: 'sort',
    label: '排序',
    component: 'InputNumber',
    componentProps: { min: 0, precision: 0, style: { width: '100%' } },
    defaultValue: 0,
  },
  {
    field: 'description',
    label: '任务描述',
    component: 'InputTextArea',
    componentProps: { rows: 3, placeholder: '请输入任务描述' },
  },
];

export const userTaskColumns: BasicColumn[] = [
  { title: '用户账号', dataIndex: 'username', width: 140 },
  { title: '用户姓名', dataIndex: 'realname', width: 110 },
  { title: '用户ID', dataIndex: 'userId', width: 210, ellipsis: true },
  { title: '任务', dataIndex: 'taskName', width: 180 },
  { title: '周期', dataIndex: 'cycleKey', width: 120 },
  { title: '进度', dataIndex: 'progress', width: 110, slots: { customRender: 'progress' } },
  { title: '完成状态', dataIndex: 'status', width: 100, slots: { customRender: 'status' } },
  { title: '领取状态', dataIndex: 'rewardStatus', width: 100, slots: { customRender: 'rewardStatus' } },
  { title: '完成时间', dataIndex: 'completeTime', width: 170 },
  { title: '领取时间', dataIndex: 'rewardTime', width: 170 },
];

export const userTaskSearchFormSchema: FormSchema[] = [
  {
    field: 'userKeyword',
    label: '用户',
    component: 'Input',
    colProps: { span: 6 },
    componentProps: { placeholder: '账号、姓名或用户ID' },
  },
  {
    field: 'taskId',
    label: '任务ID',
    component: 'InputNumber',
    colProps: { span: 6 },
    componentProps: { min: 1, precision: 0, style: { width: '100%' }, placeholder: '请输入任务ID' },
  },
  {
    field: 'status',
    label: '完成状态',
    component: 'Select',
    colProps: { span: 6 },
    componentProps: { options: userTaskStatusOptions, allowClear: true },
  },
  {
    field: 'rewardStatus',
    label: '领取状态',
    component: 'Select',
    colProps: { span: 6 },
    componentProps: { options: rewardStatusOptions, allowClear: true },
  },
];

export const rewardColumns: BasicColumn[] = [
  { title: '用户账号', dataIndex: 'username', width: 140 },
  { title: '用户姓名', dataIndex: 'realname', width: 110 },
  { title: '用户ID', dataIndex: 'userId', width: 210, ellipsis: true },
  { title: '任务', dataIndex: 'taskName', width: 170 },
  { title: '来源', dataIndex: 'sourceType', width: 90, slots: { customRender: 'sourceType' } },
  { title: '会员等级', dataIndex: 'memberLevel', width: 100, slots: { customRender: 'memberLevel' } },
  { title: '基础奖励', dataIndex: 'baseRewardValue', width: 100 },
  { title: '额外奖励', dataIndex: 'extraRewardValue', width: 100 },
  { title: '最终奖励', dataIndex: 'rewardValue', width: 100 },
  { title: '积分流水号', dataIndex: 'pointsTransactionNo', width: 210, ellipsis: true },
  { title: '发放时间', dataIndex: 'createdAt', width: 170 },
];

export const rewardSearchFormSchema: FormSchema[] = [
  {
    field: 'userKeyword',
    label: '用户',
    component: 'Input',
    colProps: { span: 8 },
    componentProps: { placeholder: '账号、姓名或用户ID' },
  },
  {
    field: 'rewardType',
    label: '奖励类型',
    component: 'Select',
    colProps: { span: 8 },
    componentProps: { options: rewardTypeOptions, allowClear: true },
  },
  {
    field: 'timeRange',
    label: '发放时间',
    component: 'RangePicker',
    colProps: { span: 8 },
    componentProps: { showTime: true, valueFormat: 'YYYY-MM-DD HH:mm:ss' },
  },
];

export const rewardRuleColumns: BasicColumn[] = [
  { title: '任务', dataIndex: 'taskName', width: 220 },
  { title: '任务ID', dataIndex: 'taskId', width: 100 },
  { title: '会员等级', dataIndex: 'memberLevel', width: 110 },
  { title: '额外奖励类型', dataIndex: 'extraRewardType', width: 130 },
  { title: '额外奖励', dataIndex: 'extraRewardValue', width: 110 },
  { title: '状态', dataIndex: 'status', width: 90 },
  { title: '更新时间', dataIndex: 'updatedAt', width: 170 },
];

export function getRewardRuleFormSchema(taskOptions: { label: string; value: number }[]): FormSchema[] {
  return [
    { field: 'id', label: 'ID', component: 'Input', show: false },
    {
      field: 'taskId',
      label: '活动任务',
      component: 'Select',
      required: true,
      componentProps: { options: taskOptions, placeholder: '请选择活动任务', showSearch: true, optionFilterProp: 'label' },
    },
    {
      field: 'memberLevel',
      label: '会员等级',
      component: 'Select',
      required: true,
      componentProps: { options: memberLevelOptions, placeholder: '请选择会员等级' },
    },
    {
      field: 'extraRewardType',
      label: '额外奖励类型',
      component: 'Select',
      required: true,
      componentProps: { options: rewardTypeOptions, placeholder: '请选择奖励类型' },
    },
    {
      field: 'extraRewardValue',
      label: '额外奖励',
      component: 'InputNumber',
      required: true,
      componentProps: { min: 0, precision: 0, style: { width: '100%' } },
    },
    {
      field: 'status',
      label: '状态',
      component: 'Select',
      required: true,
      componentProps: {
        options: [
          { label: '启用', value: 1 },
          { label: '停用', value: 0 },
        ],
      },
      defaultValue: 1,
    },
  ];
}

export function getOptionLabel(options: { label: string; value: string | number }[], value: string | number | undefined) {
  return options.find((item) => item.value === value)?.label || value || '-';
}
