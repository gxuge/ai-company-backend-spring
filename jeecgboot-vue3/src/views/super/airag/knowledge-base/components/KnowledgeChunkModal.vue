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
  import { createKnowledgeBaseChunk, updateKnowledgeBaseChunk } from '../KnowledgeBase.api';

  const emit = defineEmits(['success', 'register']);
  const title = ref('创建 Chunk');
  const isUpdate = ref(false);
  const kbId = ref('');
  const chunkId = ref('');

  const [registerForm, { resetFields, setFieldsValue, validate }] = useForm({
    schemas: [
      { label: 'id', field: 'id', component: 'Input', show: false },
      { label: '内容', field: 'content', required: true, component: 'InputTextArea', componentProps: { rows: 8, maxlength: 5000, showCount: true } },
      { label: 'chunk_type', field: 'chunk_type', component: 'Input', defaultValue: 'text' },
      { label: 'sort_no', field: 'sort_no', component: 'InputNumber', defaultValue: 1 },
      { label: 'status', field: 'status', component: 'RadioButtonGroup', defaultValue: 1, componentProps: { options: [{ label: '启用', value: 1 }, { label: '禁用', value: 0 }] } },
      { label: 'metadata_json', field: 'metadata_json', component: 'InputTextArea', componentProps: { rows: 4, placeholder: 'JSON 字符串' } },
    ],
    showActionButtonGroup: false,
    layout: 'vertical',
    wrapperCol: { span: 24 },
  });

  const [registerModal, { closeModal, setModalProps }] = useModalInner(async (data) => {
    await resetFields();
    setModalProps({ confirmLoading: false });
    isUpdate.value = !!data?.isUpdate;
    kbId.value = data?.kbId || '';
    chunkId.value = data?.chunkId || data?.record?.id || '';
    title.value = isUpdate.value ? '编辑 Chunk' : '创建 Chunk';
    if (isUpdate.value && data?.record) {
      await setFieldsValue(data.record);
    }
  });

  async function handleOk() {
    try {
      setModalProps({ confirmLoading: true });
      const values = await validate();
      if (unref(isUpdate)) {
        await updateKnowledgeBaseChunk(chunkId.value, values);
      } else {
        await createKnowledgeBaseChunk(kbId.value, values);
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
