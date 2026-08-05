import { defHttp } from '/@/utils/http/axios';

enum Api {
  config = '/sys/ts-member-admin/config',
  save = '/sys/ts-member-admin/config/save',
  deleteOne = '/sys/ts-member-admin/config/delete',
}

export const getMemberConfig = () => defHttp.get({ url: Api.config });
export const saveMemberConfig = (data) => defHttp.post({ url: Api.save, data });
export const deleteMemberConfig = (data) => defHttp.post({ url: Api.deleteOne, data });
