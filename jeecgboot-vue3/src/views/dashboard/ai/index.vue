<template>
  <div class="wrap">
    <div class="toolbar">
      <a-button type="primary" @click="goKnowledgeBase">知识库中心</a-button>
    </div>
    <div class="content">
      <AiChat></AiChat>
    </div>
  </div>
</template>

<script setup>
  import AiChat from '/@/views/super/airag/aiapp/chat/AiChat.vue';
  import { useRouter } from 'vue-router';

  const router = useRouter();

  function goKnowledgeBase() {
    router.push('/super/airag/knowledge-base/index');
  }
  
</script>

<style lang="less" scoped>
  .wrap {
    height: 100%;
    width: 100%;
    display: flex;
    flex-direction: column;
    gap: 12px;

    .toolbar {
      flex: 0 0 auto;
      padding: 12px 16px 0;
      text-align: right;
    }

    .content {
      flex: 1 1 auto;
      background: #fff;
      width: 100%;
      height: 100%;
    }
  }
</style>
