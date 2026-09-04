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
