<template>
  <BasicModal v-bind="$attrs" @register="registerModal" :title="getTitle" :width="640" @ok="handleSubmit" destroyOnClose>
    <BasicForm @register="registerForm" />
  </BasicModal>
</template>

<script lang="ts" setup>
  import { computed, ref, unref } from 'vue';
  import { BasicModal, useModalInner } from '/@/components/Modal';
  import { BasicForm, useForm } from '/@/components/Form';
  import { formSchema } from '../channel.data';
  import { getPublicChannelDetail, saveOrUpdatePublicChannel } from '../channel.api';

  const emit = defineEmits(['register', 'success']);
  const isUpdate = ref(false);
  const isView = ref(false);

  const [registerForm, { resetFields, setFieldsValue, validate, setProps }] = useForm({
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

    if (data?.record?.id) {
      const detail = await getPublicChannelDetail({ id: data.record.id });
      await setFieldsValue({
        ...detail,
      });
    }
  });

  const getTitle = computed(() => {
    if (unref(isView)) {
      return '渠道详情';
    }
    return unref(isUpdate) ? '编辑渠道' : '新增渠道';
  });

  async function handleSubmit() {
    try {
      const values = await validate();
      setModalProps({ confirmLoading: true });
      await saveOrUpdatePublicChannel(values, isUpdate.value);
      closeModal();
      emit('success');
    } finally {
      setModalProps({ confirmLoading: false });
    }
  }
</script>