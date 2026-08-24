import { defHttp } from '/@/utils/http/axios';

export type WorkType = 'ROLE' | 'STORY';
export type ReviewItemType = 'TEXT' | 'IMAGE';

export interface WorkReviewItem {
  id: number;
  reviewId: number;
  itemType: ReviewItemType;
  fieldCode: string;
  contentText?: string;
  assetUrl?: string;
  contentHash?: string;
  createdAt?: string;
}

export interface WorkReviewLog {
  id: number;
  reviewId: number;
  actionType: string;
  beforeStatus?: string;
  afterStatus?: string;
  operatorType?: string;
  operatorId?: string;
  reason?: string;
  createdAt?: string;
}

export interface WorkReview {
  id: number;
  reviewNo: string;
  workType: WorkType;
  workId: number;
  workTitle?: string;
  ownerUserId: string;
  workVersion: number;
  requestedPublic: number;
  snapshotJson?: string;
  snapshotHash?: string;
  status: string;
  aiDecision?: string;
  aiRiskLevel?: string;
  aiReason?: string;
  aiResultJson?: string;
  aiReviewedAt?: string;
  adminReviewerId?: string;
  adminReason?: string;
  adminReviewedAt?: string;
  submittedAt?: string;
  items?: WorkReviewItem[];
  logs?: WorkReviewLog[];
}

export interface WorkReviewPage {
  records: WorkReview[];
  total: number;
  current?: number;
  size?: number;
  pages?: number;
}

export interface WorkReviewQuery {
  workType: WorkType;
  status?: string;
  ownerUserId?: string;
  pageNo?: number;
  pageSize?: number;
}

const Api = {
  list: '/sys/ts-admin-work-reviews',
  detail: '/sys/ts-admin-work-reviews/detail',
  approve: '/sys/ts-admin-work-reviews/approve',
  reject: '/sys/ts-admin-work-reviews/reject',
  retryAi: '/sys/ts-admin-work-reviews/retry-ai',
} as const;

export const getWorkReviewList = (params: WorkReviewQuery) => defHttp.get<WorkReviewPage>({ url: Api.list, params });

export const getWorkReviewDetail = (params: { id: number }) => defHttp.get<WorkReview>({ url: Api.detail, params });

export const approveWorkReview = (data: { id: number; reason?: string }) => defHttp.post<WorkReview>({ url: Api.approve, data });

export const rejectWorkReview = (data: { id: number; reason: string }) => defHttp.post<WorkReview>({ url: Api.reject, data });

export const retryWorkReviewAi = (data: { id: number }) => defHttp.post<WorkReview>({ url: Api.retryAi, data });
