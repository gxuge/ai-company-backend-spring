import { BasicColumn } from '/@/components/Table';
import { FormSchema } from '/@/components/Form';

export const paymentProviderOptions = [
  { label: 'Stripe', value: 'STRIPE' },
  { label: 'PayPal', value: 'PAYPAL' },
];

export const paymentStatusOptions = [
  { label: '创建中', value: 'CREATING' },
  { label: '待支付', value: 'PENDING' },
  { label: '支付成功', value: 'SUCCEEDED' },
  { label: '支付失败', value: 'FAILED' },
  { label: '已取消', value: 'CANCELED' },
];

export const columns: BasicColumn[] = [
  { title: '订单号', dataIndex: 'orderNo', width: 210 },
  { title: '用户账号', dataIndex: 'username', width: 130 },
  { title: '用户姓名', dataIndex: 'realname', width: 110 },
  { title: '会员套餐', dataIndex: 'planName', width: 110 },
  { title: '套餐周期', dataIndex: 'cycleType', width: 100 },
  { title: '支付渠道', dataIndex: 'provider', width: 100, slots: { customRender: 'provider' } },
  { title: '支付金额', dataIndex: 'amount', width: 120, slots: { customRender: 'amount' } },
  { title: '支付状态', dataIndex: 'paymentStatus', width: 110, slots: { customRender: 'paymentStatus' } },
  { title: '渠道交易ID', dataIndex: 'transactionId', width: 220, ellipsis: true },
  { title: '创建时间', dataIndex: 'createdAt', width: 170 },
  { title: '回调时间', dataIndex: 'callbackTime', width: 170 },
];

export const searchFormSchema: FormSchema[] = [
  {
    field: 'orderNo',
    label: '订单号',
    component: 'Input',
    colProps: { span: 6 },
    componentProps: { placeholder: '请输入会员订单号' },
  },
  {
    field: 'keyword',
    label: '用户',
    component: 'Input',
    colProps: { span: 6 },
    componentProps: { placeholder: '账号、姓名或用户ID' },
  },
  {
    field: 'provider',
    label: '支付渠道',
    component: 'Select',
    colProps: { span: 6 },
    componentProps: {
      options: paymentProviderOptions,
      allowClear: true,
      placeholder: '请选择支付渠道',
    },
  },
  {
    field: 'status',
    label: '支付状态',
    component: 'Select',
    colProps: { span: 6 },
    componentProps: {
      options: paymentStatusOptions,
      allowClear: true,
      placeholder: '请选择支付状态',
    },
  },
];
