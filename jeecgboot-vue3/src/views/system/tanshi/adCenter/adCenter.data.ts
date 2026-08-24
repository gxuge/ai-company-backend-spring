import type { BasicColumn } from '/@/components/Table';
import type { FormSchema } from '/@/components/Form';

export const slotTypeOptions = [
  { label: '横幅', value: 'BANNER' },
  { label: '海报', value: 'POSTER' },
  { label: '弹窗', value: 'POPUP' },
  { label: '轮播图', value: 'CAROUSEL' },
];

export const slotStatusOptions = [
  { label: '启用', value: 'ENABLED' },
  { label: '停用', value: 'DISABLED' },
];

export const contentStatusOptions = [
  { label: '草稿', value: 'DRAFT' },
  { label: '已发布', value: 'PUBLISHED' },
  { label: '已下线', value: 'OFFLINE' },
];

export const sourceTypeOptions = [
  { label: '自有素材', value: 'SELF' },
  { label: '外部素材', value: 'EXTERNAL' },
];

export const mediaTypeOptions = [
  { label: '图片', value: 'IMAGE' },
  { label: '视频', value: 'VIDEO' },
  { label: '卡片', value: 'CARD' },
];

export const cardTypeOptions = [
  { label: '推广卡片', value: 'PROMOTION' },
  { label: '角色卡片', value: 'ROLE' },
  { label: '故事卡片', value: 'STORY' },
  { label: '自定义卡片', value: 'CUSTOM' },
];

export const linkTypeOptions = [
  { label: '无跳转', value: 'NONE' },
  { label: '外部链接', value: 'URL' },
  { label: '前端路由', value: 'ROUTE' },
  { label: '角色详情', value: 'ROLE' },
  { label: '故事详情', value: 'STORY' },
  { label: '深层链接', value: 'DEEP_LINK' },
];

export const platformOptions = [
  { label: '全部平台', value: 'ALL' },
  { label: 'Web', value: 'WEB' },
  { label: 'iOS', value: 'IOS' },
  { label: 'Android', value: 'ANDROID' },
];

export const audienceTypeOptions = [
  { label: '全部用户', value: 'ALL' },
  { label: '仅登录用户', value: 'LOGIN' },
  { label: '仅匿名用户', value: 'ANONYMOUS' },
  { label: '指定用户', value: 'USER_LIST' },
];

export const memberLevelOptions = [
  { label: '全部等级', value: 'ALL' },
  { label: '免费用户', value: 'FREE' },
  { label: 'PRO', value: 'PRO' },
  { label: 'ULTRA', value: 'ULTRA' },
];

export const slotColumns: BasicColumn[] = [
  { title: '广告位名称', dataIndex: 'slotName', width: 180 },
  { title: '编码', dataIndex: 'slotCode', width: 180, ellipsis: true },
  { title: '类型', dataIndex: 'slotType', width: 100, slots: { customRender: 'slotType' } },
  { title: '建议尺寸', dataIndex: 'dimensions', width: 120, slots: { customRender: 'dimensions' } },
  { title: '最大数量', dataIndex: 'maxItems', width: 90 },
  { title: '内容数', dataIndex: 'contentCount', width: 80 },
  { title: '状态', dataIndex: 'status', width: 90, slots: { customRender: 'status' } },
  { title: '更新时间', dataIndex: 'updatedAt', width: 170 },
];

export const slotSearchFormSchema: FormSchema[] = [
  {
    field: 'keyword',
    label: '关键词',
    component: 'Input',
    colProps: { span: 8 },
    componentProps: { allowClear: true, placeholder: '广告位名称或编码' },
  },
  {
    field: 'slotType',
    label: '广告位类型',
    component: 'Select',
    colProps: { span: 8 },
    componentProps: { options: slotTypeOptions, allowClear: true },
  },
  {
    field: 'status',
    label: '状态',
    component: 'Select',
    colProps: { span: 8 },
    componentProps: { options: slotStatusOptions, allowClear: true },
  },
];

