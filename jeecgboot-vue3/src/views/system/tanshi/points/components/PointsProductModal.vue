<template>
  <BasicModal v-bind="$attrs" @register="registerModal" :title="title" :width="620" @ok="handleSubmit" destroyOnClose>
    <BasicForm @register="registerForm" />
  </BasicModal>
</template>

<script lang="ts" setup>
  import { computed, ref, unref } from 'vue';
  import { BasicForm, useForm } from '/@/components/Form';
  import { BasicModal, useModalInner } from '/@/components/Modal';
  import { savePointsProduct } from '../points.api';
  import { productFormSchema } from '../points.data';

  const emit = defineEmits(['register', 'success']);
  const isUpdate = ref(false);

  const [registerForm, { resetFields, setFieldsValue, validate }] = useForm({
    schemas: productFormSchema,
    showActionButtonGroup: false,
    labelWidth: 90,
  });

  const [registerModal, { setModalProps, closeModal }] = useModalInner(async (data) => {
    await resetFields();
    isUpdate.value = !!data?.record?.id;
    await setFieldsValue({
      points: 1,
      giftPoints: 0,
      originalAmount: 0.01,
      actualAmount: 0.01,
      currency: 'USD',
      status: 1,
      sort: 0,
      ...(data?.record || {}),
    });
    setModalProps({ confirmLoading: false });
  });

  const title = computed(() => `${unref(isUpdate) ? '编辑' : '新增'}积分充值商品`);

  async function handleSubmit() {
    const values = await validate();
    setModalProps({ confirmLoading: true });
    try {
      await savePointsProduct(values);
      closeModal();
      emit('success');
    } finally {
      setModalProps({ confirmLoading: false });
    }
  }
</script>
