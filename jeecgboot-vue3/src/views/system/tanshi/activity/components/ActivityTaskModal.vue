<template>
  <BasicModal v-bind="$attrs" @register="registerModal" :title="title" :width="700" @ok="handleSubmit" destroyOnClose>
    <BasicForm @register="registerForm" />
  </BasicModal>
</template>

<script lang="ts" setup>
  import { computed, ref, unref } from 'vue';
  import { BasicModal, useModalInner } from '/@/components/Modal';
  import { BasicForm, useForm } from '/@/components/Form';
  import { createActivityTask, updateActivityTask } from '../activity.api';
  import { taskFormSchema } from '../activity.data';

  const emit = defineEmits(['register', 'success']);
  const isUpdate = ref(false);

  const [registerForm, { resetFields, setFieldsValue, validate }] = useForm({
    schemas: taskFormSchema,
    showActionButtonGroup: false,
    labelWidth: 100,
  });

  const [registerModal, { setModalProps, closeModal }] = useModalInner(async (data) => {
    await resetFields();
    isUpdate.value = !!data?.record?.id;
    await setFieldsValue({
      taskType: 'TASK',
      category: 'DAILY',
      conditionType: 'LOGIN',
      conditionValue: 1,
      rewardType: 'STAR_DIAMOND',
      rewardValue: 1,
      rewardClaimMode: 'MANUAL',
      status: 'ENABLED',
      sort: 0,
      ...(data?.record || {}),
    });
    setModalProps({ confirmLoading: false });
  });

  const title = computed(() => `${unref(isUpdate) ? '编辑' : '新增'}活动任务`);

  async function handleSubmit() {
    const values = await validate();
    setModalProps({ confirmLoading: true });
    try {
      if (isUpdate.value) {
        await updateActivityTask(values);
      } else {
        await createActivityTask(values);
      }
      closeModal();
      emit('success');
    } finally {
      setModalProps({ confirmLoading: false });
    }
  }
</script>
