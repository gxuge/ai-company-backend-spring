import type { BasicColumn } from '/@/components/Table';
import type { FormSchema } from '/@/components/Form';

export const rewardEventTypeOptions = [
  { label: '签到完成', value: 'SIGN_COMPLETED' },
  { label: '任务奖励领取', value: 'TASK_REWARD_RECEIVED' },
  { label: '会员开通', value: 'MEMBER_ACTIVATED' },
];

export const rewardEventStatusOptions = [
  { label: '待处理', value: 'PENDING' },
  { label: '处理中', value: 'PROCESSING' },
  { label: '成功', value: 'SUCCESS' },
  { label: '失败', value: 'FAILED' },
];

export const rewardEventColumns: BasicColumn[] = [
  { title: '事件ID', dataIndex: 'eventId', width: 235, ellipsis: true },
  { title: '事件类型', dataIndex: 'eventType', width: 135, slots: { customRender: 'eventType' } },
  { title: '用户账号', dataIndex: 'username', width: 130 },
  { title: '用户姓名', dataIndex: 'realname', width: 110 },
  { title: '业务ID', dataIndex: 'bizId', width: 190, ellipsis: true },
  { title: '状态', dataIndex: 'status', width: 90, slots: { customRender: 'status' } },
  { title: '奖励数量', dataIndex: 'rewardValue', width: 100, slots: { customRender: 'rewardValue' } },
  { title: '执行次数', dataIndex: 'retry', width: 95, slots: { customRender: 'retry' } },
  { title: '机器错误码', dataIndex: 'lastErrorCode', width: 190, ellipsis: true },
  { title: '积分流水号', dataIndex: 'pointsTransactionNo', width: 210, ellipsis: true },
  { title: '创建时间', dataIndex: 'createdAt', width: 170 },
  { title: '处理时间', dataIndex: 'processedAt', width: 170 },
];

export const rewardEventSearchFormSchema: FormSchema[] = [
  {
    field: 'keyword',
    label: '关键词',
    component: 'Input',
    colProps: { span: 6 },
    componentProps: { placeholder: '用户、事件ID或业务ID' },
  },
  {
    field: 'eventType',
    label: '事件类型',
    component: 'Select',
    colProps: { span: 5 },
    componentProps: { options: rewardEventTypeOptions, allowClear: true },
  },
  {
    field: 'status',
    label: '执行状态',
    component: 'Select',
    colProps: { span: 5 },
    componentProps: { options: rewardEventStatusOptions, allowClear: true },
  },
  {
    field: 'timeRange',
    label: '创建时间',
    component: 'RangePicker',
    colProps: { span: 8 },
    componentProps: { showTime: true, valueFormat: 'YYYY-MM-DD HH:mm:ss' },
  },
];

export function getRewardOptionLabel(options: { label: string; value: string }[], value?: string) {
  return options.find((item) => item.value === value)?.label || value || '-';
}
