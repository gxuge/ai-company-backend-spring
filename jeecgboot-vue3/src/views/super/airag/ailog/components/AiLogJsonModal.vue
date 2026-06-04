<template>
  <BasicModal
    v-bind="$attrs"
    @register="registerModal"
    :canFullscreen="true"
    :footer="null"
    destroyOnClose
    :title="modalTitle"
    wrapClassName="ai-log-json-modal"
  >
    <div class="toolbar">
      <a-button size="small" type="primary" @click="toggleFormat">
        {{ isFormatted ? '收起' : '展开' }}
      </a-button>
    </div>
    <div class="json-wrapper">
      <pre>{{ displayContent }}</pre>
    </div>
  </BasicModal>
</template>

<script lang="ts" setup>
  import { computed, ref } from 'vue';
  import { BasicModal, useModalInner } from '/@/components/Modal';

  const modalTitle = ref('详情');
  const content = ref('');
  const isFormatted = ref(true);

  const displayContent = computed(() => {
    if (!isFormatted.value) {
      return normalizeEscapedText(content.value || '');
    }
    try {
      return normalizeEscapedText(JSON.stringify(JSON.parse(content.value), null, 2));
    } catch (e) {
      return normalizeEscapedText(content.value || '');
    }
  });

  const toggleFormat = () => {
    isFormatted.value = !isFormatted.value;
  };

  const [registerModal] = useModalInner(async (data) => {
    modalTitle.value = data?.title || '详情';
    content.value = data?.content || '';
    isFormatted.value = true;
  });

  function normalizeEscapedText(value: string) {
    if (!value) return '';
    return value
      .replace(/\\r\\n/g, '\n')
      .replace(/\\n/g, '\n')
      .replace(/\\t/g, '  ');
  }
</script>

<style scoped lang="less">
  .toolbar {
    display: flex;
    justify-content: flex-end;
    margin-bottom: 12px;
  }

  .json-wrapper {
    max-height: 70vh;
    overflow: auto;
    border: 1px solid #f0f0f0;
    border-radius: 8px;
    background: #fafafa;
    padding: 16px;

    pre {
      margin: 0;
      white-space: pre-wrap;
      word-break: break-all;
      font-family: Consolas, Monaco, 'Courier New', monospace;
      font-size: 13px;
      line-height: 1.6;
    }
  }
</style>
