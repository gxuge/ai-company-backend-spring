<template>
  <BasicDrawer v-bind="$attrs" @register="registerDrawer" title="ETL 执行详情" :width="820">
    <a-spin :spinning="loading">
      <a-descriptions v-if="detail.id" :column="2" bordered size="small">
        <a-descriptions-item label="执行记录ID" :span="2">{{ detail.id }}</a-descriptions-item>
        <a-descriptions-item label="任务">{{ detail.taskName || '-' }}</a-descriptions-item>
        <a-descriptions-item label="任务ID">{{ detail.taskId || '-' }}</a-descriptions-item>
        <a-descriptions-item label="推荐类型">{{ optionLabel(recommendTypeOptions, detail.recommendType) }}</a-descriptions-item>
        <a-descriptions-item label="触发方式">{{ optionLabel(triggerTypeOptions, detail.triggerType) }}</a-descriptions-item>
        <a-descriptions-item label="执行状态">
          <a-tag :color="statusColorMap[detail.status || ''] || 'default'">
            {{ optionLabel(executionStatusOptions, detail.status) }}
          </a-tag>
        </a-descriptions-item>
        <a-descriptions-item label="进程退出码">{{ detail.processExitCode ?? '-' }}</a-descriptions-item>
        <a-descriptions-item label="数据开始">{{ detail.rangeStartTime || '-' }}</a-descriptions-item>
        <a-descriptions-item label="数据结束">{{ detail.rangeEndTime || '-' }}</a-descriptions-item>
        <a-descriptions-item label="train 数量">{{ formatCount(detail.trainCount) }}</a-descriptions-item>
        <a-descriptions-item label="eval 数量">{{ formatCount(detail.evalCount) }}</a-descriptions-item>
        <a-descriptions-item label="正样本">{{ formatCount(detail.positiveCount) }}</a-descriptions-item>
        <a-descriptions-item label="负样本">{{ formatCount(detail.negativeCount) }}</a-descriptions-item>
        <a-descriptions-item label="train 路径" :span="2">{{ detail.trainPath || '-' }}</a-descriptions-item>
        <a-descriptions-item label="eval 路径" :span="2">{{ detail.evalPath || '-' }}</a-descriptions-item>
        <a-descriptions-item label="日志路径" :span="2">{{ detail.logPath || '-' }}</a-descriptions-item>
        <a-descriptions-item label="错误码" :span="2">{{ detail.errorCode || '-' }}</a-descriptions-item>
        <a-descriptions-item label="错误信息" :span="2">{{ detail.errorMessage || '-' }}</a-descriptions-item>
        <a-descriptions-item label="开始时间">{{ detail.startedAt || '-' }}</a-descriptions-item>
        <a-descriptions-item label="结束时间">{{ detail.finishedAt || '-' }}</a-descriptions-item>
      </a-descriptions>

      <template v-if="detail.id">
        <div class="section-title">执行参数</div>
        <pre class="code-content">{{ formatJson(detail.argumentsJson) }}</pre>
        <div class="section-title">Python 结果</div>
        <pre class="code-content">{{ formatJson(detail.resultJson) }}</pre>
        <div class="section-title">运行日志</div>
        <pre class="code-content log-content">{{ detail.logContent || '-' }}</pre>
      </template>
      <a-empty v-else description="暂无执行详情" />
    </a-spin>
  </BasicDrawer>
</template>

<script lang="ts" setup>
  import { ref } from 'vue';
  import { BasicDrawer, useDrawerInner } from '/@/components/Drawer';
  import { getEtlExecution, type EtlExecution } from '../recommendEtl.api';
  import { executionStatusOptions, optionLabel, recommendTypeOptions, triggerTypeOptions } from '../recommendEtl.data';

  const loading = ref(false);
  const detail = ref<EtlExecution>({});
  const statusColorMap: Record<string, string> = {
    WAITING: 'warning',
    RUNNING: 'processing',
    SUCCESS: 'success',
    FAILED: 'error',
  };

  const [registerDrawer] = useDrawerInner(async (data) => {
    detail.value = {};
    if (!data?.id) {
      return;
    }
    loading.value = true;
    try {
      detail.value = await getEtlExecution({ id: data.id });
    } finally {
      loading.value = false;
    }
  });

  function formatCount(value?: number) {
    return value === null || value === undefined ? '-' : Number(value).toLocaleString();
  }

  function formatJson(value?: string) {
    if (!value) {
      return '-';
    }
    try {
      return JSON.stringify(JSON.parse(value), null, 2);
    } catch {
      return value;
    }
  }
</script>

<style lang="less" scoped>
  .section-title {
    margin: 18px 0 8px;
    font-weight: 600;
  }

  .code-content {
    max-height: 300px;
    margin: 0;
    padding: 14px;
    overflow: auto;
    color: rgba(0, 0, 0, 0.85);
    white-space: pre-wrap;
    overflow-wrap: anywhere;
    background: #f5f5f5;
    border: 1px solid #d9d9d9;
    border-radius: 4px;
  }

  .log-content {
    max-height: 520px;
  }
</style>
