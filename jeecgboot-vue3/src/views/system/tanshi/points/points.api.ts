import { defHttp } from '/@/utils/http/axios';

export interface PointsPage<T> {
  records: T[];
  total: number;
  current?: number;
  size?: number;
  pages?: number;
}

export interface PointsAccount {
  userId?: string;
  balance?: number;
  totalIncome?: number;
  totalExpense?: number;
  nickname?: string;
  username?: string;
  email?: string;
  avatar?: string;
  updatedAt?: string;
}

export interface PointsTransaction {
  transactionId?: number;
  transactionNo?: string;
  title?: string;
  userId?: string;
  username?: string;
  realname?: string;
  bizType?: string;
  bizId?: string;
  direction?: string;
  amount?: number;
  beforeBalance?: number;
  afterBalance?: number;
  status?: string;
  description?: string;
  originalTransactionNo?: string;
  operatorId?: string;
  createdAt?: string;
}

export interface PointsRechargeOrder {
  id?: number;
  orderNo?: string;
  userId?: string;
  username?: string;
  productId?: number;
  productName?: string;
  points?: number;
  giftPoints?: number;
  originalAmount?: number;
  actualAmount?: number;
  currency?: string;
  paymentChannel?: string;
  status?: string;
  paymentIntentId?: string;
  transactionId?: string;
  clientSecret?: string;
  paymentUrl?: string;
  pointsTransactionNo?: string;
  payTime?: string;
  createdAt?: string;
}

export interface PointsProduct {
  id?: number;
  name?: string;
  points?: number;
  giftPoints?: number;
  originalAmount?: number;
  actualAmount?: number;
  currency?: string;
  status?: number;
  sort?: number;
  createdAt?: string;
  updatedAt?: string;
}

export interface MemberGiftRule {
  id?: number;
  planId?: number;
  productId?: number;
  giftPoints?: number;
  status?: number;
  createdAt?: string;
  updatedAt?: string;
}

export interface MemberPlanOption {
  id?: number;
  name?: string;
  code?: string;
  status?: number;
}

export interface MemberProductOption {
  id?: number;
  planId?: number;
  cycleType?: string;
  price?: number;
  status?: number;
}

export interface MemberConfig {
  plans?: MemberPlanOption[];
  products?: MemberProductOption[];
}

export interface PointsAccountQuery {
  keyword?: string;
  minBalance?: number;
  maxBalance?: number;
  pageNo?: number;
  pageSize?: number;
}

export interface PointsTransactionQuery {
  keyword?: string;
  direction?: string;
  bizType?: string;
  status?: string;
  startTime?: string;
  endTime?: string;
  pageNo?: number;
  pageSize?: number;
}

export interface PointsRechargeQuery {
  keyword?: string;
  paymentChannel?: string;
  status?: string;
  startTime?: string;
  endTime?: string;
  pageNo?: number;
  pageSize?: number;
}

export interface PointsAdjustRequest {
  userId: string;
  operation: 'ADD' | 'DEDUCT';
  amount: number;
  reason: string;
  idempotencyKey: string;
}

export interface PointsProductSaveRequest {
  id?: number;
  name: string;
  points: number;
  giftPoints: number;
  originalAmount: number;
  actualAmount: number;
  currency: string;
  status: number;
  sort?: number;
}

export interface MemberGiftRuleSaveRequest {
  id?: number;
  planId: number;
  productId?: number;
  giftPoints: number;
  status: number;
}

enum Api {
  accountPage = '/sys/ts-points-admin/account/page',
  transactionPage = '/sys/ts-points-admin/transaction/page',
  adjust = '/sys/ts-points-admin/adjust',
  rechargePage = '/sys/ts-points-admin/recharge/page',
  productList = '/sys/ts-points-admin/product/list',
  productSave = '/sys/ts-points-admin/product/save',
  giftRuleList = '/sys/ts-points-admin/member-gift-rule/list',
  giftRuleSave = '/sys/ts-points-admin/member-gift-rule/save',
  memberConfig = '/sys/ts-member-admin/config',
}

export const pagePointsAccounts = (data: PointsAccountQuery) => defHttp.post<PointsPage<PointsAccount>>({ url: Api.accountPage, data });

export const pagePointsTransactions = (data: PointsTransactionQuery) =>
  defHttp.post<PointsPage<PointsTransaction>>({ url: Api.transactionPage, data });

export const adjustPoints = (data: PointsAdjustRequest) => defHttp.post<PointsTransaction>({ url: Api.adjust, data });

export const pagePointsRechargeOrders = (data: PointsRechargeQuery) => defHttp.post<PointsPage<PointsRechargeOrder>>({ url: Api.rechargePage, data });

export const listPointsProducts = () => defHttp.get<PointsProduct[]>({ url: Api.productList });

export const savePointsProduct = (data: PointsProductSaveRequest) => defHttp.post<void>({ url: Api.productSave, data });

export const listMemberGiftRules = () => defHttp.get<MemberGiftRule[]>({ url: Api.giftRuleList });

export const saveMemberGiftRule = (data: MemberGiftRuleSaveRequest) => defHttp.post<void>({ url: Api.giftRuleSave, data });

export const getMemberConfig = () => defHttp.get<MemberConfig>({ url: Api.memberConfig });
