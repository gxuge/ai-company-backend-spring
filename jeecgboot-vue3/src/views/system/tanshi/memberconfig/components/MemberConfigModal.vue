<template>
  <BasicModal @register="registerModal" :title="title" :width="620" @ok="handleSubmit">
    <a-form ref="formRef" :model="formState" :rules="rules" :label-col="{ span: 6 }" :wrapper-col="{ span: 17 }">
      <template v-if="resourceType === 'plan'">
        <a-form-item label="会员名称" name="name"><a-input v-model:value="formState.name" /></a-form-item>
        <a-form-item label="会员编码" name="code"><a-input v-model:value="formState.code" :disabled="!!formState.id" /></a-form-item>
        <a-form-item label="会员说明"><a-textarea v-model:value="formState.description" :rows="3" /></a-form-item>
        <a-form-item label="主题颜色"><a-input v-model:value="formState.themeColor" type="color" /></a-form-item>
        <a-form-item label="状态"><a-switch v-model:checked="statusChecked" /></a-form-item>
        <a-form-item label="排序"><a-input-number v-model:value="formState.sort" :min="0" /></a-form-item>
      </template>
      <template v-else-if="resourceType === 'product'">
        <a-form-item label="套餐周期" name="cycleType">
          <a-select v-model:value="formState.cycleType" :options="cycleOptions" />
        </a-form-item>
        <a-form-item label="售价" name="price"><a-input-number v-model:value="formState.price" :min="0" :precision="2" /></a-form-item>
        <a-form-item label="原价"><a-input-number v-model:value="formState.originalPrice" :min="0" :precision="2" /></a-form-item>
        <a-form-item label="优惠文案"><a-input v-model:value="formState.discountText" /></a-form-item>
        <a-form-item label="推荐套餐"><a-switch v-model:checked="recommendChecked" /></a-form-item>
        <a-form-item label="状态"><a-switch v-model:checked="statusChecked" /></a-form-item>
      </template>
      <template v-else-if="resourceType === 'benefit'">
        <a-form-item label="权益编码" name="code"><a-input v-model:value="formState.code" :disabled="!!formState.id" /></a-form-item>
        <a-form-item label="权益名称" name="name"><a-input v-model:value="formState.name" /></a-form-item>
        <a-form-item label="分类"><a-input v-model:value="formState.category" /></a-form-item>
        <a-form-item label="图标"><a-input v-model:value="formState.icon" /></a-form-item>
        <a-form-item label="说明"><a-textarea v-model:value="formState.description" :rows="3" /></a-form-item>
        <a-form-item label="排序"><a-input-number v-model:value="formState.sort" :min="0" /></a-form-item>
      </template>
      <template v-else-if="resourceType === 'planBenefit'">
        <a-form-item label="权益" name="benefitId">
          <a-select v-model:value="formState.benefitId" :options="benefitOptions" />
        </a-form-item>
        <a-form-item label="权益值" name="value"><a-input v-model:value="formState.value" /></a-form-item>
        <a-form-item label="单位"><a-input v-model:value="formState.unit" /></a-form-item>
        <a-form-item label="限制类型" name="limitType">
          <a-select v-model:value="formState.limitType" :options="limitOptions" />
        </a-form-item>
      </template>
      <template v-else>
        <a-form-item label="赠礼名称" name="name"><a-input v-model:value="formState.name" /></a-form-item>
        <a-form-item label="图标"><a-input v-model:value="formState.icon" /></a-form-item>
        <a-form-item label="说明"><a-textarea v-model:value="formState.description" :rows="3" /></a-form-item>
        <a-form-item label="排序"><a-input-number v-model:value="formState.sort" :min="0" /></a-form-item>
      </template>
    </a-form>
  </BasicModal>
</template>

<script lang="ts" setup>
  import { computed, reactive, ref } from 'vue';
  import { BasicModal, useModalInner } from '/@/components/Modal';
  import { saveMemberConfig } from '../memberConfig.api';

  const emit = defineEmits(['register', 'success']);
  const formRef = ref();
  const formState = reactive<any>({});
  const resourceType = ref('plan');
  const planId = ref<number>();
  const benefitOptions = ref<any[]>([]);
  const statusChecked = computed({
    get: () => formState.status !== 0,
    set: (value) => (formState.status = value ? 1 : 0),
  });
  const recommendChecked = computed({
    get: () => formState.recommend === 1,
    set: (value) => (formState.recommend = value ? 1 : 0),
  });
  const titleMap = { plan: '会员等级', product: '会员套餐', benefit: '权益', planBenefit: '等级权益', gift: '开通赠礼' };
  const title = computed(() => `${formState.id ? '编辑' : '新增'}${titleMap[resourceType.value]}`);
  const cycleOptions = [
    { label: '周卡', value: 'WEEK' },
    { label: '月卡', value: 'MONTH' },
    { label: '季卡', value: 'QUARTER' },
    { label: '年卡', value: 'YEAR' },
  ];
  const limitOptions = [
    { label: '启用型', value: 'ENABLE' },
    { label: '额度型', value: 'LIMIT' },
    { label: '按月额度', value: 'MONTH' },
  ];
  const rules = {
    name: [{ required: true, message: '请输入名称' }],
    code: [{ required: true, message: '请输入编码' }],
    cycleType: [{ required: true, message: '请选择周期' }],
    price: [{ required: true, message: '请输入售价' }],
    benefitId: [{ required: true, message: '请选择权益' }],
    value: [{ required: true, message: '请输入权益值' }],
    limitType: [{ required: true, message: '请选择限制类型' }],
  };

  const [registerModal, { closeModal, setModalProps }] = useModalInner(async (data) => {
    resourceType.value = data.resourceType;
    planId.value = data.planId;
    benefitOptions.value = (data.benefits || []).map((item) => ({ label: `${item.name}（${item.code}）`, value: item.id }));
    Object.keys(formState).forEach((key) => delete formState[key]);
    Object.assign(formState, { status: 1, recommend: 0, sort: 0, planId: data.planId, ...(data.record || {}) });
    setModalProps({ confirmLoading: false });
  });

  async function handleSubmit() {
    await formRef.value?.validate();
    setModalProps({ confirmLoading: true });
    try {
      await saveMemberConfig({ resourceType: resourceType.value, data: { ...formState, planId: planId.value ?? formState.planId } });
      closeModal();
      emit('success');
    } finally {
      setModalProps({ confirmLoading: false });
    }
  }
</script>

<style scoped>
  :deep(.ant-input-number) {
    width: 100%;
  }
</style>
