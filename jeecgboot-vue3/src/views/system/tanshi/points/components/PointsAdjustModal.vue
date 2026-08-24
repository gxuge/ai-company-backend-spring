<template>
  <BasicModal v-bind="$attrs" @register="registerModal" title="调整用户积分" :width="560" @ok="handleSubmit" destroyOnClose>
    <BasicForm @register="registerForm" />
  </BasicModal>
</template>

<script lang="ts" setup>
  import { BasicForm, useForm } from '/@/components/Form';
  import { BasicModal, useModalInner } from '/@/components/Modal';
  import { adjustPoints } from '../points.api';
  import { adjustFormSchema } from '../points.data';

  const emit = defineEmits(['register', 'success']);

  const [registerForm, { resetFields, setFieldsValue, validate }] = useForm({
    schemas: adjustFormSchema,
    showActionButtonGroup: false,
    labelWidth: 90,
  });

  const [registerModal, { setModalProps, closeModal }] = useModalInner(async (data) => {
    await resetFields();
    await setFieldsValue({
      userId: data?.record?.userId,
      operation: 'ADD',
      amount: 1,
      reason: '',
      idempotencyKey: `ADMIN_UI:${Date.now()}:${Math.random().toString(36).slice(2, 10)}`,
    });
    setModalProps({ confirmLoading: false });
  });

  async function handleSubmit() {
    const values = await validate();
    setModalProps({ confirmLoading: true });
    try {
      await adjustPoints(values);
      closeModal();
      emit('success');
    } finally {
      setModalProps({ confirmLoading: false });
    }
  }
</script>
