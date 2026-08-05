<template>
  <div class="p-4">
    <BasicTable @register="registerTable">
      <template #provider="{ text }">
        <a-tag :color="text === 'STRIPE' ? 'blue' : 'cyan'">{{ formatProvider(text) }}</a-tag>
      </template>
      <template #amount="{ record }">
        {{ formatAmount(record.amount, record.currency) }}
      </template>
      <template #paymentStatus="{ text }">
        <a-tag :color="statusColorMap[text] || 'default'">{{ formatPaymentStatus(text) }}</a-tag>
      </template>
      <template #action="{ record }">
        <TableAction :actions="getActions(record)" />
      </template>
    </BasicTable>
    <PaymentDetailDrawer @register="registerDrawer" />
  </div>
</template>

<script lang="ts" setup>
  import { BasicTable, TableAction } from '/@/components/Table';
  import { useDrawer } from '/@/components/Drawer';
  import { useListPage } from '/@/hooks/system/useListPage';
  import { columns, paymentProviderOptions, paymentStatusOptions, searchFormSchema } from './payment.data';
  import { pagePayments } from './payment.api';
  import PaymentDetailDrawer from './components/PaymentDetailDrawer.vue';

  defineOptions({ name: 'SystemTanshiPayment' });

  const statusColorMap = {
    CREATING: 'processing',
    PENDING: 'warning',
    SUCCEEDED: 'success',
    FAILED: 'error',
    CANCELED: 'default',
  };

  const [registerDrawer, { openDrawer }] = useDrawer();
  const { tableContext } = useListPage({
    designScope: 'tanshi-payment',
    tableProps: {
      title: '支付流水管理',
      api: pagePayments,
      columns,
      formConfig: {
        labelWidth: 90,
        schemas: searchFormSchema,
      },
      actionColumn: {
        width: 90,
        title: '操作',
        dataIndex: 'action',
        slots: { customRender: 'action' },
      },
      showIndexColumn: true,
    },
  });
  const [registerTable] = tableContext;

  function getActions(record) {
    return [{ label: '详情', onClick: () => openDrawer(true, { id: record.id }) }];
  }

  function formatProvider(value) {
    return paymentProviderOptions.find((item) => item.value === value)?.label || value || '-';
  }

  function formatPaymentStatus(value) {
    return paymentStatusOptions.find((item) => item.value === value)?.label || value || '-';
  }

  function formatAmount(amount, currency) {
    if (amount === null || amount === undefined) {
      return '-';
    }
    return `${amount} ${currency || ''}`.trim();
  }
</script>
