import { defHttp } from '/@/utils/http/axios';

export type FeedbackAuditTargetType = 'feedback' | 'comment' | 'append';
export type FeedbackAuditStatus = 'pending' | 'approved' | 'rejected';
export type FeedbackProcessStatus = 'received' | 'processing' | 'completed';

export interface FeedbackAuditItem {
  targetType: FeedbackAuditTargetType;
  targetId: number;
  feedbackId: number;
  userId?: string;
  userName?: string;
  title?: string;
  content?: string;
  parentId?: number;
  official?: boolean;
  auditStatus: FeedbackAuditStatus;
  auditReason?: string;
  auditedBy?: string;
  auditorName?: string;
  auditedAt?: string;
  createdAt?: string;
}

export interface FeedbackAuditPage {
  records: FeedbackAuditItem[];
  total: number;
  current?: number;
  size?: number;
  pages?: number;
}

export interface FeedbackAuditQuery {
  targetType?: FeedbackAuditTargetType;
  auditStatus?: FeedbackAuditStatus | '';
  keyword?: string;
  pageNo?: number;
  pageSize?: number;
}

export interface FeedbackAuditUpdate {
  targetType: FeedbackAuditTargetType;
  targetId: number;
  auditStatus: Exclude<FeedbackAuditStatus, 'pending'>;
  auditReason?: string;
}

enum Api {
  audit = '/sys/ts-admin-feedback/audit',
  status = '/sys/ts-admin-feedback/status',
  reply = '/sys/ts-admin-feedback/reply',
}

export const pageFeedbackAudits = (params: FeedbackAuditQuery) => defHttp.get<FeedbackAuditPage>({ url: Api.audit, params });

export const updateFeedbackAudit = (data: FeedbackAuditUpdate) => defHttp.put<void>({ url: Api.audit, data });

export const updateFeedbackStatus = (data: { feedbackId: number; status: FeedbackProcessStatus }) => defHttp.put<void>({ url: Api.status, data });

export const createOfficialReply = (data: { feedbackId: number; content: string }) => defHttp.post<number>({ url: Api.reply, data });
