<template>
  <BasicTable @register="registerTable">
    <template #tableTitle>
      <a-button type="primary" preIcon="ant-design:plus-outlined" @click="handleCreate">新增</a-button>
      <a-dropdown v-if="selectedRowKeys.length > 0">
        <template #overlay>
          <a-menu>
            <a-menu-item key="1" @click="batchHandleDelete">
              <Icon icon="ant-design:delete-outlined"></Icon>
              删除
            </a-menu-item>
          </a-menu>
        </template>
        <a-button>
          批量操作
          <Icon icon="mdi:chevron-down"></Icon>
        </a-button>
      </a-dropdown>
    </template>
    <template #action="{ record }">
      <TableAction :actions="getTableAction(record)" :dropDownActions="getDropDownAction(record)" />
    </template>
  </BasicTable>

  <AiTagsDrawer @register="tagsDrawerRegister" />
  <AiPresetsModal @register="registerModal" @success="reload" />
</template>

<script lang="ts" name="airag-ai-presets" setup>
  import { BasicTable, TableAction } from '/@/components/Table';
  import { useListPage } from '/@/hooks/system/useListPage';
  import { useModal } from '/@/components/Modal';
  import { useDrawer } from '/@/components/Drawer';
  import { presetColumns, presetSearchFormSchema } from './AiPresets.data';
  import { listPresets, deletePreset, batchDeletePreset } from './AiPresets.api';
  import AiPresetsModal from './components/AiPresetsModal.vue';
  import AiTagsDrawer from './components/AiTagsDrawer.vue';

  const [registerModal, { openModal }] = useModal();
  const [tagsDrawerRegister, { openDrawer: openTagsDrawer }] = useDrawer();

  const { tableContext } = useListPage({
    tableProps: {
      title: 'AI 生成预设',
      api: listPresets,
      columns: presetColumns,
      canResize: true,
      formConfig: {
        schemas: presetSearchFormSchema,
        autoSubmitOnEnter: true,
        showAdvancedButton: false,
      },
      actionColumn: {
        width: 150,
      },
      rowSelection: {},
      defSort: {
        column: 'id',
        order: 'desc',
      },
    },
  });

  const [registerTable, { reload }, { selectedRowKeys }] = tableContext;

  function handleCreate() {
    openModal(true, {
      isUpdate: false,
      showFooter: true,
    });
  }

  function handleEdit(record: Recordable) {
    openModal(true, {
      record,
      isUpdate: true,
      showFooter: true,
    });
  }

  function handleDetail(record: Recordable) {
    openModal(true, {
      record,
      isUpdate: true,
      showFooter: false,
    });
  }

  async function handleDelete(record) {
    await deletePreset({ id: record.id }, reload);
  }

  async function batchHandleDelete() {
    await batchDeletePreset({ ids: selectedRowKeys.value }, reload);
  }

  function handleTags(record: Recordable) {
    openTagsDrawer(true, record);
  }

  function getTableAction(record) {
    return [
      {
        label: '标签',
        onClick: handleTags.bind(null, record),
      },
    ];
  }

  function getDropDownAction(record) {
    return [
      {
        label: '编辑',
        onClick: handleEdit.bind(null, record),
      },
      {
        label: '详情',
        onClick: handleDetail.bind(null, record),
      },
      {
        label: '删除',
        popConfirm: {
          title: '是否确认删除',
          confirm: handleDelete.bind(null, record),
        },
      },
    ];
  }
</script>

