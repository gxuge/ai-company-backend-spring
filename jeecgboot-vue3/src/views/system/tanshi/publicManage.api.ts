import { defHttp } from '/@/utils/http/axios';

enum Api {
  userOptions = '/sys/ts-public-manage/user-options',
  channelOptions = '/sys/ts-public-channels/options',
  storyOptions = '/sys/ts-story-publics/story-options',
  roleOptions = '/sys/ts-role-publics/role-options',
}

export const getPublicManageUserOptions = (params) => defHttp.get({ url: Api.userOptions, params });

export const getPublicChannelOptions = (params) => defHttp.get({ url: Api.channelOptions, params });

export const getStoryPublicTargetOptions = (params) => defHttp.get({ url: Api.storyOptions, params });

export const getRolePublicTargetOptions = (params) => defHttp.get({ url: Api.roleOptions, params });
