import type { BasicColumn } from '/@/components/Table';
import type { FormSchema } from '/@/components/Form';

export const directionOptions = [
  { label: '收入', value: 'INCOME' },
  { label: '支出', value: 'EXPENSE' },
];

export const pointsStatusOptions = [
  { label: '处理中', value: 'PENDING' },
  { label: '成功', value: 'SUCCESS' },
  { label: '失败', value: 'FAILED' },
  { label: '已返还', value: 'REFUNDED' },
  { label: '已取消', value: 'CANCELED' },
];

export const pointsBizTypeOptions = [
  { label: '积分充值', value: 'RECHARGE' },
  { label: '会员赠送', value: 'MEMBER_GIFT' },
  { label: '活动奖励', value: 'ACTIVITY_REWARD' },
  { label: '签到奖励', value: 'SIGN_IN' },
  { label: '系统补偿', value: 'COMPENSATION' },
  { label: '积分返还', value: 'REFUND' },
  { label: 'AI 对话', value: 'AI_CHAT' },
  { label: '图片生成', value: 'IMAGE_GENERATE' },
  { label: '语音生成', value: 'VOICE_GENERATE' },
  { label: '故事生成', value: 'STORY_GENERATE' },
  { label: '角色创建', value: 'ROLE_CREATE' },
  { label: '3D 生成', value: 'THREE_D_GENERATE' },
  { label: '高级功能', value: 'ADVANCED_FEATURE' },
  { label: '后台增加', value: 'ADMIN_ADD' },
  { label: '后台扣减', value: 'ADMIN_DEDUCT' },
];

export const rechargeStatusOptions = [
  { label: '创建中', value: 'CREATING' },
  { label: '待支付', value: 'PENDING' },
  { label: '支付成功', value: 'SUCCEEDED' },
  { label: '支付失败', value: 'FAILED' },
  { label: '已取消', value: 'CANCELED' },
];

export const paymentChannelOptions = [
  { label: 'Stripe', value: 'STRIPE' },
  { label: 'PayPal', value: 'PAYPAL' },
];

export const operationOptions = [
  { label: '增加积分', value: 'ADD' },
  { label: '扣减积分', value: 'DEDUCT' },
];

export const enabledStatusOptions = [
  { label: '启用', value: 1 },
  { label: '停用', value: 0 },
];

export const accountColumns: BasicColumn[] = [
  { title: '用户账号', dataIndex: 'username', width: 140 },
  { title: '用户姓名', dataIndex: 'nickname', width: 120 },
  { title: '邮箱', dataIndex: 'email', width: 190, ellipsis: true },
  { title: '用户ID', dataIndex: 'userId', width: 210, ellipsis: true },
  { title: '当前余额', dataIndex: 'balance', width: 110 },
  { title: '累计获得', dataIndex: 'totalIncome', width: 110 },
  { title: '累计消费', dataIndex: 'totalExpense', width: 110 },
  { title: '最近变动', dataIndex: 'updatedAt', width: 170 },
];

export const accountSearchFormSchema: FormSchema[] = [
  {
    field: 'keyword',
    label: '用户关键词',
    component: 'Input',
    colProps: { span: 8 },
    componentProps: { placeholder: '账号、姓名、邮箱或用户ID' },
  },
  {
    field: 'minBalance',
    label: '最低余额',
    component: 'InputNumber',
    colProps: { span: 4 },
    componentProps: { min: 0, precision: 0, style: { width: '100%' } },
  },
  {
    field: 'maxBalance',
    label: '最高余额',
    component: 'InputNumber',
    colProps: { span: 4 },
    componentProps: { min: 0, precision: 0, style: { width: '100%' } },
  },
];

