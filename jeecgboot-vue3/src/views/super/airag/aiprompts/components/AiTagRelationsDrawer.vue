<template>
  <BasicDrawer @register="registerBaseDrawer" title="标签关系" width="900" destroyOnClose>
    <BasicTable @register="registerTable" />
  </BasicDrawer>
</template>

<script lang="ts" setup>
  import { ref } from 'vue';
  import { BasicTable, useTable, BasicColumn } from '/@/components/Table';
  import { BasicDrawer, useDrawerInner } from '/@/components/Drawer';
  import { listTags, listTagRelations } from '../AiPresets.api';

  const currentTagId = ref('');
  const currentTagName = ref('');

  const relationColumns: BasicColumn[] = [
    {
      title: '当前标签',
      align: 'center',
      dataIndex: 'selfTagName',
    },
    {
      title: '关系类型',
      align: 'center',
      dataIndex: 'relationType',
    },
    {
      title: '关联标签',
      align: 'center',
      dataIndex: 'relatedTagName',
    },
  ];

  const [registerBaseDrawer] = useDrawerInner(async (data) => {
    currentTagId.value = data?.id || '';
    currentTagName.value = data?.name || '';
    reload();
  });

  async function loadRelations() {
    if (!currentTagId.value) {
      return {
        records: [],
        total: 0,
        size: 10,
        current: 1,
      };
    }

    const [sourceRes, targetRes, tagRes] = await Promise.all([
      listTagRelations({ sourceTagId: currentTagId.value, pageNo: 1, pageSize: 500 }),
      listTagRelations({ targetTagId: currentTagId.value, pageNo: 1, pageSize: 500 }),
      listTags({ pageNo: 1, pageSize: 2000 }),
    ]);

    const sourceRecords = Array.isArray(sourceRes?.records) ? sourceRes.records : [];
    const targetRecords = Array.isArray(targetRes?.records) ? targetRes.records : [];
    const tagRecords = Array.isArray(tagRes?.records) ? tagRes.records : [];

    const tagNameMap = new Map(tagRecords.map((item) => [item.id, item.name]));
    const relationMap = new Map<string, any>();

    sourceRecords.forEach((item) => {
      relationMap.set(item.id, {
        ...item,
        selfTagName: currentTagName.value || tagNameMap.get(currentTagId.value) || currentTagId.value,
        relatedTagName: tagNameMap.get(item.targetTagId) || item.targetTagId,
      });
    });
    targetRecords.forEach((item) => {
      relationMap.set(item.id, {
        ...item,
        selfTagName: currentTagName.value || tagNameMap.get(currentTagId.value) || currentTagId.value,
        relatedTagName: tagNameMap.get(item.sourceTagId) || item.sourceTagId,
      });
    });

    const records = Array.from(relationMap.values());
    return {
      records,
      total: records.length,
      size: 10,
      current: 1,
    };
  }

  const [registerTable, { reload }] = useTable({
    title: '关联关系列表',
    api: loadRelations,
    columns: relationColumns,
    striped: true,
    useSearchForm: false,
    showTableSetting: true,
    clickToRowSelect: false,
    bordered: true,
    showIndexColumn: false,
    rowKey: 'id',
  });
</script>
