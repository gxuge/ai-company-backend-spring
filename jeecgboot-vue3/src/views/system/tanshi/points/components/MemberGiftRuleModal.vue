<template>
  <BasicModal v-bind="$attrs" @register="registerModal" :title="title" :width="620" @ok="handleSubmit" destroyOnClose>
    <BasicForm @register="registerForm" />
  </BasicModal>
</template>

<script lang="ts" setup>
  import { computed, ref, unref } from 'vue';
  import { BasicForm, useForm } from '/@/components/Form';
  import { BasicModal, useModalInner } from '/@/components/Modal';
  import { saveMemberGiftRule } from '../points.api';
  import { getGiftRuleFormSchema } from '../points.data';

  const emit = defineEmits(['register', 'success']);
  const isUpdate = ref(false);

  const [registerForm, { resetFields, setFieldsValue, updateSchema, validate }] = useForm({
    schemas: getGiftRuleFormSchema([], [{ label: '等级默认规则', value: 0 }]),
    showActionButtonGroup: false,
    labelWidth: 100,
  });

  const [registerModal, { setModalProps, closeModal }] = useModalInner(async (data) => {
    await resetFields();
    isUpdate.value = !!data?.record?.id;
    await updateSchema([
      {
        field: 'planId',
        componentProps: {
          options: data?.planOptions || [],
          showSearch: true,
          optionFilterProp: 'label',
          placeholder: '请选择会员等级',
        },
      },
      {
        field: 'productId',
        componentProps: {
          options: [{ label: '等级默认规则', value: 0 }, ...(data?.productOptions || [])],
          showSearch: true,
          optionFilterProp: 'label',
          placeholder: '请选择套餐或等级默认规则',
        },
      },
    ]);
    await setFieldsValue({
      productId: 0,
      giftPoints: 1,
      status: 1,
      ...(data?.record || {}),
    });
    setModalProps({ confirmLoading: false });
  });

  const title = computed(() => `${unref(isUpdate) ? '编辑' : '新增'}会员赠送积分规则`);

  async function handleSubmit() {
    const values = await validate();
    setModalProps({ confirmLoading: true });
    try {
      await saveMemberGiftRule(values);
      closeModal();
      emit('success');
    } finally {
      setModalProps({ confirmLoading: false });
    }
  }
</script>
