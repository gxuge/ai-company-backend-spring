<template>
  <BasicModal
    destroyOnClose
    @register="registerModal"
    :canFullscreen="false"
    width="720px"
    :title="title"
    @ok="handleOk"
    @cancel="handleCancel"
  >
    <BasicForm @register="registerForm" />
  </BasicModal>
</template>

<script lang="ts" setup>
  import { ref, unref } from 'vue';
  import BasicModal from '/@/components/Modal/src/BasicModal.vue';
  import { useModalInner } from '/@/components/Modal';
  import BasicForm from '/@/components/Form/src/BasicForm.vue';
  import { useForm } from '/@/components/Form';
  import { knowledgeBaseFormSchema } from '../KnowledgeBase.data';
  import { createKnowledgeBase, getKnowledgeBaseDetail, updateKnowledgeBase } from '../KnowledgeBase.api';

  const emit = defineEmits(['success', 'register']);

  const title = ref('创建知识库');
  const isUpdate = ref(false);

  const [registerForm, { resetFields, setFieldsValue, validate }] = useForm({
    schemas: knowledgeBaseFormSchema,
    showActionButtonGroup: false,
    layout: 'vertical',
    wrapperCol: { span: 24 },
  });

  const [registerModal, { closeModal, setModalProps }] = useModalInner(async (data) => {
    await resetFields();
    setModalProps({ confirmLoading: false });
    isUpdate.value = !!data?.isUpdate;
    title.value = isUpdate.value ? '编辑知识库' : '创建知识库';
    if (unref(isUpdate) && data?.id) {
      const res = await getKnowledgeBaseDetail(data.id);
      if (res?.success && res?.result) {
        await setFieldsValue(res.result);
      } else if (data?.record) {
        await setFieldsValue(data.record);
      }
    }
    setModalProps({ minHeight: 420, bodyStyle: { padding: '10px' } });
  });

  async function handleOk() {
    try {
      setModalProps({ confirmLoading: true });
      const values = await validate();
      if (unref(isUpdate)) {
        await updateKnowledgeBase(values);
      } else {
        await createKnowledgeBase(values);
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
