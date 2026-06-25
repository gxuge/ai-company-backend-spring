import { defHttp } from '/@/utils/http/axios';
import { Modal } from 'ant-design-vue';
import { getPublicChannelOptions, getRolePublicTargetOptions } from '../publicManage.api';

const Api = {
  list: '/sys/ts-role-publics',
  detail: '/sys/ts-role-publics/detail',
  create: '/sys/ts-role-publics',
  update: '/sys/ts-role-publics',
  delete: '/sys/ts-role-publics',
  submit: '/sys/ts-role-publics/submit',
  approve: '/sys/ts-role-publics/approve',
  reject: '/sys/ts-role-publics/reject',
  online: '/sys/ts-role-publics/online',
  offline: '/sys/ts-role-publics/offline',
} as const;

export const getRolePublicList = (params) => defHttp.get({ url: Api.list, params });

export const getRolePublicDetail = (params) => defHttp.get({ url: Api.detail, params });

export const saveOrUpdateRolePublic = (params, isUpdate) => {
  return isUpdate ? defHttp.put({ url: Api.update, params }) : defHttp.post({ url: Api.create, params });
};

export const deleteRolePublic = (params, handleSuccess) => {
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

export const submitRolePublic = (params) => defHttp.post({ url: Api.submit, params });
export const approveRolePublic = (params) => defHttp.post({ url: Api.approve, params });
export const rejectRolePublic = (params) => defHttp.post({ url: Api.reject, params });
export const onlineRolePublic = (params) => defHttp.post({ url: Api.online, params });
export const offlineRolePublic = (params) => defHttp.post({ url: Api.offline, params });

export const getRoleChannelOptions = async () => {
  const options = await getPublicChannelOptions({ targetType: 'role' });
  return (options || []).map((item) => ({
    label: item.label,
    value: item.value,
  }));
};

export const getRoleTargetOptions = (params) => getRolePublicTargetOptions(params);
