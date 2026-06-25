<template>
  <BasicModal v-bind="$attrs" @register="registerModal" :title="getTitle" :width="720" @ok="handleSubmit" destroyOnClose>
    <BasicForm @register="registerForm" />
  </BasicModal>
</template>

<script lang="ts" setup>
  import { computed, ref, unref } from 'vue';
  import { useDebounceFn } from '@vueuse/core';
  import { BasicModal, useModalInner } from '/@/components/Modal';
  import { BasicForm, useForm } from '/@/components/Form';
  import { formSchema } from '../rolePublic.data';
  import { getRoleChannelOptions, getRolePublicDetail, getRoleTargetOptions, saveOrUpdateRolePublic } from '../rolePublic.api';
  import { getPublicManageUserOptions } from '../../publicManage.api';

  const emit = defineEmits(['register', 'success']);
  const isUpdate = ref(false);
  const isView = ref(false);
  const userKeyword = ref('');
  const roleKeyword = ref('');
  const selectedOwnerUserId = ref<string | undefined>();
  const channelOptionsCache = ref<any[] | null>(null);
  const ownerUserOptionsCache = new Map<string, any>();
  const roleOptionsCache = new Map<string, any>();

  const [registerForm, { resetFields, setFieldsValue, validate, setProps, updateSchema }] = useForm({
    schemas: formSchema,
    showActionButtonGroup: false,
    labelWidth: 110,
  });

  const [registerModal, { setModalProps, closeModal }] = useModalInner(async (data) => {
    await resetFields();
    isUpdate.value = !!data?.isUpdate;
    isView.value = !!data?.viewMode;
    setModalProps({
      confirmLoading: false,
      showOkBtn: !isView.value,
      showCancelBtn: !isView.value,
    });
    setProps({ disabled: isView.value });
    userKeyword.value = '';
    roleKeyword.value = '';
    selectedOwnerUserId.value = undefined;

    const channelOptions = await getCachedChannelOptions();
    updateSchema([
      {
        field: 'ownerUserId',
        componentProps: buildOwnerUserProps(),
      },
      {
        field: 'roleId',
        componentProps: buildRoleProps(undefined),
      },
      {
        field: 'channelCode',
        componentProps: {
          options: channelOptions,
          placeholder: '请选择渠道',
        },
      },
    ]);

    if (data?.record?.id) {
      const detail = await getRolePublicDetail({ id: data.record.id });
      selectedOwnerUserId.value = detail?.ownerUserId;
      updateSchema({
        field: 'roleId',
        componentProps: buildRoleProps(detail?.ownerUserId),
      });
      await setFieldsValue({
        ...detail,
      });
    }
  });

  const getTitle = computed(() => {
    if (unref(isView)) {
      return '角色公开记录详情';
    }
    return unref(isUpdate) ? '编辑角色公开记录' : '新增角色公开记录';
  });

  async function handleSubmit() {
    try {
      const values = await validate();
      setModalProps({ confirmLoading: true });
      await saveOrUpdateRolePublic(values, isUpdate.value);
      closeModal();
      emit('success');
    } finally {
      setModalProps({ confirmLoading: false });
    }
  }

  function buildOwnerUserProps() {
    return {
      api: fetchOwnerUserOptions,
      params: { keyword: userKeyword.value },
      resultField: 'records',
      labelField: 'displayName',
      valueField: 'id',
      immediate: false,
      showSearch: true,
      filterOption: false,
      pageConfig: {
        isPage: true,
      },
      placeholder: '请选择所属用户',
      onSearch: debounceOwnerSearch,
      onChange: handleOwnerChange,
    };
  }

  function buildRoleProps(ownerUserId?: string) {
    return {
      api: fetchRoleOptions,
      params: {
        ownerUserId,
        keyword: roleKeyword.value,
      },
      resultField: 'records',
      labelField: 'label',
      valueField: 'value',
      immediate: false,
      showSearch: true,
      filterOption: false,
      disabled: isView.value || !ownerUserId,
      pageConfig: {
        isPage: true,
      },
      placeholder: ownerUserId ? '请选择角色' : '请先选择所属用户',
      onSearch: debounceRoleSearch,
    };
  }

  function handleOwnerSearch(value) {
    userKeyword.value = value || '';
    updateSchema({
      field: 'ownerUserId',
      componentProps: buildOwnerUserProps(),
    });
  }

  function handleOwnerChange(value) {
    const normalizedValue = value || undefined;
    if (selectedOwnerUserId.value === normalizedValue) {
      return;
    }
    selectedOwnerUserId.value = normalizedValue;
    roleKeyword.value = '';
    setFieldsValue({ roleId: undefined });
    updateSchema({
      field: 'roleId',
      componentProps: buildRoleProps(normalizedValue),
    });
  }

  function handleRoleSearch(value) {
    roleKeyword.value = value || '';
    updateSchema({
      field: 'roleId',
      componentProps: buildRoleProps(selectedOwnerUserId.value),
    });
  }

  const debounceOwnerSearch = useDebounceFn(handleOwnerSearch, 300);
  const debounceRoleSearch = useDebounceFn(handleRoleSearch, 300);

  async function getCachedChannelOptions() {
    if (channelOptionsCache.value) {
      return channelOptionsCache.value;
    }
    const options = await getRoleChannelOptions();
    channelOptionsCache.value = options || [];
    return channelOptionsCache.value;
  }

  async function fetchOwnerUserOptions(params) {
    const cacheKey = JSON.stringify({
      keyword: params?.keyword || '',
      pageNo: params?.pageNo || 1,
      pageSize: params?.pageSize || 10,
    });
    if (ownerUserOptionsCache.has(cacheKey)) {
      return ownerUserOptionsCache.get(cacheKey);
    }
    const result = await getPublicManageUserOptions(params);
    ownerUserOptionsCache.set(cacheKey, result);
    return result;
  }

  async function fetchRoleOptions(params) {
    const ownerUserId = params?.ownerUserId;
    if (!ownerUserId) {
      return {
        records: [],
        total: 0,
        current: params?.pageNo || 1,
        size: params?.pageSize || 10,
      };
    }
    const cacheKey = JSON.stringify({
      ownerUserId,
      keyword: params?.keyword || '',
      pageNo: params?.pageNo || 1,
      pageSize: params?.pageSize || 10,
    });
    if (roleOptionsCache.has(cacheKey)) {
      return roleOptionsCache.get(cacheKey);
    }
    const result = await getRoleTargetOptions(params);
    roleOptionsCache.set(cacheKey, result);
    return result;
  }
</script>
