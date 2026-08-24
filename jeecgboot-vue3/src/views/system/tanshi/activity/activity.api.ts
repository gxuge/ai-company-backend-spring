import { defHttp } from '/@/utils/http/axios';

export interface ActivityPage<T> {
  records: T[];
  total: number;
  current?: number;
  size?: number;
  pages?: number;
}

export interface ActivityTask {
  id?: number;
  taskName?: string;
  taskType?: string;
  category?: string;
  description?: string;
  conditionType?: string;
  conditionValue?: number;
  rewardType?: string;
  rewardValue?: number;
  startTime?: string;
  endTime?: string;
  status?: string;
  sort?: number;
  createdAt?: string;
  updatedAt?: string;
}

export interface UserTaskProgress {
  id?: number;
  userId?: string;
  username?: string;
  realname?: string;
  taskId?: number;
  taskName?: string;
  cycleKey?: string;
  currentValue?: number;
  targetValue?: number;
  status?: string;
  rewardStatus?: string;
  completeTime?: string;
  rewardTime?: string;
  updatedAt?: string;
}

export interface ActivityRewardRecord {
  id?: number;
  userId?: string;
  username?: string;
  realname?: string;
  taskId?: number;
  taskName?: string;
  rewardType?: string;
  baseRewardValue?: number;
  extraRewardValue?: number;
  rewardValue?: number;
  sourceType?: string;
  sourceId?: string;
  memberLevel?: string;
  pointsTransactionNo?: string;
  createdAt?: string;
}

export interface ActivityRewardRule {
  id?: number;
  taskId?: number;
  memberLevel?: string;
  extraRewardType?: string;
  extraRewardValue?: number;
  status?: number;
  createdAt?: string;
  updatedAt?: string;
}

export interface ActivityTaskQuery {
  keyword?: string;
  taskType?: string;
  category?: string;
  status?: string;
  pageNo?: number;
  pageSize?: number;
}

export interface UserTaskQuery {
  userKeyword?: string;
  taskId?: number;
  status?: string;
  rewardStatus?: string;
  pageNo?: number;
  pageSize?: number;
}

export interface ActivityRewardQuery {
  userKeyword?: string;
  rewardType?: string;
  startTime?: string;
  endTime?: string;
  pageNo?: number;
  pageSize?: number;
}

export interface ActivityTaskSave {
  id?: number;
  taskName: string;
  taskType: string;
  category: string;
  description?: string;
  conditionType: string;
  conditionValue: number;
  rewardType: string;
  rewardValue: number;
  startTime?: string;
  endTime?: string;
  status: string;
  sort: number;
}

export interface ActivityRewardRuleSave {
  id?: number;
  taskId: number;
  memberLevel: string;
  extraRewardType: string;
  extraRewardValue: number;
  status: number;
}

enum Api {
  taskPage = '/sys/ts-activity-admin/task/page',
  taskCreate = '/sys/ts-activity-admin/task/create',
  taskUpdate = '/sys/ts-activity-admin/task/update',
  userTaskPage = '/sys/ts-activity-admin/user-task/page',
  rewardPage = '/sys/ts-activity-admin/reward/page',
  rewardRuleList = '/sys/ts-activity-admin/reward-rule/list',
  rewardRuleSave = '/sys/ts-activity-admin/reward-rule/save',
}

export const pageActivityTasks = (data: ActivityTaskQuery) => defHttp.post<ActivityPage<ActivityTask>>({ url: Api.taskPage, data });

export const createActivityTask = (data: ActivityTaskSave) => defHttp.post<number>({ url: Api.taskCreate, data });

export const updateActivityTask = (data: ActivityTaskSave & { id: number }) => defHttp.post<void>({ url: Api.taskUpdate, data });

export const pageUserTaskProgress = (data: UserTaskQuery) => defHttp.post<ActivityPage<UserTaskProgress>>({ url: Api.userTaskPage, data });

export const pageActivityRewards = (data: ActivityRewardQuery) => defHttp.post<ActivityPage<ActivityRewardRecord>>({ url: Api.rewardPage, data });

export const listActivityRewardRules = () => defHttp.get<ActivityRewardRule[]>({ url: Api.rewardRuleList });

export const saveActivityRewardRule = (data: ActivityRewardRuleSave) => defHttp.post<void>({ url: Api.rewardRuleSave, data });
