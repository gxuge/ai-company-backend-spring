<template>
  <BasicDrawer v-bind="$attrs" @register="registerDrawer" title="AI调用链详情" width="70%" destroyOnClose>
    <a-spin :spinning="loading">
      <a-descriptions bordered size="small" :column="2" class="summary">
        <a-descriptions-item label="接口路径">{{ getLogValue('endpoint') || '-' }}</a-descriptions-item>
        <a-descriptions-item label="场景">{{ getLogValue('bizScene', 'biz_scene') || '-' }}</a-descriptions-item>
        <a-descriptions-item label="供应商/模型">{{ modelText }}</a-descriptions-item>
        <a-descriptions-item label="状态">
          <a-tag :color="getLogValue('status') === 'success' ? 'green' : getLogValue('status') === 'failed' ? 'red' : 'blue'">
            {{ getLogValue('status') || '-' }}
          </a-tag>
        </a-descriptions-item>
        <a-descriptions-item label="模板">{{ promptText }}</a-descriptions-item>
        <a-descriptions-item label="修复模板">{{ repairPromptText }}</a-descriptions-item>
        <a-descriptions-item label="Trace ID">{{ getLogValue('traceId', 'trace_id') || '-' }}</a-descriptions-item>
        <a-descriptions-item label="耗时(ms)">{{ getLogValue('costMs', 'cost_ms') ?? '-' }}</a-descriptions-item>
        <a-descriptions-item label="请求参数" :span="2">
          <a-button v-if="getLogValue('requestParams', 'request_params')" type="link" @click="openContent('request_params', getLogValue('requestParams', 'request_params'))">
            查看请求参数
          </a-button>
          <span v-else>-</span>
        </a-descriptions-item>
        <a-descriptions-item label="错误信息" :span="2">{{ getLogValue('errorMessage', 'error_message') || '-' }}</a-descriptions-item>
      </a-descriptions>

      <div class="section">
        <div class="section-title">调用链步骤</div>
        <div v-if="!steps.length" class="empty-block">暂无步骤数据</div>
        <div v-for="step in steps" :key="step.id" class="step-card">
          <div class="step-header">
            <div class="left">
              <a-tag color="blue">#{{ getStepValue(step, 'stepNo', 'step_no') }}</a-tag>
              <span class="name">{{ getStepValue(step, 'stepName', 'step_name') || getStepValue(step, 'stepType', 'step_type') }}</span>
              <a-tag :color="getStepValue(step, 'status') === 'success' ? 'green' : 'red'">{{ getStepValue(step, 'status') }}</a-tag>
            </div>
            <div class="right">
              <a-button v-if="getStepValue(step, 'developerPrompt', 'developer_prompt')" size="small" @click="openContent('developer_prompt', getStepValue(step, 'developerPrompt', 'developer_prompt'))">Developer</a-button>
              <a-button v-if="getStepValue(step, 'userPrompt', 'user_prompt')" size="small" @click="openContent('user_prompt', getStepValue(step, 'userPrompt', 'user_prompt'))">User</a-button>
              <a-button v-if="getStepValue(step, 'toolSchema', 'tool_schema')" size="small" @click="openContent('tool_schema', getStepValue(step, 'toolSchema', 'tool_schema'))">Schema</a-button>
              <a-button v-if="getStepValue(step, 'renderedPrompt', 'rendered_prompt')" size="small" @click="openContent('rendered_prompt', getStepValue(step, 'renderedPrompt', 'rendered_prompt'))">Rendered</a-button>
              <a-button v-if="getStepValue(step, 'requestPayloadJson', 'request_payload_json')" size="small" @click="openContent('request_payload', getStepValue(step, 'requestPayloadJson', 'request_payload_json'))">Request</a-button>
              <a-button v-if="getStepValue(step, 'responseRaw', 'response_raw')" size="small" @click="openContent('response_raw', getStepValue(step, 'responseRaw', 'response_raw'))">Raw</a-button>
              <a-button v-if="getStepValue(step, 'responseJson', 'response_json')" size="small" @click="openContent('response_json', getStepValue(step, 'responseJson', 'response_json'))">JSON</a-button>
              <a-button v-if="getStepValue(step, 'extraInfoJson', 'extra_info_json')" size="small" @click="openContent('extra_info', getStepValue(step, 'extraInfoJson', 'extra_info_json'))">Extra</a-button>
              <a-button v-if="getStepValue(step, 'finalOutputJson', 'final_output_json')" size="small" @click="openContent('final_output', getStepValue(step, 'finalOutputJson', 'final_output_json'))">Final</a-button>
            </div>
          </div>
          <div class="step-meta">
            <span>类型：{{ getStepValue(step, 'stepType', 'step_type') || '-' }}</span>
            <span>模板：{{ joinPrompt(getStepValue(step, 'promptCode', 'prompt_code'), getStepValue(step, 'promptVersion', 'prompt_version')) }}</span>
            <span>模型：{{ joinModel(getStepValue(step, 'provider'), getStepValue(step, 'modelName', 'model_name')) }}</span>
          </div>
          <div v-if="getStepValue(step, 'validationIssues', 'validation_issues')" class="step-issues">
            {{ getStepValue(step, 'validationIssues', 'validation_issues') }}
          </div>
          <div class="step-preview">
            <div v-if="getStepValue(step, 'requestPayloadJson', 'request_payload_json')" class="preview-line">
              <span class="preview-label">Request:</span>
              <span class="preview-value">{{ truncate(getStepValue(step, 'requestPayloadJson', 'request_payload_json')) }}</span>
            </div>
            <div v-if="getStepValue(step, 'finalOutputJson', 'final_output_json')" class="preview-line">
              <span class="preview-label">Final:</span>
              <span class="preview-value">{{ truncate(getStepValue(step, 'finalOutputJson', 'final_output_json')) }}</span>
            </div>
            <div v-if="getStepValue(step, 'extraInfoJson', 'extra_info_json')" class="preview-line">
              <span class="preview-label">Extra:</span>
              <span class="preview-value">{{ truncate(getStepValue(step, 'extraInfoJson', 'extra_info_json')) }}</span>
            </div>
          </div>
        </div>
      </div>

      <div class="section">
        <div class="section-title">最终结果</div>
        <a-button v-if="getLogValue('finalResultJson', 'final_result_json')" type="link" @click="openContent('final_result_json', getLogValue('finalResultJson', 'final_result_json'))">
          查看最终结果JSON
        </a-button>
      </div>
    </a-spin>
    <AiLogJsonModal @register="registerJsonModal" />
  </BasicDrawer>
