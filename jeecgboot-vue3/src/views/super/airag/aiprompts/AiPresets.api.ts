import { defHttp } from '/@/utils/http/axios';
import { useMessage } from '/@/hooks/web/useMessage';

const { createConfirm } = useMessage();

enum PresetApi {
  list = '/sys/tsPreset/list',
  queryById = '/sys/tsPreset/queryById',
  add = '/sys/tsPreset/add',
  edit = '/sys/tsPreset/edit',
  deleteOne = '/sys/tsPreset/delete',
  deleteBatch = '/sys/tsPreset/deleteBatch',
}

enum PresetTagApi {
  tagList = '/sys/tsTag/list',
  tagTypeList = '/sys/tsTagType/list',
  tagRelationList = '/sys/tsTagRelation/list',
  addTag = '/sys/tsTag/add',
}
export const listPresets = (params) => defHttp.get({ url: PresetApi.list, params });

export const queryPresetById = (params) =>
  defHttp.get({ url: PresetApi.queryById, params }, { isTransformResponse: false });

export const saveOrUpdatePreset = (params, isUpdate) =>
  isUpdate ? defHttp.post({ url: PresetApi.edit, params }) : defHttp.post({ url: PresetApi.add, params });

export const deletePreset = (params, handleSuccess) =>
  defHttp.delete({ url: PresetApi.deleteOne, params }, { joinParamsToUrl: true }).then(() => handleSuccess());

export const batchDeletePreset = (params, handleSuccess) => {
  createConfirm({
    iconType: 'warning',
    title: '确认删除',
    content: '是否删除选中数据',
    okText: '确认',
    cancelText: '取消',
    onOk: () =>
      defHttp
        .delete({ url: PresetApi.deleteBatch, data: params }, { joinParamsToUrl: true })
        .then(() => handleSuccess()),
  });
};

export const listTags = (params) => defHttp.get({ url: PresetTagApi.tagList, params });
export const listTagTypes = (params) => defHttp.get({ url: PresetTagApi.tagTypeList, params });
export const listTagRelations = (params) => defHttp.get({ url: PresetTagApi.tagRelationList, params });

export const addTagForPreset = (params) => defHttp.post({ url: PresetTagApi.addTag, params });
