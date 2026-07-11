import type { FormSchema } from '/@/components/Form';
import type { BasicColumn } from '/@/components/Table';

export const knowledgeBaseSearchSchema: FormSchema[] = [
  {
    label: '知识库名称',
    field: 'name',
    component: 'Input',
    componentProps: {
      placeholder: '请输入知识库名称',
      allowClear: true,
    },
  },
  {
    label: '状态',
    field: 'status',
    component: 'Select',
    componentProps: {
      options: [
        { label: '启用', value: 1 },
        { label: '禁用', value: 0 },
      ],
      placeholder: '请选择状态',
      allowClear: true,
    },
  },
];

export const knowledgeBaseColumns: BasicColumn[] = [
  {
    title: '知识库名称',
    dataIndex: 'name',
    width: 180,
    ellipsis: true,
  },
  {
    title: '描述',
    dataIndex: 'description',
    width: 260,
    ellipsis: true,
  },
  {
    title: '业务类型',
    dataIndex: 'biz_type',
    width: 120,
  },
  {
    title: '状态',
    dataIndex: 'status',
    width: 100,
    customRender: ({ text }) => (Number(text) === 1 ? '启用' : '禁用'),
  },
];

export const knowledgeBaseFormSchema: FormSchema[] = [
  {
    label: 'id',
    field: 'id',
    component: 'Input',
    show: false,
  },
  {
    label: '知识库名称',
    field: 'name',
    required: true,
    component: 'Input',
    componentProps: {
      placeholder: '请输入知识库名称',
      maxlength: 100,
      showCount: true,
    },
  },
  {
    label: '描述',
    field: 'description',
    component: 'InputTextArea',
    componentProps: {
      placeholder: '请输入知识库描述',
      rows: 4,
      maxlength: 500,
      showCount: true,
    },
  },
  {
    label: '业务类型',
    field: 'biz_type',
    component: 'Input',
    componentProps: {
      placeholder: '例如：agent / story / doc / faq',
      allowClear: true,
    },
  },
  {
    label: '状态',
    field: 'status',
    required: true,
    component: 'RadioButtonGroup',
    defaultValue: 1,
    componentProps: {
      options: [
        { label: '启用', value: 1 },
        { label: '禁用', value: 0 },
      ],
    },
  },
];

