import { defHttp } from '/@/utils/http/axios';

export interface RewardEventPage<T> {
  records: T[];
  total: number;
  current?: number;
  size?: number;
  pages?: number;
}

export interface RewardEventItem {
  id?: number;
  eventId?: string;
  eventType?: string;
  userId?: string;
  username?: string;
  realname?: string;
  bizId?: string;
  status?: string;
  retryCount?: number;
  maxRetryCount?: number;
  rewardStatus?: string;
  rewardValue?: number;
  pointsTransactionNo?: string;
  lastErrorCode?: string;
  lastErrorMessage?: string;
  processedAt?: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface RewardEventDetail extends RewardEventItem {
  payloadJson?: string;
  resultJson?: string;
}

export interface RewardEventSummary {
  pendingCount?: number;
  processingCount?: number;
  successCount?: number;
  failedCount?: number;
  todayCount?: number;
}

export interface RewardEventQuery {
  keyword?: string;
  eventType?: string;
  status?: string;
  startTime?: string;
  endTime?: string;
  pageNo?: number;
  pageSize?: number;
}

enum Api {
  eventPage = '/sys/ts-reward-admin/event/page',
  eventSummary = '/sys/ts-reward-admin/event/summary',
  eventDetail = '/sys/ts-reward-admin/event/detail',
  eventRetry = '/sys/ts-reward-admin/event/retry',
}

export const pageRewardEvents = (data: RewardEventQuery) => defHttp.post<RewardEventPage<RewardEventItem>>({ url: Api.eventPage, data });

export const summarizeRewardEvents = (data: RewardEventQuery) => defHttp.post<RewardEventSummary>({ url: Api.eventSummary, data });

export const getRewardEventDetail = (params: { id: number }) => defHttp.get<RewardEventDetail>({ url: Api.eventDetail, params });

export const retryRewardEvent = (data: { eventId: string }) => defHttp.post({ url: Api.eventRetry, data });
