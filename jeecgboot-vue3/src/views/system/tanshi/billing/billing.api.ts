import { defHttp } from '/@/utils/http/axios';

export interface BillingPage<T> {
  records: T[];
  total: number;
  current?: number;
  size?: number;
  pages?: number;
}

export interface BillingQuery {
  keyword?: string;
  category?: string;
  moneyDirection?: string;
  pointsDirection?: string;
  bizType?: string;
  status?: string;
  startTime?: string;
  endTime?: string;
  pageNo?: number;
  pageSize?: number;
}

export interface BillingRecord {
  recordId?: number;
  recordType?: string;
  userId?: string;
  nickname?: string;
  title?: string;
  orderNo?: string;
  bizType?: string;
  moneyAmount?: number;
  pointsAmount?: number;
  moneyDirection?: string;
  pointsDirection?: string;
  status?: string;
  createdAt?: string;
}

export interface BillingDetail extends BillingRecord {
  paymentChannel?: string;
  originalAmount?: number;
  discountAmount?: number;
  actualAmount?: number;
  beforeBalance?: number;
  afterBalance?: number;
  relatedBizId?: string;
  description?: string;
  payTime?: string;
}

export interface BillingSummary {
  moneyIncome?: number;
  moneyExpense?: number;
  pointsIncome?: number;
  pointsExpense?: number;
  recordCount?: number;
}

enum Api {
  page = '/sys/ts-billing-admin/page',
  detail = '/sys/ts-billing-admin/detail',
  summary = '/sys/ts-billing-admin/summary',
}

export const pagePlatformBills = (data: BillingQuery) => defHttp.post<BillingPage<BillingRecord>>({ url: Api.page, data });

export const getPlatformBill = (data: { recordType: string; recordId: number }) => defHttp.post<BillingDetail>({ url: Api.detail, data });

export const summarizePlatformBills = (data: BillingQuery) => defHttp.post<BillingSummary>({ url: Api.summary, data });