</template>

<script lang="ts" setup>
  import { computed, ref } from 'vue';
  import { BasicDrawer, useDrawerInner } from '/@/components/Drawer';
  import { useModal } from '/@/components/Modal';
  import { getAiLogDetail } from '../ailog.api';
  import AiLogJsonModal from './AiLogJsonModal.vue';

  const loading = ref(false);
  const detail = ref<any>(null);
  const [registerJsonModal, { openModal }] = useModal();

  const steps = computed(() => detail.value?.steps || []);
  const modelText = computed(() => joinModel(getLogValue('provider'), getLogValue('modelName', 'model_name')));
  const promptText = computed(() => joinPrompt(getLogValue('promptCode', 'prompt_code'), getLogValue('promptVersion', 'prompt_version')));
  const repairPromptText = computed(() => joinPrompt(getLogValue('repairPromptCode', 'repair_prompt_code'), getLogValue('repairPromptVersion', 'repair_prompt_version')));

  const [registerDrawer] = useDrawerInner(async (data) => {
    loading.value = true;
    detail.value = null;
    try {
      const res = await getAiLogDetail({ id: data?.id });
      if (res?.success) {
        detail.value = res.result;
      }
    } finally {
      loading.value = false;
    }
  });

  function openContent(title: string, content: string) {
    openModal(true, { title, content });
  }

  function joinPrompt(code?: string, version?: string) {
    if (!code && !version) return '-';
    return `${code || '-'}@${version || '-'}`;
  }

  function joinModel(provider?: string, modelName?: string) {
    if (!provider && !modelName) return '-';
    return `${provider || '-'} / ${modelName || '-'}`;
  }

  function getLogValue(...keys: string[]) {
    return getValue(detail.value?.log, keys);
  }

  function getStepValue(step: any, ...keys: string[]) {
    return getValue(step, keys);
  }

  function getValue(target: any, keys: string[]) {
    if (!target || !keys?.length) return undefined;
    for (const key of keys) {
      if (target[key] !== undefined && target[key] !== null && target[key] !== '') {
        return target[key];
      }
    }
    return target[keys[0]];
  }

  function truncate(value?: string, maxLength = 180) {
    if (!value) return '';
    return value.length > maxLength ? `${value.slice(0, maxLength)}...` : value;
  }
</script>

<style scoped lang="less">
  .summary {
    margin-bottom: 16px;
  }

  .section {
    margin-top: 16px;
    padding: 16px;
    background: #fff;
    border: 1px solid #f0f0f0;
    border-radius: 8px;
  }

  .section-title {
    font-size: 14px;
    font-weight: 600;
    margin-bottom: 12px;
    color: #1f2329;
  }

  .empty-block {
    color: #999;
  }

  .step-card {
    border: 1px solid #f0f0f0;
    border-radius: 8px;
    padding: 12px;
    margin-bottom: 12px;
    background: #fafafa;
  }

  .step-header {
    display: flex;
    justify-content: space-between;
    gap: 12px;
  }

  .left {
    display: flex;
    align-items: center;
    gap: 8px;
    flex-wrap: wrap;
  }

  .right {
    display: flex;
    gap: 8px;
    flex-wrap: wrap;
    justify-content: flex-end;
  }

  .name {
    font-weight: 600;
    color: #1f2329;
  }

  .step-meta {
    display: flex;
    gap: 16px;
    flex-wrap: wrap;
    margin-top: 10px;
    color: #666;
    font-size: 12px;
  }

  .step-issues {
    margin-top: 10px;
    color: #cf1322;
    white-space: pre-wrap;
    word-break: break-all;
    font-size: 12px;
  }

  .step-preview {
    margin-top: 10px;
    display: flex;
    flex-direction: column;
    gap: 6px;
  }

  .preview-line {
    display: flex;
    gap: 8px;
    font-size: 12px;
    color: #666;
  }

  .preview-label {
    flex-shrink: 0;
    color: #1f2329;
    font-weight: 500;
  }

  .preview-value {
    word-break: break-all;
    white-space: pre-wrap;
  }
</style>
