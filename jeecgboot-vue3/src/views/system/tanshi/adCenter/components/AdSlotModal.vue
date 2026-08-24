<template>
  <BasicModal v-bind="$attrs" @register="registerModal" :title="title" :width="640" @ok="handleSubmit" destroyOnClose>
    <BasicForm @register="registerForm" />
  </BasicModal>
</template>

<script lang="ts" setup>
  import { computed, ref, unref } from 'vue';
  import { BasicForm, useForm } from '/@/components/Form';
  import { BasicModal, useModalInner } from '/@/components/Modal';
  import { createAdSlot, updateAdSlot } from '../adCenter.api';
  import { slotFormSchema } from '../adCenter.data';

  const emit = defineEmits(['register', 'success']);
  const isUpdate = ref(false);

  const [registerForm, { resetFields, setFieldsValue, updateSchema, validate }] = useForm({
    schemas: slotFormSchema,
    showActionButtonGroup: false,
    labelWidth: 100,
  });

  const [registerModal, { setModalProps, closeModal }] = useModalInner(async (data) => {
    await resetFields();
    isUpdate.value = !!data?.record?.id;
    await updateSchema({
      field: 'slotCode',
      dynamicDisabled: isUpdate.value,
    });
    await setFieldsValue({
      slotType: 'BANNER',
      maxItems: 1,
      status: 'ENABLED',
      ...(data?.record || {}),
    });
    setModalProps({ confirmLoading: false });
  });

  const title = computed(() => `${unref(isUpdate) ? '编辑' : '新增'}广告位`);

  async function handleSubmit() {
    const values = await validate();
    setModalProps({ confirmLoading: true });
    try {
      if (isUpdate.value) {
        await updateAdSlot(values);
      } else {
        await createAdSlot(values);
      }
      closeModal();
      emit('success');
    } finally {
      setModalProps({ confirmLoading: false });
    }
  }
</script>
