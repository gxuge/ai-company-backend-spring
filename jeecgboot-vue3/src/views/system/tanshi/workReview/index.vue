<template>
  <div class="work-review-page">
    <a-tabs v-model:activeKey="activeTab" destroyInactiveTabPane>
      <a-tab-pane v-for="tab in reviewTabs" :key="tab.key" :tab="tab.title">
        <WorkReviewTable v-if="activeTab === tab.key" :work-type="tab.workType" :item-type="tab.itemType" :title="tab.title" />
      </a-tab-pane>
    </a-tabs>
  </div>
</template>

<script lang="ts" setup>
  import { ref } from 'vue';
  import type { ReviewItemType, WorkType } from './workReview.api';
  import WorkReviewTable from './components/WorkReviewTable.vue';

  defineOptions({ name: 'SystemTanshiWorkReview' });

  interface ReviewTab {
    key: string;
    title: string;
    workType: WorkType;
    itemType: ReviewItemType;
  }

  const activeTab = ref('role-content');
  const reviewTabs: ReviewTab[] = [
    { key: 'role-content', title: '角色内容审核', workType: 'ROLE', itemType: 'TEXT' },
    { key: 'role-image', title: '角色图片审核', workType: 'ROLE', itemType: 'IMAGE' },
    { key: 'story-content', title: '故事内容审核', workType: 'STORY', itemType: 'TEXT' },
    { key: 'story-image', title: '故事图片审核', workType: 'STORY', itemType: 'IMAGE' },
  ];
</script>

<style lang="less" scoped>
  .work-review-page {
    min-height: calc(100vh - 112px);
    padding: 16px;
    background: @component-background;
  }
</style>
