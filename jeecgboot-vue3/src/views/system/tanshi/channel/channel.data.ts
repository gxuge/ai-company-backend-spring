import { BasicColumn } from '/@/components/Table';
import { FormSchema } from '/@/components/Form';

export const channelTargetTypeOptions = [
  { label: '角色', value: 'role' },
  { label: '故事', value: 'story' },
  { label: '两者都可用', value: 'both' },
];

export const channelStatusOptions = [
  { label: '启用', value: 'enabled' },
  { label: '停用', value: 'disabled' },
];

export const columns: BasicColumn[] = [
  {
    title: 'ID',
    dataIndex: 'id',
    width: 110,
  },
  {
    title: '渠道编码',
    dataIndex: 'channelCode',
    width: 180,
  },
  {
    title: '渠道名称',
    dataIndex: 'channelName',
    width: 180,
  },
  {
    title: '渠道图片',
    dataIndex: 'channelImageUrl',
    width: 120,
    slots: { customRender: 'channelImage' },
  },
  {
    title: '适用对象',
    dataIndex: 'targetType',
    width: 120,
    slots: { customRender: 'targetType' },
  },
  {
    title: '状态',
    dataIndex: 'status',
    width: 100,
    slots: { customRender: 'status' },
  },
  {
    title: '排序',
    dataIndex: 'sortOrder',
    width: 90,
  },
  {
    title: '备注',
    dataIndex: 'remark',
    ellipsis: true,
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
    componentProps: { placeholder: '渠道编码/名称' },
  },
  {
    field: 'targetType',
    label: '适用对象',
    component: 'Select',
    colProps: { span: 8 },
    componentProps: {
      options: channelTargetTypeOptions,
      allowClear: true,
      placeholder: '请选择适用对象',
    },
  },
  {
    field: 'status',
    label: '状态',
    component: 'Select',
    colProps: { span: 8 },
    componentProps: {
      options: channelStatusOptions,
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
    field: 'channelCode',
    label: '渠道编码',
    component: 'Input',
    required: true,
    componentProps: { placeholder: '请输入渠道编码' },
  },
  {
    field: 'channelName',
    label: '渠道名称',
    component: 'Input',
    required: true,
    componentProps: { placeholder: '请输入渠道名称' },
  },
  {
    field: 'targetType',
    label: '适用对象',
    component: 'Select',
    required: true,
    componentProps: {
      options: channelTargetTypeOptions,
      placeholder: '请选择适用对象',
    },
  },
  {
    field: 'channelImageUrl',
    label: '渠道图片',
    component: 'JImageUpload',
    componentProps: {
      fileMax: 1,
    },
  },
  {
    field: 'status',
    label: '状态',
    component: 'Select',
    componentProps: {
      options: channelStatusOptions,
      placeholder: '请选择状态',
    },
    defaultValue: 'enabled',
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
    field: 'remark',
    label: '备注',
    component: 'InputTextArea',
    componentProps: {
      rows: 4,
      placeholder: '请输入备注',
    },
  },
];
