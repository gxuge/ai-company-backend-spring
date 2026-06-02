import { defHttp } from '/@/utils/http/axios';

enum Api {
  list = '/sys/tsAiLog/list',
  queryById = '/sys/tsAiLog/queryById',
  detail = '/sys/tsAiLog/detail',
}

export const getAiLogList = (params) => {
  return defHttp.get({ url: Api.list, params });
};

export const getAiLogById = (params) => {
  return defHttp.get({ url: Api.queryById, params });
};

export const getAiLogDetail = (params) => {
  return defHttp.get({ url: Api.detail, params });
};