export const transactionColumns: BasicColumn[] = [
  { title: '流水号', dataIndex: 'transactionNo', width: 220, ellipsis: true },
  { title: '用户账号', dataIndex: 'username', width: 130 },
  { title: '用户姓名', dataIndex: 'realname', width: 110 },
  { title: '业务类型', dataIndex: 'bizType', width: 125, slots: { customRender: 'bizType' } },
  { title: '方向', dataIndex: 'direction', width: 80, slots: { customRender: 'direction' } },
  { title: '积分数量', dataIndex: 'amount', width: 100, slots: { customRender: 'amount' } },
  { title: '变动前', dataIndex: 'beforeBalance', width: 95 },
  { title: '变动后', dataIndex: 'afterBalance', width: 95 },
  { title: '状态', dataIndex: 'status', width: 90, slots: { customRender: 'status' } },
  { title: '说明', dataIndex: 'description', width: 180, ellipsis: true },
  { title: '操作人', dataIndex: 'operatorId', width: 150, ellipsis: true },
  { title: '创建时间', dataIndex: 'createdAt', width: 170 },
];

export const transactionSearchFormSchema: FormSchema[] = [
  {
    field: 'keyword',
    label: '关键词',
    component: 'Input',
    colProps: { span: 6 },
    componentProps: { placeholder: '用户、流水号或用户ID' },
  },
  {
    field: 'direction',
    label: '方向',
    component: 'Select',
    colProps: { span: 4 },
    componentProps: { options: directionOptions, allowClear: true },
  },
  {
    field: 'bizType',
    label: '业务类型',
    component: 'Select',
    colProps: { span: 6 },
    componentProps: { options: pointsBizTypeOptions, allowClear: true, showSearch: true },
  },
  {
    field: 'status',
    label: '状态',
    component: 'Select',
    colProps: { span: 4 },
    componentProps: { options: pointsStatusOptions, allowClear: true },
  },
  {
    field: 'timeRange',
    label: '创建时间',
    component: 'RangePicker',
    colProps: { span: 8 },
    componentProps: { showTime: true, valueFormat: 'YYYY-MM-DD HH:mm:ss' },
  },
];

export const rechargeColumns: BasicColumn[] = [
  { title: '订单号', dataIndex: 'orderNo', width: 210, ellipsis: true },
  { title: '用户账号', dataIndex: 'username', width: 130 },
  { title: '商品', dataIndex: 'productName', width: 150 },
  { title: '购买积分', dataIndex: 'points', width: 95 },
  { title: '赠送积分', dataIndex: 'giftPoints', width: 95 },
  { title: '实付金额', dataIndex: 'actualAmount', width: 110, slots: { customRender: 'amount' } },
  { title: '支付渠道', dataIndex: 'paymentChannel', width: 100, slots: { customRender: 'channel' } },
  { title: '状态', dataIndex: 'status', width: 100, slots: { customRender: 'status' } },
  { title: '支付意图', dataIndex: 'paymentIntentId', width: 200, ellipsis: true },
  { title: '到账流水', dataIndex: 'pointsTransactionNo', width: 200, ellipsis: true },
  { title: '支付时间', dataIndex: 'payTime', width: 170 },
  { title: '创建时间', dataIndex: 'createdAt', width: 170 },
];

export const rechargeSearchFormSchema: FormSchema[] = [
  {
    field: 'keyword',
    label: '关键词',
    component: 'Input',
    colProps: { span: 6 },
    componentProps: { placeholder: '用户、订单号或用户ID' },
  },
  {
    field: 'paymentChannel',
    label: '支付渠道',
    component: 'Select',
    colProps: { span: 5 },
    componentProps: { options: paymentChannelOptions, allowClear: true },
  },
  {
    field: 'status',
    label: '支付状态',
    component: 'Select',
    colProps: { span: 5 },
    componentProps: { options: rechargeStatusOptions, allowClear: true },
  },
  {
    field: 'timeRange',
    label: '创建时间',
    component: 'RangePicker',
    colProps: { span: 8 },
    componentProps: { showTime: true, valueFormat: 'YYYY-MM-DD HH:mm:ss' },
  },
];

export const productColumns = [
  { title: '商品名称', dataIndex: 'name', width: 180 },
  { title: '购买积分', dataIndex: 'points', width: 100 },
  { title: '赠送积分', dataIndex: 'giftPoints', width: 100 },
  { title: '原价', dataIndex: 'originalAmount', width: 100 },
  { title: '实付金额', dataIndex: 'actualAmount', width: 100 },
  { title: '币种', dataIndex: 'currency', width: 80 },
  { title: '排序', dataIndex: 'sort', width: 70 },
  { title: '状态', dataIndex: 'status', width: 80 },
  { title: '更新时间', dataIndex: 'updatedAt', width: 170 },
  { title: '操作', dataIndex: 'action', width: 80, fixed: 'right' },
];

