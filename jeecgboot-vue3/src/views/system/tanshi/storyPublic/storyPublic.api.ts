import { defHttp } from '/@/utils/http/axios';
import { Modal } from 'ant-design-vue';
import { getPublicChannelOptions, getStoryPublicTargetOptions } from '../publicManage.api';

const Api = {
  list: '/sys/ts-story-publics',
  detail: '/sys/ts-story-publics/detail',
  create: '/sys/ts-story-publics',
  update: '/sys/ts-story-publics',
  delete: '/sys/ts-story-publics',
  submit: '/sys/ts-story-publics/submit',
  approve: '/sys/ts-story-publics/approve',
  reject: '/sys/ts-story-publics/reject',
  online: '/sys/ts-story-publics/online',
  offline: '/sys/ts-story-publics/offline',
} as const;

export const getStoryPublicList = (params) => defHttp.get({ url: Api.list, params });

export const getStoryPublicDetail = (params) => defHttp.get({ url: Api.detail, params });

export const saveOrUpdateStoryPublic = (params, isUpdate) => {
  return isUpdate ? defHttp.put({ url: Api.update, params }) : defHttp.post({ url: Api.create, params });
};

export const deleteStoryPublic = (params, handleSuccess) => {
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

export const submitStoryPublic = (params) => defHttp.post({ url: Api.submit, params });
export const approveStoryPublic = (params) => defHttp.post({ url: Api.approve, params });
export const rejectStoryPublic = (params) => defHttp.post({ url: Api.reject, params });
export const onlineStoryPublic = (params) => defHttp.post({ url: Api.online, params });
export const offlineStoryPublic = (params) => defHttp.post({ url: Api.offline, params });

export const getStoryChannelOptions = async () => {
  const options = await getPublicChannelOptions({ targetType: 'story' });
  return (options || []).map((item) => ({
    label: item.label,
    value: item.value,
  }));
};

export const getStoryTargetOptions = (params) => getStoryPublicTargetOptions(params);
