<template>
  <BasicModal destroyOnClose @register="registerModal" :canFullscreen="false" width="820px" :title="title" @ok="handleOk" @cancel="handleCancel">
    <BasicForm @register="registerForm" />
  </BasicModal>
</template>

<script lang="ts" setup>
  import { ref, unref } from 'vue';
  import BasicModal from '/@/components/Modal/src/BasicModal.vue';
  import { useModalInner } from '/@/components/Modal';
  import BasicForm from '/@/components/Form/src/BasicForm.vue';
  import { useForm } from '/@/components/Form';
  import {
    createKnowledgeBaseChunkIndex,
    getKnowledgeBaseChunkIndexDetail,
    updateKnowledgeBaseChunkIndex,
  } from '../KnowledgeBase.api';

  const emit = defineEmits(['success', 'register']);
  const title = ref('创建索引');
  const isUpdate = ref(false);
  const kbId = ref('');
  const chunkId = ref('');
  const indexId = ref('');

  const [registerForm, { resetFields, setFieldsValue, validate }] = useForm({
    schemas: [
      { label: 'id', field: 'id', component: 'Input', show: false },
      { label: 'chunk_id', field: 'chunk_id', required: true, component: 'Input', defaultValue: '', helpMessage: '索引必须绑定到具体 chunk' },
      { label: 'index_text', field: 'index_text', required: true, component: 'InputTextArea', componentProps: { rows: 6, maxlength: 5000, showCount: true } },
      {
        label: 'index_type',
        field: 'index_type',
        component: 'Select',
        defaultValue: 'default',
        componentProps: {
          options: [
            { label: 'default', value: 'default' },
            { label: 'manual', value: 'manual' },
            { label: 'question', value: 'question' },
            { label: 'keyword', value: 'keyword' },
            { label: 'summary', value: 'summary' },
            { label: 'auto_question', value: 'auto_question' },
          ],
        },
      },
      {
        label: 'embedding_status',
        field: 'embedding_status',
        component: 'Select',
        defaultValue: 'pending',
        componentProps: {
          options: [
            { label: 'pending', value: 'pending' },
            { label: 'processing', value: 'processing' },
            { label: 'success', value: 'success' },
            { label: 'failed', value: 'failed' },
          ],
        },
      },
      {
        label: 'status',
        field: 'status',
        component: 'RadioButtonGroup',
        defaultValue: 1,
        componentProps: {
          options: [
            { label: '启用', value: 1 },
            { label: '禁用', value: 0 },
          ],
        },
      },
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
    chunkId.value = data?.chunkId || '';
    indexId.value = data?.indexId || data?.record?.id || '';
    title.value = isUpdate.value ? '编辑索引' : '创建索引';
    if (isUpdate.value && data?.record) {
      await setFieldsValue(data.record);
    } else if (chunkId.value) {
      await setFieldsValue({ chunk_id: chunkId.value });
    }
    if (isUpdate.value && !data?.record && indexId.value) {
      const res = await getKnowledgeBaseChunkIndexDetail(indexId.value);
      if (res?.success && res?.result) {
        await setFieldsValue(res.result);
      }
    }
  });

  async function handleOk() {
    try {
      setModalProps({ confirmLoading: true });
      const values = await validate();
      const realChunkId = values.chunk_id || chunkId.value;
      if (!realChunkId) {
        return;
      }
      if (unref(isUpdate)) {
        await updateKnowledgeBaseChunkIndex(indexId.value, values);
      } else {
        await createKnowledgeBaseChunkIndex(kbId.value, realChunkId, values);
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
