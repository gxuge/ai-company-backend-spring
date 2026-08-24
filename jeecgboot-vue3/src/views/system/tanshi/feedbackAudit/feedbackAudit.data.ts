import type { FormSchema } from '/@/components/Form';
import type { BasicColumn } from '/@/components/Table';

export const targetTypeOptions = [
  { label: '用户反馈', value: 'feedback' },
  { label: '评论/回复', value: 'comment' },
  { label: '追加内容', value: 'append' },
];

export const auditStatusOptions = [
  { label: '待审核', value: 'pending' },
  { label: '已通过', value: 'approved' },
  { label: '已驳回', value: 'rejected' },
];

export const processStatusOptions = [
  { label: '已收到', value: 'received' },
  { label: '处理中', value: 'processing' },
  { label: '已完成', value: 'completed' },
];

export const auditColumns: BasicColumn[] = [
  { title: '内容类型', dataIndex: 'targetType', width: 110, slots: { customRender: 'targetType' } },
  { title: '反馈ID', dataIndex: 'feedbackId', width: 100 },
  { title: '发布用户', dataIndex: 'userName', width: 140, slots: { customRender: 'user' } },
  { title: '标题/内容', dataIndex: 'content', width: 360, slots: { customRender: 'content' } },
  { title: '审核状态', dataIndex: 'auditStatus', width: 100, slots: { customRender: 'auditStatus' } },
  { title: '审核人', dataIndex: 'auditorName', width: 120 },
  { title: '提交时间', dataIndex: 'createdAt', width: 170 },
  { title: '审核时间', dataIndex: 'auditedAt', width: 170 },
];

export const auditSearchFormSchema: FormSchema[] = [
  {
    field: 'targetType',
    label: '内容类型',
    component: 'Select',
    colProps: { span: 6 },
    componentProps: { options: targetTypeOptions, allowClear: true, placeholder: '全部类型' },
  },
  {
    field: 'auditStatus',
    label: '审核状态',
    component: 'Select',
    colProps: { span: 6 },
    componentProps: { options: auditStatusOptions, allowClear: true, placeholder: '全部状态' },
    defaultValue: 'pending',
  },
  {
    field: 'keyword',
    label: '关键词',
    component: 'Input',
    colProps: { span: 8 },
    componentProps: { allowClear: true, placeholder: '标题、内容或发布用户' },
  },
];

export function getOptionLabel(options: { label: string; value: string }[], value?: string) {
  return options.find((item) => item.value === value)?.label || value || '-';
}
