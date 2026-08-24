import type { BasicColumn } from '/@/components/Table';
import type { FormSchema } from '/@/components/Form';

export const reviewStatusOptions = [
  { label: 'AI 审核中', value: 'PENDING_AI' },
  { label: '待管理员审核', value: 'PENDING_ADMIN' },
  { label: '审核通过', value: 'APPROVED' },
  { label: '已驳回', value: 'REJECTED' },
  { label: '版本已失效', value: 'OBSOLETE' },
];

export const reviewStatusColorMap: Record<string, string> = {
  PENDING_AI: 'processing',
  PENDING_ADMIN: 'blue',
  APPROVED: 'success',
  REJECTED: 'error',
  OBSOLETE: 'default',
};

export const aiDecisionOptions = [
  { label: '通过', value: 'PASS' },
  { label: '人工复核', value: 'MANUAL' },
  { label: '拦截', value: 'BLOCK' },
];

export const aiDecisionColorMap: Record<string, string> = {
  PASS: 'success',
  MANUAL: 'warning',
  BLOCK: 'error',
};

export const aiRiskOptions = [
  { label: '低风险', value: 'LOW' },
  { label: '中风险', value: 'MEDIUM' },
  { label: '高风险', value: 'HIGH' },
];

export const aiRiskColorMap: Record<string, string> = {
  LOW: 'success',
  MEDIUM: 'warning',
  HIGH: 'error',
};

export const columns: BasicColumn[] = [
  { title: '审核单号', dataIndex: 'reviewNo', width: 220, ellipsis: true },
  { title: '作品名称', dataIndex: 'workTitle', width: 180, ellipsis: true },
  { title: '作品ID', dataIndex: 'workId', width: 100 },
  { title: '版本', dataIndex: 'workVersion', width: 80 },
  { title: '所属用户ID', dataIndex: 'ownerUserId', width: 210, ellipsis: true },
  { title: '审核状态', dataIndex: 'status', width: 130, slots: { customRender: 'status' } },
  { title: 'AI 结论', dataIndex: 'aiDecision', width: 110, slots: { customRender: 'aiDecision' } },
  { title: '风险等级', dataIndex: 'aiRiskLevel', width: 100, slots: { customRender: 'aiRiskLevel' } },
  { title: '申请公开', dataIndex: 'requestedPublic', width: 90, slots: { customRender: 'requestedPublic' } },
  { title: '提交时间', dataIndex: 'submittedAt', width: 180 },
  { title: '终审时间', dataIndex: 'adminReviewedAt', width: 180 },
];

export const searchFormSchema: FormSchema[] = [
  {
    field: 'ownerUserId',
    label: '所属用户ID',
    component: 'Input',
    colProps: { span: 8 },
    componentProps: { allowClear: true, placeholder: '请输入所属用户ID' },
  },
  {
    field: 'status',
    label: '审核状态',
    component: 'Select',
    colProps: { span: 8 },
    componentProps: {
      options: reviewStatusOptions,
      allowClear: true,
      placeholder: '请选择审核状态',
    },
  },
];

export const fieldLabelMap: Record<string, string> = {
  roleName: '角色名称',
  roleSubtitle: '角色副标题',
  occupation: '职业',
  greeting: '开场白',
  backgroundStory: '角色背景故事',
  dialoguePreview: '对话预览',
  extJson: '扩展内容',
  avatarUrl: '角色头像',
  coverUrl: '封面图片',
  title: '故事标题',
  storyIntro: '故事简介',
  siteSetting: '场景设定',
  storyBackground: '故事背景',
  sceneNameSnapshot: '场景名称',
  plotOutline: '剧情大纲',
  sceneImageUrl: '场景图片',
};

export const reviewActionLabelMap: Record<string, string> = {
  SUBMIT: '提交审核',
  AI_PASS: 'AI 初审完成',
  AI_BLOCK: 'AI 拦截',
  AI_ERROR: 'AI 审核异常',
  ADMIN_APPROVE: '管理员通过',
  ADMIN_REJECT: '管理员驳回',
  OBSOLETE: '旧版本失效',
};

export function getOptionLabel(options: { label: string; value: string }[], value?: string) {
  return options.find((item) => item.value === value)?.label || value || '-';
}
