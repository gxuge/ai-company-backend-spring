import type { FormSchema } from '/@/components/Form';
import type { BasicColumn } from '/@/components/Table';

export const recommendTypeOptions = [
  { label: '角色', value: 'ROLE' },
  { label: '故事', value: 'STORY' },
];

export const rangeModeOptions = [
  { label: '最近天数', value: 'RECENT_DAYS' },
  { label: '固定时间', value: 'FIXED' },
];

export const storageTypeOptions = [
  { label: '本地目录', value: 'LOCAL' },
  { label: 'OSS', value: 'OSS' },
];

export const executionStatusOptions = [
  { label: '等待中', value: 'WAITING' },
  { label: '执行中', value: 'RUNNING' },
  { label: '成功', value: 'SUCCESS' },
  { label: '失败', value: 'FAILED' },
];

export const triggerTypeOptions = [
  { label: '手动', value: 'MANUAL' },
  { label: '定时', value: 'SCHEDULED' },
];

export const etlTaskColumns: BasicColumn[] = [
  { title: '任务名称', dataIndex: 'taskName', width: 180, ellipsis: true },
  { title: '推荐类型', dataIndex: 'recommendType', width: 100, slots: { customRender: 'recommendType' } },
  { title: '数据范围', dataIndex: 'range', width: 210, slots: { customRender: 'range' } },
  { title: '切分比例', dataIndex: 'ratio', width: 120, slots: { customRender: 'ratio' } },
  { title: '存储', dataIndex: 'storageType', width: 100, slots: { customRender: 'storageType' } },
  { title: 'Cron', dataIndex: 'cronExpression', width: 170, ellipsis: true },
  { title: '状态', dataIndex: 'enabled', width: 90, slots: { customRender: 'enabled' } },
  { title: '运行状态', dataIndex: 'runningExecutionId', width: 100, slots: { customRender: 'running' } },
  { title: '最近触发', dataIndex: 'lastRunAt', width: 170 },
];

export const etlExecutionColumns: BasicColumn[] = [
  { title: '记录ID', dataIndex: 'id', width: 185, ellipsis: true },
  { title: '任务名称', dataIndex: 'taskName', width: 170, ellipsis: true },
  { title: '推荐类型', dataIndex: 'recommendType', width: 100, slots: { customRender: 'recommendType' } },
  { title: '触发方式', dataIndex: 'triggerType', width: 100, slots: { customRender: 'triggerType' } },
  { title: '状态', dataIndex: 'status', width: 95, slots: { customRender: 'status' } },
  { title: 'train', dataIndex: 'trainCount', width: 100 },
  { title: 'eval', dataIndex: 'evalCount', width: 100 },
  { title: '正样本', dataIndex: 'positiveCount', width: 100 },
  { title: '负样本', dataIndex: 'negativeCount', width: 100 },
  { title: '耗时', dataIndex: 'durationMs', width: 110, slots: { customRender: 'duration' } },
  { title: '开始时间', dataIndex: 'startedAt', width: 170 },
  { title: '结束时间', dataIndex: 'finishedAt', width: 170 },
  { title: '错误码', dataIndex: 'errorCode', width: 190, ellipsis: true },
];

export const etlTaskSearchSchema: FormSchema[] = [
  {
    field: 'keyword',
    label: '任务名称',
    component: 'Input',
    colProps: { span: 8 },
    componentProps: { allowClear: true },
  },
  {
    field: 'recommendType',
    label: '推荐类型',
    component: 'Select',
    colProps: { span: 8 },
    componentProps: { options: recommendTypeOptions, allowClear: true },
  },
  {
    field: 'enabled',
    label: '启用状态',
    component: 'Select',
    colProps: { span: 8 },
    componentProps: {
      options: [
        { label: '启用', value: 1 },
        { label: '停用', value: 0 },
      ],
      allowClear: true,
    },
  },
];

