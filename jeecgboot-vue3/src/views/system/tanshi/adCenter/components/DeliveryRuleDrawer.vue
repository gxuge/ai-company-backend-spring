<template>
  <BasicDrawer v-bind="$attrs" @register="registerDrawer" :title="drawerTitle" :width="680" destroyOnClose showFooter @ok="handleSubmit">
    <a-alert
      class="rule-alert"
      type="info"
      show-icon
      message="平台、登录状态、会员等级和指定用户会同时参与匹配；全部平台或全部等级不能与具体选项混选。"
    />
    <BasicForm @register="registerForm" />
  </BasicDrawer>
</template>

<script lang="ts" setup>
  import { computed, ref } from 'vue';
  import { BasicDrawer, useDrawerInner } from '/@/components/Drawer';
  import { BasicForm, useForm } from '/@/components/Form';
  import { useMessage } from '/@/hooks/web/useMessage';
  import { getAdDeliveryRule, saveAdDeliveryRule } from '../adCenter.api';
  import { ruleFormSchema } from '../adCenter.data';

  const emit = defineEmits(['register', 'success']);
  const { createMessage } = useMessage();
  const contentTitle = ref('');

  const [registerForm, { resetFields, setFieldsValue, validate }] = useForm({
    schemas: ruleFormSchema,
    showActionButtonGroup: false,
    labelWidth: 100,
  });

  const [registerDrawer, { setDrawerProps, closeDrawer }] = useDrawerInner(async (data) => {
    await resetFields();
    contentTitle.value = data?.record?.title || '';
    if (!data?.record?.id) {
      return;
    }
    setDrawerProps({ loading: true, confirmLoading: false });
    try {
      const rule = await getAdDeliveryRule({ contentId: data.record.id });
      await setFieldsValue({
        contentId: data.record.id,
        platforms: rule?.platforms?.length ? rule.platforms : ['ALL'],
        audienceType: rule?.audienceType || 'ALL',
        memberLevels: rule?.memberLevels?.length ? rule.memberLevels : ['ALL'],
        userIdsText: (rule?.userIds || []).join('\n'),
      });
    } finally {
      setDrawerProps({ loading: false });
    }
  });

  const drawerTitle = computed(() => (contentTitle.value ? `投放规则 - ${contentTitle.value}` : '投放规则'));

  async function handleSubmit() {
    const values = await validate();
    if (hasMixedAll(values.platforms) || hasMixedAll(values.memberLevels)) {
      createMessage.warning('全部选项不能与具体选项同时选择');
      return;
    }
    const userIds = String(values.userIdsText || '')
      .split(/[\s,，]+/)
      .map((item) => item.trim())
      .filter(Boolean);
    setDrawerProps({ confirmLoading: true });
    try {
      await saveAdDeliveryRule({
        contentId: values.contentId,
        platforms: values.platforms,
        audienceType: values.audienceType,
        memberLevels: values.memberLevels,
        userIds: Array.from(new Set(userIds)),
      });
      closeDrawer();
      emit('success');
    } finally {
      setDrawerProps({ confirmLoading: false });
    }
  }

  function hasMixedAll(values?: string[]) {
    return !!values?.includes('ALL') && values.length > 1;
  }
</script>

<style lang="less" scoped>
  .rule-alert {
    margin-bottom: 16px;
  }
</style>
