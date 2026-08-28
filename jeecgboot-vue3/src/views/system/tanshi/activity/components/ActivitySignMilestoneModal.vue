<template>
  <BasicModal v-bind="$attrs" @register="registerModal" :title="title" :width="620" @ok="handleSubmit" destroyOnClose>
    <BasicForm @register="registerForm" />
  </BasicModal>
</template>

<script lang="ts" setup>
  import { computed, ref, unref } from 'vue';
  import { BasicModal, useModalInner } from '/@/components/Modal';
  import { BasicForm, useForm } from '/@/components/Form';
  import { saveActivitySignMilestoneRule } from '../activity.api';
  import { getSignMilestoneFormSchema } from '../activity.data';

  const emit = defineEmits(['register', 'success']);
  const isUpdate = ref(false);

  const [registerForm, { resetFields, setFieldsValue, updateSchema, validate }] = useForm({
    schemas: getSignMilestoneFormSchema([]),
    showActionButtonGroup: false,
    labelWidth: 100,
  });

  const [registerModal, { setModalProps, closeModal }] = useModalInner(async (data) => {
    await resetFields();
    isUpdate.value = !!data?.record?.id;
    await updateSchema({
      field: 'taskId',
      componentProps: {
        options: data?.taskOptions || [],
        placeholder: '请选择签到任务',
        showSearch: true,
        optionFilterProp: 'label',
      },
    });
    await setFieldsValue({
      milestoneDay: 4,
      rewardType: 'STAR_DIAMOND',
      rewardValue: 10,
      status: 1,
      ...(data?.record || {}),
    });
    setModalProps({ confirmLoading: false });
  });

  const title = computed(() => `${unref(isUpdate) ? '编辑' : '新增'}签到里程碑奖励`);

  async function handleSubmit() {
    const values = await validate();
    setModalProps({ confirmLoading: true });
    try {
      await saveActivitySignMilestoneRule(values);
      closeModal();
      emit('success');
    } finally {
      setModalProps({ confirmLoading: false });
    }
  }
</script>