export const etlExecutionSearchSchema: FormSchema[] = [
  {
    field: 'keyword',
    label: '任务名称',
    component: 'Input',
    colProps: { span: 6 },
    componentProps: { allowClear: true },
  },
  {
    field: 'recommendType',
    label: '推荐类型',
    component: 'Select',
    colProps: { span: 6 },
    componentProps: { options: recommendTypeOptions, allowClear: true },
  },
  {
    field: 'status',
    label: '执行状态',
    component: 'Select',
    colProps: { span: 6 },
    componentProps: { options: executionStatusOptions, allowClear: true },
  },
  {
    field: 'triggerType',
    label: '触发方式',
    component: 'Select',
    colProps: { span: 6 },
    componentProps: { options: triggerTypeOptions, allowClear: true },
  },
];

export const etlTaskFormSchema: FormSchema[] = [
  {
    field: 'taskName',
    label: '任务名称',
    component: 'Input',
    required: true,
    colProps: { span: 12 },
  },
  {
    field: 'recommendType',
    label: '推荐类型',
    component: 'Select',
    required: true,
    colProps: { span: 12 },
    componentProps: { options: recommendTypeOptions },
  },
  {
    field: 'timeRangeMode',
    label: '时间范围',
    component: 'Select',
    required: true,
    colProps: { span: 12 },
    componentProps: { options: rangeModeOptions },
  },
  {
    field: 'recentDays',
    label: '最近天数',
    component: 'InputNumber',
    required: true,
    colProps: { span: 12 },
    componentProps: { min: 1, max: 3650, style: { width: '100%' } },
    ifShow: ({ values }) => values.timeRangeMode === 'RECENT_DAYS',
  },
  {
    field: 'startTime',
    label: '开始时间',
    component: 'DatePicker',
    required: true,
    colProps: { span: 12 },
    componentProps: { showTime: true, valueFormat: 'YYYY-MM-DD HH:mm:ss', style: { width: '100%' } },
    ifShow: ({ values }) => values.timeRangeMode === 'FIXED',
  },
  {
    field: 'endTime',
    label: '结束时间',
    component: 'DatePicker',
    required: true,
    colProps: { span: 12 },
    componentProps: { showTime: true, valueFormat: 'YYYY-MM-DD HH:mm:ss', style: { width: '100%' } },
    ifShow: ({ values }) => values.timeRangeMode === 'FIXED',
  },
  {
    field: 'scriptPath',
    label: '脚本路径',
    component: 'Input',
    required: true,
    colProps: { span: 24 },
    componentProps: { placeholder: '相对于服务端允许脚本根目录，例如 generate_easyrec_dataset.py' },
  },
  {
    field: 'outputDir',
    label: '输出目录',
    component: 'Input',
    required: true,
    colProps: { span: 16 },
    componentProps: { placeholder: '相对于服务端允许输出根目录，例如 role/daily' },
  },
  {
    field: 'storageType',
    label: '存储方式',
    component: 'Select',
    required: true,
    colProps: { span: 8 },
    componentProps: { options: storageTypeOptions },
  },
  {
    field: 'trainRatio',
    label: 'train 比例',
    component: 'InputNumber',
    required: true,
    colProps: { span: 8 },
    componentProps: { min: 0.00001, max: 0.99999, step: 0.05, precision: 5, style: { width: '100%' } },
  },
  {
    field: 'evalRatio',
    label: 'eval 比例',
    component: 'InputNumber',
    required: true,
    colProps: { span: 8 },
    componentProps: { min: 0.00001, max: 0.99999, step: 0.05, precision: 5, style: { width: '100%' } },
  },
  {
    field: 'timeoutSeconds',
    label: '超时秒数',
    component: 'InputNumber',
    required: true,
    colProps: { span: 8 },
    componentProps: { min: 10, max: 86400, style: { width: '100%' } },
  },
  {
    field: 'cronExpression',
    label: 'Cron',
    component: 'Input',
    colProps: { span: 18 },
    componentProps: { placeholder: '例如 0 0 2 * * ?' },
  },
  {
    field: 'enabled',
    label: '启用定时',
    component: 'Switch',
    colProps: { span: 6 },
  },
  {
    field: 'runParamsJson',
    label: '附加参数',
    component: 'InputTextArea',
    colProps: { span: 24 },
    componentProps: {
      rows: 5,
      placeholder: '{"min_interactions": 3, "negative_ratio": 4}',
    },
  },
];

export function optionLabel(options: { label: string; value: string }[], value?: string) {
  return options.find((item) => item.value === value)?.label || value || '-';
}
