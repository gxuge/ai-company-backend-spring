<template>
  <BasicModal v-bind="$attrs" @register="registerModal" :title="title" :width="620" @ok="handleSubmit" destroyOnClose>
    <BasicForm @register="registerForm" />
  </BasicModal>
</template>

<script lang="ts" setup>
  import { computed, ref, unref } from 'vue';
  import { BasicModal, useModalInner } from '/@/components/Modal';
  import { BasicForm, useForm } from '/@/components/Form';
  import { saveActivityRewardRule } from '../activity.api';
  import { getRewardRuleFormSchema } from '../activity.data';

  const emit = defineEmits(['register', 'success']);
  const isUpdate = ref(false);

  const [registerForm, { resetFields, setFieldsValue, updateSchema, validate }] = useForm({
    schemas: getRewardRuleFormSchema([]),
    showActionButtonGroup: false,
    labelWidth: 110,
  });

  const [registerModal, { setModalProps, closeModal }] = useModalInner(async (data) => {
    await resetFields();
    isUpdate.value = !!data?.record?.id;
    await updateSchema({
      field: 'taskId',
      componentProps: {
        options: data?.taskOptions || [],
        placeholder: '请选择活动任务',
        showSearch: true,
        optionFilterProp: 'label',
      },
    });
    await setFieldsValue({
      memberLevel: 'NORMAL',
      extraRewardType: 'STAR_DIAMOND',
      extraRewardValue: 0,
      status: 1,
      ...(data?.record || {}),
    });
    setModalProps({ confirmLoading: false });
  });

  const title = computed(() => `${unref(isUpdate) ? '编辑' : '新增'}会员奖励加成`);

  async function handleSubmit() {
    const values = await validate();
    setModalProps({ confirmLoading: true });
    try {
      await saveActivityRewardRule(values);
      closeModal();
      emit('success');
    } finally {
      setModalProps({ confirmLoading: false });
    }
  }
</script>
