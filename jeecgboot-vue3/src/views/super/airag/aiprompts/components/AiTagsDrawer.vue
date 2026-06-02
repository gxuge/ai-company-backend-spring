<template>
  <BasicDrawer @register="registerBaseDrawer" title="预设标签关联" width="900" destroyOnClose>
    <BasicTable @register="registerTable">
      <template #tableTitle>
        <a-button type="primary" @click="handleCreate">新增标签</a-button>
      </template>
      <template #action="{ record }">
        <TableAction :actions="getTableAction(record)" />
      </template>
    </BasicTable>
    <AiTagsModal @register="registerModal" @success="handleSuccess" />
    <AiTagRelationsDrawer @register="registerRelationsDrawer" />
  </BasicDrawer>
</template>

<script lang="ts" setup>
  import { ref } from 'vue';
  import { BasicTable, TableAction, useTable } from '/@/components/Table';
  import { BasicDrawer, useDrawer, useDrawerInner } from '/@/components/Drawer';
  import { useModal } from '/@/components/Modal';
  import { presetTagColumns, presetTagSearchFormSchema } from '../AiTags.data';
  import { listTags, listTagTypes } from '../AiPresets.api';
  import AiTagsModal from './AiTagsModal.vue';
  import AiTagRelationsDrawer from './AiTagRelationsDrawer.vue';

  const presetId = ref('');
  const [registerModal, { openModal }] = useModal();
  const [registerRelationsDrawer, { openDrawer: openRelationsDrawer }] = useDrawer();

  const [registerBaseDrawer] = useDrawerInner(async (data) => {
    presetId.value = data.id;
    setProps({ searchInfo: { presetId: data.id } });
    reload();
  });

  async function listTagsWithTypeName(params) {
    const [tagRes, typeRes] = await Promise.all([
      listTags(params),
      listTagTypes({ pageNo: 1, pageSize: 500 }),
    ]);

    const typeRecords = Array.isArray(typeRes?.records) ? typeRes.records : Array.isArray(typeRes) ? typeRes : [];
    const typeMap = new Map(typeRecords.map((item) => [item.id, item.name]));

    if (Array.isArray(tagRes?.records)) {
      tagRes.records = tagRes.records.map((item) => ({
        ...item,
        typeName: typeMap.get(item.typeId) ? `${item.typeId}（${typeMap.get(item.typeId)}）` : item.typeId,
      }));
    }
    return tagRes;
  }

  const [registerTable, { reload, setProps }] = useTable({
    title: '标签关联列表',
    api: listTagsWithTypeName,
    columns: presetTagColumns,
    formConfig: {
      labelWidth: 70,
      schemas: presetTagSearchFormSchema,
      autoSubmitOnEnter: true,
    },
    striped: true,
    useSearchForm: true,
    showTableSetting: true,
    clickToRowSelect: false,
    bordered: true,
    showIndexColumn: false,
    rowKey: 'id',
    actionColumn: {
      width: 120,
      title: '操作',
    },
  });

  function handleCreate() {
    openModal(true, {
      isUpdate: false,
      showFooter: true,
      presetId: presetId.value,
    });
  }

  function handleSuccess() {
    reload();
  }

  function handleRelations(record) {
    openRelationsDrawer(true, record);
  }

  function getTableAction(record) {
    return [
      {
        label: '标签关系',
        onClick: handleRelations.bind(null, record),
      },
    ];
  }
</script>