export const giftRuleColumns = [
  { title: '会员等级', dataIndex: 'planName', width: 160 },
  { title: '会员套餐', dataIndex: 'productName', width: 180 },
  { title: '赠送积分', dataIndex: 'giftPoints', width: 110 },
  { title: '状态', dataIndex: 'status', width: 80 },
  { title: '更新时间', dataIndex: 'updatedAt', width: 170 },
  { title: '操作', dataIndex: 'action', width: 80, fixed: 'right' },
];

export const adjustFormSchema: FormSchema[] = [
  { field: 'userId', label: '用户ID', component: 'Input', required: true, componentProps: { disabled: true } },
  {
    field: 'operation',
    label: '调整方式',
    component: 'Select',
    required: true,
    componentProps: { options: operationOptions },
    defaultValue: 'ADD',
  },
  {
    field: 'amount',
    label: '积分数量',
    component: 'InputNumber',
    required: true,
    componentProps: { min: 1, precision: 0, style: { width: '100%' } },
  },
  {
    field: 'reason',
    label: '调整原因',
    component: 'InputTextArea',
    required: true,
    componentProps: { rows: 3, placeholder: '请输入调整原因' },
  },
  { field: 'idempotencyKey', label: '幂等Key', component: 'Input', show: false },
];

export const productFormSchema: FormSchema[] = [
  { field: 'id', label: 'ID', component: 'Input', show: false },
  { field: 'name', label: '商品名称', component: 'Input', required: true, componentProps: { placeholder: '例如：星钻月卡' } },
  {
    field: 'points',
    label: '购买积分',
    component: 'InputNumber',
    required: true,
    componentProps: { min: 1, precision: 0, style: { width: '100%' } },
  },
  {
    field: 'giftPoints',
    label: '赠送积分',
    component: 'InputNumber',
    required: true,
    componentProps: { min: 0, precision: 0, style: { width: '100%' } },
  },
  {
    field: 'originalAmount',
    label: '原价',
    component: 'InputNumber',
    required: true,
    componentProps: { min: 0.01, precision: 2, style: { width: '100%' } },
  },
  {
    field: 'actualAmount',
    label: '实付金额',
    component: 'InputNumber',
    required: true,
    componentProps: { min: 0.01, precision: 2, style: { width: '100%' } },
  },
  { field: 'currency', label: '币种', component: 'Input', required: true, defaultValue: 'USD' },
  {
    field: 'status',
    label: '状态',
    component: 'Select',
    required: true,
    componentProps: { options: enabledStatusOptions },
    defaultValue: 1,
  },
  {
    field: 'sort',
    label: '排序',
    component: 'InputNumber',
    componentProps: { min: 0, precision: 0, style: { width: '100%' } },
    defaultValue: 0,
  },
];

export function getGiftRuleFormSchema(
  planOptions: { label: string; value: number }[],
  productOptions: { label: string; value: number }[]
): FormSchema[] {
  return [
    { field: 'id', label: 'ID', component: 'Input', show: false },
    {
      field: 'planId',
      label: '会员等级',
      component: 'Select',
      required: true,
      componentProps: { options: planOptions, showSearch: true, optionFilterProp: 'label', placeholder: '请选择会员等级' },
    },
    {
      field: 'productId',
      label: '会员套餐',
      component: 'Select',
      required: true,
      componentProps: { options: productOptions, showSearch: true, optionFilterProp: 'label', placeholder: '请选择套餐或等级默认规则' },
    },
    {
      field: 'giftPoints',
      label: '赠送积分',
      component: 'InputNumber',
      required: true,
      componentProps: { min: 1, precision: 0, style: { width: '100%' } },
    },
    {
      field: 'status',
      label: '状态',
      component: 'Select',
      required: true,
      componentProps: { options: enabledStatusOptions },
      defaultValue: 1,
    },
  ];
}

export function getOptionLabel(options: { label: string; value: string | number }[], value: string | number | undefined) {
  return options.find((item) => item.value === value)?.label || value || '-';
}