export const contentColumns: BasicColumn[] = [
  { title: '素材', dataIndex: 'mediaUrl', width: 110, slots: { customRender: 'mediaUrl' } },
  { title: '标题', dataIndex: 'title', width: 180, ellipsis: true },
  { title: '内容编码', dataIndex: 'contentCode', width: 180, ellipsis: true },
  { title: '广告位', dataIndex: 'slotName', width: 160 },
  { title: '来源', dataIndex: 'sourceType', width: 100, slots: { customRender: 'sourceType' } },
  { title: '媒体', dataIndex: 'mediaType', width: 90, slots: { customRender: 'mediaType' } },
  { title: '动作', dataIndex: 'actionType', width: 110, slots: { customRender: 'actionType' } },
  { title: '投放时间', dataIndex: 'deliveryTime', width: 300, slots: { customRender: 'deliveryTime' } },
  { title: '状态', dataIndex: 'status', width: 90, slots: { customRender: 'status' } },
  { title: '排序', dataIndex: 'sortOrder', width: 70 },
  { title: '更新时间', dataIndex: 'updatedAt', width: 170 },
];

export function getContentSearchFormSchema(slotOptions: { label: string; value: number }[]): FormSchema[] {
  return [
    {
      field: 'keyword',
      label: '关键词',
      component: 'Input',
      colProps: { span: 8 },
      componentProps: { allowClear: true, placeholder: '标题或内容编码' },
    },
    {
      field: 'slotId',
      label: '广告位',
      component: 'Select',
      colProps: { span: 8 },
      componentProps: { options: slotOptions, allowClear: true, showSearch: true, optionFilterProp: 'label' },
    },
    {
      field: 'status',
      label: '状态',
      component: 'Select',
      colProps: { span: 8 },
      componentProps: { options: contentStatusOptions, allowClear: true },
    },
  ];
}

export const slotFormSchema: FormSchema[] = [
  { field: 'id', label: 'ID', component: 'Input', show: false },
  {
    field: 'slotName',
    label: '广告位名称',
    component: 'Input',
    required: true,
    componentProps: { placeholder: '例如：首页顶部轮播' },
  },
  {
    field: 'slotCode',
    label: '广告位编码',
    component: 'Input',
    required: true,
    componentProps: { placeholder: '例如：HOME_BANNER' },
  },
  {
    field: 'slotType',
    label: '广告位类型',
    component: 'Select',
    required: true,
    componentProps: { options: slotTypeOptions },
  },
  {
    field: 'width',
    label: '建议宽度',
    component: 'InputNumber',
    componentProps: { min: 1, precision: 0, style: { width: '100%' } },
  },
  {
    field: 'height',
    label: '建议高度',
    component: 'InputNumber',
    componentProps: { min: 1, precision: 0, style: { width: '100%' } },
  },
  {
    field: 'maxItems',
    label: '最大展示数',
    component: 'InputNumber',
    required: true,
    componentProps: { min: 1, max: 50, precision: 0, style: { width: '100%' } },
  },
  {
    field: 'status',
    label: '状态',
    component: 'Select',
    required: true,
    componentProps: { options: slotStatusOptions },
  },
  {
    field: 'description',
    label: '说明',
    component: 'InputTextArea',
    componentProps: { rows: 3, maxlength: 500, showCount: true },
  },
];

