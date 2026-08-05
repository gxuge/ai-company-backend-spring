<template>
  <BasicModal @register="registerModal" :title="formState.id ? '编辑用户会员' : '手动开通会员'" :width="680" @ok="handleSubmit">
    <a-form ref="formRef" :model="formState" :rules="rules" :label-col="{ span: 6 }" :wrapper-col="{ span: 17 }">
      <a-form-item label="用户ID" name="userId"><a-input v-model:value="formState.userId" :disabled="!!formState.id" /></a-form-item>
      <a-form-item label="会员等级" name="planId">
        <a-select v-model:value="formState.planId" :options="planOptions" @change="handlePlanChange" />
      </a-form-item>
      <a-form-item label="会员套餐" name="productId">
        <a-select v-model:value="formState.productId" :options="productOptions" />
      </a-form-item>
      <a-form-item label="生效时间" name="startTime"><a-date-picker v-model:value="formState.startTime" show-time /></a-form-item>
      <a-form-item label="到期时间" name="endTime"><a-date-picker v-model:value="formState.endTime" show-time /></a-form-item>
      <a-form-item label="会员状态"><a-switch v-model:checked="statusChecked" checked-children="有效" un-checked-children="失效" /></a-form-item>
      <a-form-item label="自动续费"><a-switch v-model:checked="renewChecked" /></a-form-item>
    </a-form>
  </BasicModal>
</template>

<script lang="ts" setup>
  import { computed, reactive, ref } from 'vue';
  import dayjs from 'dayjs';
  import { BasicModal, useModalInner } from '/@/components/Modal';
  import { getMemberConfig, saveMembership } from '../userMembership.api';

  const emit = defineEmits(['register', 'success']);
  const formRef = ref();
  const formState = reactive<any>({});
  const config = reactive<any>({ plans: [], products: [] });
  const planOptions = computed(() => config.plans.map((item) => ({ label: `${item.name}（${item.code}）`, value: item.id })));
  const productOptions = computed(() =>
    config.products
      .filter((item) => item.planId === formState.planId)
      .map((item) => ({ label: `${cycleText(item.cycleType)} / ¥${item.price}`, value: item.id }))
  );
  const statusChecked = computed({ get: () => formState.status === 1, set: (value) => (formState.status = value ? 1 : 0) });
  const renewChecked = computed({ get: () => formState.autoRenew === 1, set: (value) => (formState.autoRenew = value ? 1 : 0) });
  const rules = {
    userId: [{ required: true, message: '请输入用户ID' }],
    planId: [{ required: true, message: '请选择会员等级' }],
    productId: [{ required: true, message: '请选择会员套餐' }],
    startTime: [{ required: true, message: '请选择生效时间' }],
    endTime: [{ required: true, message: '请选择到期时间' }],
  };
  const [registerModal, { closeModal, setModalProps }] = useModalInner(async (data) => {
    Object.assign(config, await getMemberConfig());
    Object.keys(formState).forEach((key) => delete formState[key]);
    const record = data.record || {};
    Object.assign(formState, {
      status: 1,
      autoRenew: 0,
      startTime: dayjs(),
      endTime: dayjs().add(1, 'month'),
      ...record,
      startTime: record.startTime ? dayjs(record.startTime) : dayjs(),
      endTime: record.endTime ? dayjs(record.endTime) : dayjs().add(1, 'month'),
    });
  });
  function handlePlanChange() {
    if (!config.products.some((item) => item.id === formState.productId && item.planId === formState.planId)) formState.productId = undefined;
  }
  function cycleText(value) {
    return { WEEK: '周卡', MONTH: '月卡', QUARTER: '季卡', YEAR: '年卡' }[value] || value;
  }
  async function handleSubmit() {
    await formRef.value?.validate();
    setModalProps({ confirmLoading: true });
    try {
      await saveMembership({
        ...formState,
        startTime: formState.startTime?.format('YYYY-MM-DD HH:mm:ss'),
        endTime: formState.endTime?.format('YYYY-MM-DD HH:mm:ss'),
      });
      closeModal();
      emit('success');
    } finally {
      setModalProps({ confirmLoading: false });
    }
  }
</script>

<style scoped>
  :deep(.ant-picker) {
    width: 100%;
  }
</style>
