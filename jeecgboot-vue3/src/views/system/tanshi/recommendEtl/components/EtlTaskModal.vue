<template>
  <BasicModal v-bind="$attrs" @register="registerModal" :title="title" :width="820" @ok="handleSubmit" destroyOnClose>
    <BasicForm @register="registerForm" />
  </BasicModal>
</template>

<script lang="ts" setup>
  import { computed, ref, unref } from 'vue';
  import { BasicForm, useForm } from '/@/components/Form';
  import { BasicModal, useModalInner } from '/@/components/Modal';
  import { createEtlTask, updateEtlTask, type EtlTask } from '../recommendEtl.api';
  import { etlTaskFormSchema } from '../recommendEtl.data';

  const emit = defineEmits(['register', 'success']);
  const isUpdate = ref(false);
  const recordId = ref<EtlTask['id']>();

  const [registerForm, { resetFields, setFieldsValue, validate }] = useForm({
    schemas: etlTaskFormSchema,
    showActionButtonGroup: false,
    labelWidth: 100,
  });

  const [registerModal, { setModalProps, closeModal }] = useModalInner(async (data) => {
    await resetFields();
    const record: EtlTask = data?.record || {};
    isUpdate.value = !!record.id;
    recordId.value = record.id;
    await setFieldsValue({
      recommendType: 'ROLE',
      timeRangeMode: 'RECENT_DAYS',
      recentDays: 30,
      storageType: 'LOCAL',
      trainRatio: 0.9,
      evalRatio: 0.1,
      timeoutSeconds: 3600,
      ...record,
      enabled: record.id ? record.enabled === 1 : false,
    });
    setModalProps({ confirmLoading: false });
  });

  const title = computed(() => `${unref(isUpdate) ? '编辑' : '新增'}推荐 ETL 任务`);

  async function handleSubmit() {
    const values = await validate();
    const payload: EtlTask = {
      ...values,
      id: recordId.value,
      enabled: values.enabled ? 1 : 0,
    };
    setModalProps({ confirmLoading: true });
    try {
      if (isUpdate.value) {
        await updateEtlTask(payload);
      } else {
        delete payload.id;
        await createEtlTask(payload);
      }
      closeModal();
      emit('success');
    } finally {
      setModalProps({ confirmLoading: false });
    }
  }
</script>
