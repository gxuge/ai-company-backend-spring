import type { BasicColumn } from '/@/components/Table';
import type { FormSchema } from '/@/components/Form';

export const categoryOptions = [
  { label: '全部分类', value: 'ALL' },
  { label: '会员订阅', value: 'MEMBERSHIP' },
  { label: '积分充值', value: 'RECHARGE' },
  { label: '积分流水', value: 'POINTS' },
];

export const moneyDirectionOptions = [
  { label: '全部现金方向', value: 'ALL' },
  { label: '现金收入', value: 'INCOME' },
  { label: '现金支出', value: 'EXPENSE' },
  { label: '无现金变化', value: 'NONE' },
];

export const pointsDirectionOptions = [
  { label: '全部积分方向', value: 'ALL' },
  { label: '积分收入', value: 'INCOME' },
  { label: '积分支出', value: 'EXPENSE' },
  { label: '无积分变化', value: 'NONE' },
];

export const billingStatusOptions = [
  { label: '创建中', value: 'CREATING' },
  { label: '待支付', value: 'PENDING' },
  { label: '支付成功', value: 'SUCCEEDED' },
  { label: '成功', value: 'SUCCESS' },
  { label: '支付失败', value: 'FAILED' },
  { label: '已返还', value: 'REFUNDED' },
  { label: '已取消', value: 'CANCELED' },
];

export const billingColumns: BasicColumn[] = [
  { title: '账单类型', dataIndex: 'recordType', width: 100, slots: { customRender: 'recordType' } },
  { title: '用户', dataIndex: 'nickname', width: 130 },
  { title: '用户ID', dataIndex: 'userId', width: 190, ellipsis: true },
  { title: '账单名称', dataIndex: 'title', width: 170, ellipsis: true },
  { title: '订单/流水号', dataIndex: 'orderNo', width: 220, ellipsis: true },
  { title: '现金变化', dataIndex: 'moneyAmount', width: 120, slots: { customRender: 'money' } },
  { title: '积分变化', dataIndex: 'pointsAmount', width: 110, slots: { customRender: 'points' } },
  { title: '状态', dataIndex: 'status', width: 100, slots: { customRender: 'status' } },
  { title: '创建时间', dataIndex: 'createdAt', width: 170 },
];

export const billingSearchFormSchema: FormSchema[] = [
  {
    field: 'keyword',
    label: '关键词',
    component: 'Input',
    colProps: { span: 6 },
    componentProps: { placeholder: '用户、订单号、流水号或用户ID' },
  },
  {
    field: 'category',
    label: '账单分类',
    component: 'Select',
    colProps: { span: 4 },
    componentProps: { options: categoryOptions },
    defaultValue: 'ALL',
  },
  {
    field: 'moneyDirection',
    label: '现金方向',
    component: 'Select',
    colProps: { span: 5 },
    componentProps: { options: moneyDirectionOptions },
    defaultValue: 'ALL',
  },
  {
    field: 'pointsDirection',
    label: '积分方向',
    component: 'Select',
    colProps: { span: 5 },
    componentProps: { options: pointsDirectionOptions },
    defaultValue: 'ALL',
  },
  {
    field: 'status',
    label: '状态',
    component: 'Select',
    colProps: { span: 4 },
    componentProps: { options: billingStatusOptions, allowClear: true },
  },
  {
    field: 'timeRange',
    label: '创建时间',
    component: 'RangePicker',
    colProps: { span: 8 },
    componentProps: { showTime: true, valueFormat: 'YYYY-MM-DD HH:mm:ss' },
  },
];

export function getOptionLabel(options: { label: string; value: string | number }[], value: string | number | undefined) {
  return options.find((item) => item.value === value)?.label || value || '-';
}
