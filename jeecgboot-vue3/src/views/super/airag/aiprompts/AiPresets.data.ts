import { BasicColumn } from '/@/components/Table';
import { FormSchema } from '/@/components/Table';

export const presetColumns: BasicColumn[] = [
  {
    title: '预设名称',
    align: 'center',
    dataIndex: 'name',
  },
  {
    title: '目标类型',
    align: 'center',
    dataIndex: 'targetType',
    customRender: ({ text }) => {
      if (text === 'character') return '角色';
      if (text === 'story') return '故事';
      if (text === 'both') return '角色+故事';
      return text || '-';
    },
  },
  {
    title: '描述',
    align: 'center',
    dataIndex: 'description',
  },
];

export const presetSearchFormSchema: FormSchema[] = [
  {
    label: '预设名称',
    field: 'name',
    component: 'Input',
    colProps: { span: 6 },
  },
  {
    label: '目标类型',
    field: 'targetType',
    component: 'Select',
    componentProps: {
      options: [
        { label: '角色', value: 'character' },
        { label: '故事', value: 'story' },
        { label: '两者', value: 'both' },
      ],
      allowClear: true,
    },
    colProps: { span: 6 },
  },
];

export const presetFormSchema: FormSchema[] = [
  {
    label: '预设名称',
    field: 'name',
    component: 'Input',
    required: true,
  },
  {
    label: '目标类型',
    field: 'targetType',
    component: 'Select',
    required: true,
    componentProps: {
      options: [
        { label: '角色', value: 'character' },
        { label: '故事', value: 'story' },
        { label: '两者', value: 'both' },
      ],
    },
  },
  {
    label: '描述',
    field: 'description',
    component: 'InputTextArea',
  },
  {
    label: '',
    field: 'id',
    component: 'Input',
    show: false,
  },
];
