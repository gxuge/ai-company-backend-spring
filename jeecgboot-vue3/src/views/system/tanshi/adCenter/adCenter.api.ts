import { defHttp } from '/@/utils/http/axios';

export interface AdPage<T> {
  records: T[];
  total: number;
  current?: number;
  size?: number;
  pages?: number;
}

export interface AdSlot {
  id?: number;
  slotCode?: string;
  slotName?: string;
  slotType?: string;
  width?: number;
  height?: number;
  maxItems?: number;
  status?: string;
  description?: string;
  contentCount?: number;
  createdAt?: string;
  updatedAt?: string;
}

export interface AdContent {
  id?: number;
  slotId?: number;
  slotCode?: string;
  slotName?: string;
  contentCode?: string;
  title?: string;
  subtitle?: string;
  sourceType?: string;
  mediaType?: string;
  mediaUrl?: string;
  posterUrl?: string;
  cardType?: string;
  payloadJson?: string;
  imageUrl?: string;
  actionType?: string;
  actionPayload?: string;
  linkType?: string;
  linkValue?: string;
  status?: string;
  sortOrder?: number;
  startTime?: string;
  endTime?: string;
  extJson?: string;
  publishAt?: string;
  offlineAt?: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface AdDeliveryRule {
  id?: number;
  contentId: number;
  platforms: string[];
  audienceType: string;
  memberLevels: string[];
  userIds: string[];
}

export interface AdStats {
  impressions?: number;
  clicks?: number;
  clickThroughRate?: number;
}

export interface AdSlotQuery {
  keyword?: string;
  slotType?: string;
  status?: string;
  pageNo?: number;
  pageSize?: number;
}

export interface AdContentQuery {
  keyword?: string;
  slotId?: number;
  status?: string;
  pageNo?: number;
  pageSize?: number;
}

export interface AdSlotSave {
  id?: number;
  slotCode: string;
  slotName: string;
  slotType: string;
  width?: number;
  height?: number;
  maxItems: number;
  status: string;
  description?: string;
}

export interface AdContentSave {
  id?: number;
  slotId: number;
  contentCode?: string;
  title: string;
  subtitle?: string;
  sourceType: string;
  mediaType: string;
  mediaUrl?: string;
  mediaUploadUrl?: string;
  mediaFileUrl?: string;
  posterUrl?: string;
  cardType?: string;
  payloadJson?: string;
  imageUrl?: string;
  actionType: string;
  actionPayload?: string;
  linkType?: string;
  linkValue?: string;
  sortOrder: number;
  startTime?: string;
  endTime?: string;
  extJson?: string;
}

export interface AdStatsQuery {
  slotCode?: string;
  contentId?: number;
  startTime?: string;
  endTime?: string;
}

enum Api {
  slotPage = '/sys/ts-ad-admin/slot/page',
  slotCreate = '/sys/ts-ad-admin/slot/create',
  slotUpdate = '/sys/ts-ad-admin/slot/update',
  slotDelete = '/sys/ts-ad-admin/slot/delete',
  slotStatus = '/sys/ts-ad-admin/slot/status',
  contentPage = '/sys/ts-ad-admin/content/page',
  contentCreate = '/sys/ts-ad-admin/content/create',
  contentUpdate = '/sys/ts-ad-admin/content/update',
  contentDelete = '/sys/ts-ad-admin/content/delete',
  contentPublish = '/sys/ts-ad-admin/content/publish',
  contentOffline = '/sys/ts-ad-admin/content/offline',
  deliveryRule = '/sys/ts-ad-admin/delivery-rule',
  deliveryRuleSave = '/sys/ts-ad-admin/delivery-rule/save',
  statsSummary = '/sys/ts-ad-admin/stats/summary',
}

export const pageAdSlots = (data: AdSlotQuery) => defHttp.post<AdPage<AdSlot>>({ url: Api.slotPage, data });

export const createAdSlot = (data: AdSlotSave) => defHttp.post<number>({ url: Api.slotCreate, data });

export const updateAdSlot = (data: AdSlotSave & { id: number }) => defHttp.post<void>({ url: Api.slotUpdate, data });

export const deleteAdSlot = (data: { id: number }) => defHttp.post<void>({ url: Api.slotDelete, data });

export const updateAdSlotStatus = (data: { id: number; status: string }) => defHttp.post<void>({ url: Api.slotStatus, data });

export const pageAdContents = (data: AdContentQuery) => defHttp.post<AdPage<AdContent>>({ url: Api.contentPage, data });

export const createAdContent = (data: AdContentSave) => defHttp.post<number>({ url: Api.contentCreate, data });

export const updateAdContent = (data: AdContentSave & { id: number }) => defHttp.post<void>({ url: Api.contentUpdate, data });

export const deleteAdContent = (data: { id: number }) => defHttp.post<void>({ url: Api.contentDelete, data });

export const publishAdContent = (data: { id: number }) => defHttp.post<void>({ url: Api.contentPublish, data });

export const offlineAdContent = (data: { id: number }) => defHttp.post<void>({ url: Api.contentOffline, data });

export const getAdDeliveryRule = (params: { contentId: number }) => defHttp.get<AdDeliveryRule>({ url: Api.deliveryRule, params });

export const saveAdDeliveryRule = (data: AdDeliveryRule) => defHttp.post<void>({ url: Api.deliveryRuleSave, data });

export const getAdStats = (data: AdStatsQuery) => defHttp.post<AdStats>({ url: Api.statsSummary, data });