export const contentFormSchema: FormSchema[] = [
  { field: 'id', label: 'ID', component: 'Input', show: false },
  {
    field: 'slotId',
    label: '所属广告位',
    component: 'Select',
    required: true,
    componentProps: { options: [], showSearch: true, optionFilterProp: 'label' },
  },
  {
    field: 'contentCode',
    label: '内容编码',
    component: 'Input',
    componentProps: { placeholder: '留空由系统生成' },
  },
  {
    field: 'title',
    label: '标题',
    component: 'Input',
    required: true,
    componentProps: { maxlength: 200, showCount: true },
  },
  {
    field: 'subtitle',
    label: '副标题',
    component: 'InputTextArea',
    componentProps: { rows: 2, maxlength: 500, showCount: true },
  },
  {
    field: 'sourceType',
    label: '素材来源',
    component: 'Select',
    required: true,
    componentProps: { options: sourceTypeOptions },
  },
  {
    field: 'mediaType',
    label: '媒体类型',
    component: 'Select',
    required: true,
    componentProps: { options: mediaTypeOptions },
  },
  {
    field: 'mediaUploadUrl',
    label: '图片素材',
    component: 'JImageUpload',
    dynamicShow: ({ values }) => values.sourceType === 'SELF' && values.mediaType === 'IMAGE',
    dynamicRules: ({ values }) =>
      values.sourceType === 'SELF' && values.mediaType === 'IMAGE' ? [{ required: true, message: '请上传图片素材' }] : [],
    componentProps: { fileMax: 1 },
  },
  {
    field: 'mediaFileUrl',
    label: '视频素材',
    component: 'JUpload',
    dynamicShow: ({ values }) => values.sourceType === 'SELF' && values.mediaType === 'VIDEO',
    dynamicRules: ({ values }) =>
      values.sourceType === 'SELF' && values.mediaType === 'VIDEO' ? [{ required: true, message: '请上传视频素材' }] : [],
    componentProps: { maxCount: 1, accept: 'video/*', text: '上传视频' },
  },
  {
    field: 'mediaUrl',
    label: '素材地址',
    component: 'Input',
    dynamicShow: ({ values }) => values.mediaType !== 'CARD' && !(values.sourceType === 'SELF' && values.mediaType === 'IMAGE'),
    dynamicRules: ({ values }) =>
      values.mediaType !== 'CARD' && !(values.sourceType === 'SELF' && values.mediaType === 'IMAGE')
        ? [{ required: true, message: '请输入素材地址' }]
        : [],
    componentProps: { placeholder: '外部素材请输入 HTTP/HTTPS 地址；视频请输入视频地址' },
  },
  {
    field: 'posterUrl',
    label: '视频封面',
    component: 'JImageUpload',
    dynamicShow: ({ values }) => values.mediaType === 'VIDEO',
    componentProps: { fileMax: 1 },
  },
  {
    field: 'cardType',
    label: '卡片类型',
    component: 'Select',
    dynamicShow: ({ values }) => values.mediaType === 'CARD',
    dynamicRules: ({ values }) => (values.mediaType === 'CARD' ? [{ required: true, message: '请选择卡片类型' }] : []),
    componentProps: { options: cardTypeOptions },
  },
  {
    field: 'payloadJson',
    label: '卡片内容',
    component: 'InputTextArea',
    dynamicShow: ({ values }) => values.mediaType === 'CARD',
    dynamicRules: ({ values }) => (values.mediaType === 'CARD' ? [{ required: true, message: '请输入卡片内容JSON' }] : []),
    componentProps: {
      rows: 8,
      placeholder: '{ "badge": "推荐", "title": "标题", "description": "描述", "buttonText": "立即查看" }',
    },
  },
  {
    field: 'actionType',
    label: '点击动作',
    component: 'Select',
    required: true,
    componentProps: { options: linkTypeOptions },
  },
  {
    field: 'actionPayload',
    label: '动作目标',
    component: 'Input',
    dynamicRules: ({ values }) => (values.actionType && values.actionType !== 'NONE' ? [{ required: true, message: '请输入动作目标' }] : []),
    dynamicShow: ({ values }) => values.actionType !== 'NONE',
    componentProps: { placeholder: 'URL、前端路由、深层链接或角色/故事ID' },
  },
  {
    field: 'startTime',
    label: '开始时间',
    component: 'DatePicker',
    componentProps: { showTime: true, valueFormat: 'YYYY-MM-DD HH:mm:ss', style: { width: '100%' } },
  },
  {
    field: 'endTime',
    label: '结束时间',
    component: 'DatePicker',
    componentProps: { showTime: true, valueFormat: 'YYYY-MM-DD HH:mm:ss', style: { width: '100%' } },
  },
  {
    field: 'sortOrder',
    label: '排序',
    component: 'InputNumber',
    componentProps: { precision: 0, style: { width: '100%' } },
  },
  {
    field: 'extJson',
    label: '扩展配置',
    component: 'InputTextArea',
    componentProps: { rows: 4, placeholder: '可选，输入 JSON 对象或数组' },
  },
];

export const ruleFormSchema: FormSchema[] = [
  { field: 'contentId', label: '内容ID', component: 'Input', show: false },
  {
    field: 'platforms',
    label: '投放平台',
    component: 'CheckboxGroup',
    required: true,
    componentProps: { options: platformOptions },
  },
  {
    field: 'audienceType',
    label: '目标用户',
    component: 'Select',
    required: true,
    componentProps: { options: audienceTypeOptions },
  },
  {
    field: 'memberLevels',
    label: '会员等级',
    component: 'CheckboxGroup',
    required: true,
    componentProps: { options: memberLevelOptions },
  },
  {
    field: 'userIdsText',
    label: '指定用户ID',
    component: 'InputTextArea',
    dynamicShow: ({ values }) => values.audienceType === 'USER_LIST',
    dynamicRules: ({ values }) => (values.audienceType === 'USER_LIST' ? [{ required: true, message: '请输入至少一个用户ID' }] : []),
    componentProps: { rows: 8, placeholder: '每行一个用户ID，也支持逗号分隔' },
  },
];

export function getOptionLabel(options: { label: string; value: string }[], value?: string) {
  return options.find((item) => item.value === value)?.label || value || '-';
}
