<template>
  <BasicModal v-bind="$attrs" @register="registerModal" destroyOnClose :title="title" :maxHeight="500" :width="760" @ok="handleSubmit">
    <BasicForm @register="registerForm" name="AiPresetsForm" />
  </BasicModal>
</template>

<script lang="ts" setup>
  import { computed, ref, unref } from 'vue';
  import { BasicModal, useModalInner } from '/@/components/Modal';
  import { BasicForm, useForm } from '/@/components/Form/index';
  import { presetFormSchema } from '../AiPresets.data';
  import { saveOrUpdatePreset } from '../AiPresets.api';

  const emit = defineEmits(['register', 'success']);
  const isUpdate = ref(true);
  const showFooter = ref(true);

  const [registerForm, { setProps, resetFields, setFieldsValue, validate, scrollToField }] = useForm({
    labelWidth: 120,
    schemas: presetFormSchema,
    showActionButtonGroup: false,
    baseColProps: { span: 24 },
    baseRowStyle: { padding: '0 16px' },
  });

  const [registerModal, { setModalProps, closeModal }] = useModalInner(async (data) => {
    await resetFields();
    setModalProps({ confirmLoading: false });
    isUpdate.value = !!data?.isUpdate;
    showFooter.value = !!data?.showFooter;
    if (unref(isUpdate)) {
      await setFieldsValue({
        ...data.record,
      });
    }
    setModalProps({ showCancelBtn: showFooter.value, showOkBtn: showFooter.value });
    setProps({ disabled: !showFooter.value });
  });

  const title = computed(() => (!unref(isUpdate) ? '新增预设' : !showFooter.value ? '预设详情' : '编辑预设'));

  async function handleSubmit() {
    try {
      const values = await validate();
      setModalProps({ confirmLoading: true });
      await saveOrUpdatePreset(values, isUpdate.value);
      closeModal();
      emit('success');
    } catch ({ errorFields }) {
      if (errorFields && errorFields.length > 0) {
        const firstField = errorFields[0];
        if (firstField) {
          scrollToField(firstField.name, { behavior: 'smooth', block: 'center' });
        }
      }
      return Promise.reject(errorFields);
    } finally {
      setModalProps({ confirmLoading: false });
    }
  }
</script>

