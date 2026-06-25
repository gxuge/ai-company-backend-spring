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
  import { formSchema } from '../storyPublic.data';
  import { getStoryChannelOptions, getStoryPublicDetail, getStoryTargetOptions, saveOrUpdateStoryPublic } from '../storyPublic.api';
  import { getPublicManageUserOptions } from '../../publicManage.api';

  const emit = defineEmits(['register', 'success']);
  const isUpdate = ref(false);
  const isView = ref(false);
  const userKeyword = ref('');
  const storyKeyword = ref('');
  const selectedOwnerUserId = ref<string | undefined>();
  const channelOptionsCache = ref<any[] | null>(null);
  const ownerUserOptionsCache = new Map<string, any>();
  const storyOptionsCache = new Map<string, any>();

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
    storyKeyword.value = '';
    selectedOwnerUserId.value = undefined;

    const channelOptions = await getCachedChannelOptions();
    updateSchema([
      {
        field: 'ownerUserId',
        componentProps: buildOwnerUserProps(),
      },
      {
        field: 'storyId',
        componentProps: buildStoryProps(undefined),
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
      const detail = await getStoryPublicDetail({ id: data.record.id });
      selectedOwnerUserId.value = detail?.ownerUserId;
      updateSchema({
        field: 'storyId',
        componentProps: buildStoryProps(detail?.ownerUserId),
      });
      await setFieldsValue({
        ...detail,
      });
    }
  });

  const getTitle = computed(() => {
    if (unref(isView)) {
      return '故事公开记录详情';
    }
    return unref(isUpdate) ? '编辑故事公开记录' : '新增故事公开记录';
  });

  async function handleSubmit() {
    try {
      const values = await validate();
      setModalProps({ confirmLoading: true });
      await saveOrUpdateStoryPublic(values, isUpdate.value);
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

  function buildStoryProps(ownerUserId?: string) {
    return {
      api: fetchStoryOptions,
      params: {
        ownerUserId,
        keyword: storyKeyword.value,
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
      placeholder: ownerUserId ? '请选择故事' : '请先选择所属用户',
      onSearch: debounceStorySearch,
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
    storyKeyword.value = '';
    setFieldsValue({ storyId: undefined });
    updateSchema({
      field: 'storyId',
      componentProps: buildStoryProps(normalizedValue),
    });
  }

  function handleStorySearch(value) {
    storyKeyword.value = value || '';
    updateSchema({
      field: 'storyId',
      componentProps: buildStoryProps(selectedOwnerUserId.value),
    });
  }

  const debounceOwnerSearch = useDebounceFn(handleOwnerSearch, 300);
  const debounceStorySearch = useDebounceFn(handleStorySearch, 300);

  async function getCachedChannelOptions() {
    if (channelOptionsCache.value) {
      return channelOptionsCache.value;
    }
    const options = await getStoryChannelOptions();
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

  async function fetchStoryOptions(params) {
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
    if (storyOptionsCache.has(cacheKey)) {
      return storyOptionsCache.get(cacheKey);
    }
    const result = await getStoryTargetOptions(params);
    storyOptionsCache.set(cacheKey, result);
    return result;
  }
</script>
