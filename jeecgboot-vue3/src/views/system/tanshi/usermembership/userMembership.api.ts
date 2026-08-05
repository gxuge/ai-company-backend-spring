import { defHttp } from '/@/utils/http/axios';

enum Api {
  page = '/sys/ts-member-admin/membership/page',
  save = '/sys/ts-member-admin/membership/save',
  deleteOne = '/sys/ts-member-admin/membership/delete',
  detail = '/sys/ts-member-admin/membership/detail',
  quotaSave = '/sys/ts-member-admin/quota/save',
  quotaDelete = '/sys/ts-member-admin/quota/delete',
  config = '/sys/ts-member-admin/config',
}

export const pageMemberships = (data) => defHttp.post({ url: Api.page, data });
export const saveMembership = (data) => defHttp.post({ url: Api.save, data });
export const deleteMembership = (data) => defHttp.post({ url: Api.deleteOne, data });
export const getMembershipDetail = (data) => defHttp.post({ url: Api.detail, data });
export const saveQuota = (data) => defHttp.post({ url: Api.quotaSave, data });
export const deleteQuota = (data) => defHttp.post({ url: Api.quotaDelete, data });
export const getMemberConfig = () => defHttp.get({ url: Api.config });
