<template>
  <BasicModal v-bind="$attrs" @register="registerModal" :title="title" :width="760" @ok="handleSubmit" destroyOnClose>
    <BasicForm @register="registerForm" />
  </BasicModal>
</template>

<script lang="ts" setup>
  import { computed, ref, unref } from 'vue';
  import { BasicForm, useForm } from '/@/components/Form';
  import { BasicModal, useModalInner } from '/@/components/Modal';
  import { createAdContent, updateAdContent } from '../adCenter.api';
  import { contentFormSchema } from '../adCenter.data';

  const emit = defineEmits(['register', 'success']);
  const isUpdate = ref(false);

  const [registerForm, { resetFields, setFieldsValue, updateSchema, validate }] = useForm({
    schemas: contentFormSchema,
    showActionButtonGroup: false,
    labelWidth: 100,
  });

  const [registerModal, { setModalProps, closeModal }] = useModalInner(async (data) => {
    await resetFields();
    isUpdate.value = !!data?.record?.id;
    await updateSchema([
      {
        field: 'slotId',
        componentProps: {
          options: data?.slotOptions || [],
          showSearch: true,
          optionFilterProp: 'label',
          placeholder: '请选择广告位',
        },
      },
      {
        field: 'contentCode',
        dynamicDisabled: isUpdate.value,
      },
    ]);
    await setFieldsValue({
      sourceType: 'SELF',
      mediaType: 'IMAGE',
      linkType: 'NONE',
      sortOrder: 0,
      ...(data?.record || {}),
      mediaUrl: data?.record?.mediaUrl || data?.record?.imageUrl,
      mediaUploadUrl: data?.record?.mediaUrl || data?.record?.imageUrl,
      mediaFileUrl: data?.record?.mediaUrl || data?.record?.imageUrl,
      actionType: data?.record?.actionType || data?.record?.linkType || 'NONE',
      actionPayload: data?.record?.actionPayload || data?.record?.linkValue,
    });
    setModalProps({ confirmLoading: false });
  });

  const title = computed(() => `${unref(isUpdate) ? '编辑' : '新增'}广告内容`);

  async function handleSubmit() {
    const values = await validate();
    setModalProps({ confirmLoading: true });
    try {
      const payload = {
        ...values,
        mediaUrl: values.mediaType === 'CARD' ? undefined : values.mediaUrl || values.mediaUploadUrl || values.mediaFileUrl,
        imageUrl: values.mediaType === 'CARD' ? undefined : values.mediaUrl || values.mediaUploadUrl || values.mediaFileUrl,
        actionPayload: values.actionType === 'NONE' ? undefined : values.actionPayload,
        linkType: values.actionType,
        linkValue: values.actionType === 'NONE' ? undefined : values.actionPayload,
      };
      if (isUpdate.value) {
        await updateAdContent(payload);
      } else {
        await createAdContent(payload);
      }
      closeModal();
      emit('success');
    } finally {
      setModalProps({ confirmLoading: false });
    }
  }
</script>
