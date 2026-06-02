<template>
  <BasicModal v-bind="$attrs" @register="registerModal" title="已有标签" width="1000px" @ok="handleSubmit" destroyOnClose @openChange="handleOpenChange">
    <BasicTable @register="registerTable" :rowSelection="rowSelection" />
  </BasicModal>
</template>

<script lang="ts" setup>
  import { ref, unref, toRaw } from 'vue';
  import { BasicModal, useModalInner } from '/src/components/Modal';
  import { BasicTable, useTable } from '/src/components/Table';
  import { listTags } from '../AiPresets.api';

  const emit = defineEmits(['select', 'register']);
  const checkedKeys = ref<Array<string | number>>([]);
  const checkedRows = ref<any[]>([]);
  const [registerModal, { setModalProps, closeModal }] = useModalInner();

  const [registerTable] = useTable({
    api: listTags,
    rowKey: 'id',
    columns: [
      { title: '标签名', dataIndex: 'name', align: 'center' },
      { title: '作用域', dataIndex: 'scope', align: 'center' },
      { title: '类型', dataIndex: 'typeId', align: 'center' },
    ],
    formConfig: {
      labelWidth: 70,
      schemas: [
        {
          label: '标签名',
          field: 'name',
          component: 'Input',
          colProps: { span: 8 },
        },
      ],
      autoSubmitOnEnter: true,
    },
    striped: true,
    useSearchForm: true,
    showTableSetting: false,
    bordered: true,
    showIndexColumn: false,
    canResize: false,
  });

  const rowSelection = {
    type: 'radio',
    columnWidth: 50,
    selectedRowKeys: checkedKeys,
    onChange: onSelectChange,
  };

  function onSelectChange(selectedRowKeys: (string | number)[], rows: any[]) {
    checkedKeys.value = selectedRowKeys;
    checkedRows.value = rows;
  }

  function handleOpenChange(visible) {
    if (visible) {
      checkedKeys.value = [];
      checkedRows.value = [];
    }
  }

  function handleSubmit() {
    if (!checkedRows.value || checkedRows.value.length === 0) {
      return;
    }
    setModalProps({ confirmLoading: true });
    closeModal();
    emit('select', toRaw(unref(checkedRows))[0]);
    setModalProps({ confirmLoading: false });
  }
</script>
