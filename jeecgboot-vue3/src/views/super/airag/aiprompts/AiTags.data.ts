import { BasicColumn } from '/@/components/Table';
import { FormSchema } from '/@/components/Table';

export const presetTagColumns: BasicColumn[] = [
  {
    title: '标签名',
    align: 'center',
    dataIndex: 'name',
  },
  {
    title: '类型',
    align: 'center',
    dataIndex: 'typeName',
  },
];

export const presetTagSearchFormSchema: FormSchema[] = [
  {
    label: '标签名',
    field: 'name',
    component: 'Input',
    colProps: { span: 8 },
  },
];

export const presetTagFormSchema: FormSchema[] = [
  {
    label: '预设ID',
    field: 'presetId',
    component: 'Input',
    show: false,
  },
  {
    label: '标签名',
    field: 'tagName',
    component: 'Input',
    required: true,
    componentProps: {
      placeholder: '请输入标签名',
    },
  },
  {
    label: '标签类型',
    field: 'tagType',
    component: 'Select',
    required: true,
    componentProps: {
      options: [],
      placeholder: '请选择标签类型',
    },
  },
  {
    label: '是否必选',
    field: 'required',
    component: 'Select',
    defaultValue: 0,
    componentProps: {
      options: [
        { label: '否', value: 0 },
        { label: '是', value: 1 },
      ],
    },
  },
  {
    label: '权重覆盖',
    field: 'weightOverride',
    component: 'InputNumber',
    componentProps: {
      precision: 0,
    },
  },
  {
    label: '排序',
    field: 'sortOrder',
    component: 'InputNumber',
    defaultValue: 0,
    componentProps: {
      min: 0,
      precision: 0,
    },
  },
  {
    label: '',
    field: 'id',
    component: 'Input',
    show: false,
  },
];
