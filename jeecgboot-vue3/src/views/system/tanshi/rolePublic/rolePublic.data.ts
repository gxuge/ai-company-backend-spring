import { BasicColumn } from '/@/components/Table';
import { FormSchema } from '/@/components/Form';

export const rolePublicStatusOptions = [
  { label: '草稿', value: 'draft' },
  { label: '待提交', value: 'pending' },
  { label: '已上架', value: 'online' },
  { label: '已下架', value: 'offline' },
  { label: '已驳回', value: 'rejected' },
];

export const columns: BasicColumn[] = [
  {
    title: 'ID',
    dataIndex: 'id',
    width: 110,
  },
  {
    title: '角色ID',
    dataIndex: 'roleId',
    width: 120,
  },
  {
    title: '角色名称',
    dataIndex: 'roleName',
    width: 160,
  },
  {
    title: '所属用户',
    dataIndex: 'ownerDisplayName',
    width: 180,
  },
  {
    title: '渠道',
    dataIndex: 'channelName',
    width: 160,
  },
  {
    title: '状态',
    dataIndex: 'status',
    width: 100,
    slots: { customRender: 'status' },
  },
  {
    title: '展示标题',
    dataIndex: 'displayTitle',
    width: 180,
    ellipsis: true,
  },
  {
    title: '排序',
    dataIndex: 'sortOrder',
    width: 90,
  },
  {
    title: '上架时间',
    dataIndex: 'publishedAt',
    width: 180,
  },
  {
    title: '更新时间',
    dataIndex: 'updateTime',
    width: 180,
  },
];

export const searchFormSchema: FormSchema[] = [
  {
    field: 'keyword',
    label: '关键词',
    component: 'Input',
    colProps: { span: 8 },
    componentProps: { placeholder: '角色名/展示标题/简介' },
  },
  {
    field: 'channelCode',
    label: '渠道',
    component: 'Select',
    colProps: { span: 8 },
    componentProps: {
      options: [],
      allowClear: true,
      placeholder: '请选择渠道',
    },
  },
  {
    field: 'status',
    label: '状态',
    component: 'Select',
    colProps: { span: 8 },
    componentProps: {
      options: rolePublicStatusOptions,
      allowClear: true,
      placeholder: '请选择状态',
    },
  },
];

export const formSchema: FormSchema[] = [
  {
    field: 'id',
    label: 'ID',
    component: 'Input',
    show: false,
  },
  {
    field: 'ownerUserId',
    label: '所属用户',
    component: 'ApiSelect',
    required: true,
    componentProps: {
      options: [],
      placeholder: '请选择所属用户',
    },
    dynamicDisabled: ({ values }) => !!values.id,
  },
  {
    field: 'roleId',
    label: '选择角色',
    component: 'ApiSelect',
    required: true,
    componentProps: { options: [], placeholder: '请先选择所属用户' },
    dynamicDisabled: ({ values }) => !!values.id,
  },
  {
    field: 'channelCode',
    label: '渠道编码',
    component: 'Select',
    required: true,
    componentProps: {
      options: [],
      placeholder: '请选择渠道',
    },
    dynamicDisabled: ({ values }) => !!values.id,
  },
  {
    field: 'displayTitle',
    label: '展示标题',
    component: 'Input',
    componentProps: { placeholder: '请输入展示标题' },
  },
  {
    field: 'displaySubtitle',
    label: '展示副标题',
    component: 'Input',
    componentProps: { placeholder: '请输入展示副标题' },
  },
  {
    field: 'coverImageUrl',
    label: '封面图',
    component: 'JImageUpload',
  },
  {
    field: 'introText',
    label: '展示简介',
    component: 'InputTextArea',
    componentProps: { rows: 4, placeholder: '请输入展示简介' },
  },
  {
    field: 'sortOrder',
    label: '排序',
    component: 'InputNumber',
    componentProps: {
      min: 0,
      precision: 0,
      style: { width: '100%' },
    },
    defaultValue: 0,
  },
  {
    field: 'extJson',
    label: '扩展配置',
    component: 'InputTextArea',
    componentProps: { rows: 4, placeholder: '请输入扩展JSON' },
  },
];
