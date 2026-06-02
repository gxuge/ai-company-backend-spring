<template>
  <BasicModal v-bind="$attrs" @register="registerModal" destroyOnClose :title="title" :maxHeight="500" :width="760" @ok="handleSubmit">
    <BasicForm @register="registerForm" name="AiTagsForm" />
  </BasicModal>
</template>

<script lang="ts" setup>
  import { computed, ref, unref } from 'vue';
  import { BasicModal, useModalInner } from '/@/components/Modal';
  import { BasicForm, useForm } from '/@/components/Form/index';
  import { presetTagFormSchema } from '../AiTags.data';
  import { addTagForPreset, listTagTypes } from '../AiPresets.api';

  const emit = defineEmits(['register', 'success']);
  const isUpdate = ref(true);
  const showFooter = ref(true);
  const currentPresetId = ref('');

  const [registerForm, { setProps, resetFields, setFieldsValue, updateSchema, validate, scrollToField }] = useForm({
    labelWidth: 120,
    schemas: presetTagFormSchema,
    showActionButtonGroup: false,
    baseColProps: { span: 24 },
    baseRowStyle: { padding: '0 16px' },
  });

  const [registerModal, { setModalProps, closeModal }] = useModalInner(async (data) => {
    await resetFields();
    setModalProps({ confirmLoading: false });
    const options = await initTagTypeOptions();
    isUpdate.value = !!data?.isUpdate;
    showFooter.value = !!data?.showFooter;
    currentPresetId.value = data?.presetId || data?.record?.presetId || '';
    if (unref(isUpdate)) {
      await setFieldsValue({
        tagType: data?.record?.typeId || data?.record?.tagType,
        ...data.record,
      });
    } else {
      await setFieldsValue({
        presetId: currentPresetId.value,
        tagType: data?.record?.typeId || data?.record?.tagType || options?.[0]?.value,
        required: 0,
        sortOrder: 0,
        ...data.record,
      });
    }
    setModalProps({ showCancelBtn: showFooter.value, showOkBtn: showFooter.value });
    setProps({ disabled: !showFooter.value });
  });

  const title = computed(() => (!unref(isUpdate) ? '新增标签' : !showFooter.value ? '标签关联详情' : '编辑标签关联'));

  async function initTagTypeOptions() {
    try {
      const res = await listTagTypes({ pageNo: 1, pageSize: 200 });
      const records = Array.isArray(res?.records) ? res.records : Array.isArray(res) ? res : [];
      const options = records
        .filter((item) => item && item.id && item.name)
        .map((item) => ({
          label: `${item.name}(${item.id})`,
          value: item.id,
        }));
      await updateSchema([
        {
          field: 'tagType',
          componentProps: {
            options,
            placeholder: '请选择标签类型',
          },
        },
      ]);
      return options;
    } catch (e) {
      console.error('加载标签类型失败', e);
      await updateSchema([
        {
          field: 'tagType',
          componentProps: {
            options: [],
            placeholder: '标签类型加载失败',
          },
        },
      ]);
      return [];
    }
  }

  async function handleSubmit() {
    try {
      const values = await validate();
      values.tagName = (values.tagName || '').trim();
      values.presetId = values.presetId || currentPresetId.value;
      values.typeId = values.tagType;
      delete values.tagType;
      setModalProps({ confirmLoading: true });
      await addTagForPreset(values);
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
