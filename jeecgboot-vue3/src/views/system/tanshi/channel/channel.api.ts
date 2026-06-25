import { defHttp } from '/@/utils/http/axios';
import { Modal } from 'ant-design-vue';

const Api = {
  list: '/sys/ts-public-channels',
  detail: '/sys/ts-public-channels/detail',
  create: '/sys/ts-public-channels',
  update: '/sys/ts-public-channels',
  delete: '/sys/ts-public-channels',
} as const;

export const getPublicChannelList = (params) => defHttp.get({ url: Api.list, params });

export const getPublicChannelDetail = (params) => defHttp.get({ url: Api.detail, params });

export const saveOrUpdatePublicChannel = (params, isUpdate) => {
  return isUpdate ? defHttp.put({ url: Api.update, params }) : defHttp.post({ url: Api.create, params });
};

export const deletePublicChannel = (params, handleSuccess) => {
  Modal.confirm({
    title: '确认删除',
    content: '删除后不可恢复，是否继续？',
    okText: '确认',
    cancelText: '取消',
    onOk: () => {
      return defHttp.delete({ url: Api.delete, params }, { joinParamsToUrl: true }).then(() => {
        handleSuccess();
      });
    },
  });
};
