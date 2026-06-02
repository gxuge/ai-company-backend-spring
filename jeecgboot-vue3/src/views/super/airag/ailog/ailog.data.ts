import { BasicColumn, FormSchema } from '/@/components/Table';

export const columns: BasicColumn[] = [
  {
    title: '接口路径',
    dataIndex: 'endpoint',
    width: 240,
  },
  {
    title: '场景',
    dataIndex: 'bizScene',
    width: 140,
  },
  {
    title: '供应商',
    dataIndex: 'provider',
    width: 120,
  },
  {
    title: '模型',
    dataIndex: 'modelName',
    width: 180,
  },
  {
    title: '模板',
    dataIndex: 'promptCode',
    width: 180,
    customRender: ({ record }) => {
      const code = record?.promptCode || '-';
      const version = record?.promptVersion || '-';
      return `${code}@${version}`;
    },
  },
  {
    title: '修复',
    dataIndex: 'hasRepair',
    width: 90,
    slots: { customRender: 'hasRepair' },
  },
  {
    title: '状态',
    dataIndex: 'status',
    width: 100,
    slots: { customRender: 'status' },
  },
  {
    title: '耗时(ms)',
    dataIndex: 'costMs',
    width: 100,
  },
  {
    title: '创建时间',
    dataIndex: 'createTime',
    width: 180,
  },
];

export const searchFormSchema: FormSchema[] = [
  {
    field: 'endpoint',
    label: '接口路径',
    component: 'Input',
    colProps: { span: 8 },
  },
  {
    field: 'bizScene',
    label: '场景',
    component: 'Input',
    colProps: { span: 8 },
  },
  {
    field: 'provider',
    label: '供应商',
    component: 'Input',
    colProps: { span: 8 },
  },
  {
    field: 'status',
    label: '状态',
    component: 'Select',
    componentProps: {
      options: [
        { label: '成功', value: 'success' },
        { label: '失败', value: 'failed' },
        { label: '执行中', value: 'running' },
      ],
    },
    colProps: { span: 8 },
  },
  {
    field: 'traceId',
    label: 'Trace ID',
    component: 'Input',
    colProps: { span: 8 },
  },
  {
    field: 'promptCode',
    label: '模板编码',
    component: 'Input',
    colProps: { span: 8 },
  },
];
