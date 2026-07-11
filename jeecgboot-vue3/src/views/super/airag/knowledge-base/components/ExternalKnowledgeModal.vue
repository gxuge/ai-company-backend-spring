<template>
  <BasicModal destroyOnClose @register="registerModal" :canFullscreen="false" width="780px" :title="title" @ok="handleOk" @cancel="handleCancel">
    <BasicForm @register="registerForm" />
  </BasicModal>
</template>

<script lang="ts" setup>
  import { ref, unref } from 'vue';
  import BasicModal from '/@/components/Modal/src/BasicModal.vue';
  import { useModalInner } from '/@/components/Modal';
  import BasicForm from '/@/components/Form/src/BasicForm.vue';
  import { useForm } from '/@/components/Form';
  import { createExternalKnowledgeBase, updateExternalKnowledgeBase } from '../KnowledgeBase.api';

  const emit = defineEmits(['success', 'register']);
  const title = ref('外部知识库');
  const isUpdate = ref(false);
  const recordId = ref('');

  const [registerForm, { resetFields, setFieldsValue, validate }] = useForm({
    schemas: [
      { label: 'external_kb_id', field: 'external_kb_id', required: true, component: 'Input' },
      { label: 'name', field: 'name', required: true, component: 'Input' },
      { label: 'endpoint_url', field: 'endpoint_url', required: true, component: 'Input' },
      { label: 'enabled', field: 'enabled', component: 'Switch', defaultValue: true },
      {
        label: 'auth_type',
        field: 'auth_type',
        component: 'Select',
        defaultValue: 'none',
        componentProps: {
          options: [
            { label: 'none', value: 'none' },
            { label: 'api_key', value: 'api_key' },
            { label: 'bearer', value: 'bearer' },
          ],
        },
      },
      { label: 'auth_config', field: 'auth_config', component: 'InputTextArea', componentProps: { rows: 4, placeholder: 'JSON' } },
      { label: 'timeout_ms', field: 'timeout_ms', component: 'InputNumber', defaultValue: 8000 },
      { label: 'weight', field: 'weight', component: 'InputNumber', defaultValue: 1 },
      { label: 'metadata_json', field: 'metadata_json', component: 'InputTextArea', componentProps: { rows: 4 } },
    ],
    showActionButtonGroup: false,
    layout: 'vertical',
    wrapperCol: { span: 24 },
  });

  const [registerModal, { closeModal, setModalProps }] = useModalInner(async (data) => {
    await resetFields();
    setModalProps({ confirmLoading: false });
    isUpdate.value = !!data?.isUpdate;
    recordId.value = data?.id || data?.record?.id || '';
    title.value = isUpdate.value ? '编辑外部知识库' : '新增外部知识库';
    if (data?.record) {
      await setFieldsValue(data.record);
    }
  });

  async function handleOk() {
    try {
      setModalProps({ confirmLoading: true });
      const values = await validate();
      if (unref(isUpdate)) {
        await updateExternalKnowledgeBase(recordId.value, values);
      } else {
        await createExternalKnowledgeBase(values);
      }
      closeModal();
      emit('success');
    } finally {
      setModalProps({ confirmLoading: false });
    }
  }

  function handleCancel() {
    closeModal();
  }
</script>

