<template>
  <BasicModal
    v-bind="$attrs"
    @register="registerModal"
    title="广告内容预览"
    :width="520"
    :show-ok-btn="false"
    :show-cancel-btn="false"
    destroyOnClose
  >
    <div v-if="content" class="preview">
      <div class="preview-media">
        <a-image v-if="mediaType === 'IMAGE' && mediaUrl" :src="mediaUrl" :width="440" :height="250" :preview="{ src: mediaUrl }" />
        <video v-else-if="mediaType === 'VIDEO' && mediaUrl" class="preview-video" controls :src="mediaUrl" :poster="posterUrl || undefined" />
        <div v-else class="preview-card">
          <div v-if="cardPayload.badge" class="card-badge">{{ cardPayload.badge }}</div>
          <div class="card-title">{{ cardPayload.title || content.title }}</div>
          <div v-if="cardPayload.subtitle" class="card-subtitle">{{ cardPayload.subtitle }}</div>
          <div v-if="cardPayload.description" class="card-description">{{ cardPayload.description }}</div>
          <div v-if="cardPayload.buttonText" class="card-action">{{ cardPayload.buttonText }}</div>
          <pre v-if="!hasCardPreviewText" class="card-json">{{ content.payloadJson || '{}' }}</pre>
        </div>
      </div>

      <div class="preview-meta">
        <div class="meta-title">{{ content.title }}</div>
        <div v-if="content.subtitle" class="meta-subtitle">{{ content.subtitle }}</div>
        <div class="meta-line">
          <span>{{ getOptionLabel(mediaTypeOptions, mediaType) }}</span>
          <span>{{ getOptionLabel(sourceTypeOptions, sourceType) }}</span>
          <span>{{ getOptionLabel(cardTypeOptions, content.cardType) }}</span>
        </div>
        <div v-if="actionType !== 'NONE'" class="meta-action"> {{ getOptionLabel(linkTypeOptions, actionType) }}：{{ actionPayload }} </div>
      </div>
    </div>
  </BasicModal>
</template>

<script lang="ts" setup>
  import { computed, ref } from 'vue';
  import { BasicModal, useModalInner } from '/@/components/Modal';
  import { getFileAccessHttpUrl } from '/@/utils/common/compUtils';
  import type { AdContent } from '../adCenter.api';
  import { cardTypeOptions, getOptionLabel, linkTypeOptions, mediaTypeOptions, sourceTypeOptions } from '../adCenter.data';

  const content = ref<AdContent>();

  const sourceType = computed(() => content.value?.sourceType || 'SELF');
  const mediaType = computed(() => content.value?.mediaType || 'IMAGE');
  const actionType = computed(() => content.value?.actionType || content.value?.linkType || 'NONE');
  const actionPayload = computed(() => content.value?.actionPayload || content.value?.linkValue || '');

  const mediaUrl = computed(() => resolveUrl(content.value?.mediaUrl || content.value?.imageUrl, sourceType.value));
  const posterUrl = computed(() => resolveUrl(content.value?.posterUrl, sourceType.value));
  const cardPayload = computed<Record<string, string>>(() => {
    if (!content.value?.payloadJson) {
      return {};
    }
    try {
      const value = JSON.parse(content.value.payloadJson);
      return value && typeof value === 'object' && !Array.isArray(value) ? value : {};
    } catch {
      return {};
    }
  });
  const hasCardPreviewText = computed(() => ['title', 'subtitle', 'description', 'buttonText', 'badge'].some((key) => !!cardPayload.value[key]));

  const [registerModal] = useModalInner((data) => {
    content.value = data?.record;
  });

  function resolveUrl(value?: string, type?: string) {
    const url = value?.split(',')[0]?.trim();
    if (!url) {
      return '';
    }
    return type === 'EXTERNAL' || /^https?:\/\//i.test(url) ? url : getFileAccessHttpUrl(url);
  }
</script>

<style lang="less" scoped>
  .preview {
    padding: 8px;
  }

  .preview-media {
    display: flex;
    justify-content: center;
    min-height: 250px;
    padding: 16px;
    background: @background-color-light;
    border: 1px solid @border-color-base;
    border-radius: 4px;
  }

  .preview-video {
    display: block;
    width: 440px;
    max-width: 100%;
    max-height: 320px;
    background: #000;
  }

  .preview-card {
    width: 100%;
    padding: 24px;
    background: @component-background;
    border: 1px solid @border-color-base;
    border-radius: 4px;
  }

  .card-badge {
    display: inline-block;
    margin-bottom: 12px;
    padding: 2px 8px;
    color: @primary-color;
    background: fade(@primary-color, 10%);
    border-radius: 3px;
  }

  .card-title,
  .meta-title {
    font-size: 18px;
    font-weight: 600;
  }

  .card-subtitle,
  .meta-subtitle,
  .card-description {
    margin-top: 8px;
    color: @text-color-secondary;
    line-height: 1.6;
  }

  .card-action {
    display: inline-block;
    margin-top: 20px;
    padding: 6px 14px;
    color: #fff;
    background: @primary-color;
    border-radius: 4px;
  }

  .card-json {
    margin: 0;
    white-space: pre-wrap;
    word-break: break-word;
  }

  .preview-meta {
    padding-top: 16px;
  }

  .meta-line {
    display: flex;
    gap: 12px;
    margin-top: 10px;
    color: @text-color-secondary;
  }

  .meta-action {
    margin-top: 10px;
    word-break: break-all;
  }
</style>
