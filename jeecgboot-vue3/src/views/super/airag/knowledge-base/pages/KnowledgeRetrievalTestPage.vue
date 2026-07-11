<template>
  <PageWrapper contentFullHeight fixedHeight>
    <div class="test-page">
      <div class="page-header">
        <div>
          <h2 class="title">检索测试</h2>
          <p class="desc">像 FastGPT 一样先调参，再看召回结果、调试信息和引用长度。</p>
        </div>
        <a-space wrap>
          <a-button @click="resetForm">恢复默认</a-button>
          <a-button @click="goLogs">日志</a-button>
          <a-button type="primary" :loading="testing" @click="runTest">执行测试</a-button>
        </a-space>
      </div>

      <a-row :gutter="16" class="panel-grid">
        <a-col :xs="24" :xxl="9" :xl="10" :lg="11">
          <a-affix :offset-top="16">
            <a-space direction="vertical" style="width: 100%" :size="16">
              <a-card title="基础检索" class="config-card">
                <a-form layout="vertical">
                  <a-form-item label="query" required>
                    <a-textarea v-model:value="form.query" :rows="4" placeholder="请输入检索问题" />
                  </a-form-item>
                  <a-row :gutter="12">
                    <a-col :span="12">
                      <a-form-item label="search_mode">
                        <a-segmented v-model:value="form.search_mode" :options="searchModeOptions" block />
                      </a-form-item>
                    </a-col>
                    <a-col :span="12">
                      <a-form-item label="top_k">
                        <a-input-number v-model:value="form.top_k" :min="1" style="width: 100%" />
                      </a-form-item>
                    </a-col>
                  </a-row>
                  <a-row :gutter="12">
                    <a-col :span="12">
                      <a-form-item label="similarity_threshold">
                        <a-input-number v-model:value="form.similarity_threshold" :min="0" :max="1" :step="0.05" style="width: 100%" />
                      </a-form-item>
                    </a-col>
                    <a-col :span="12">
                      <a-form-item label="keyword_threshold">
                        <a-input-number v-model:value="form.keyword_threshold" :min="0" :max="1" :step="0.05" style="width: 100%" />
                      </a-form-item>
                    </a-col>
                  </a-row>
                </a-form>
              </a-card>

              <a-card title="Query Optimization" class="config-card">
                <a-form layout="vertical">
                  <a-row :gutter="12">
                    <a-col :span="12">
                      <a-form-item label="use_query_optimization">
                        <a-switch v-model:checked="form.use_query_optimization" />
                      </a-form-item>
                    </a-col>
                    <a-col :span="12">
                      <a-form-item label="query_optimization_mode">
                        <a-select v-model:value="form.query_optimization_mode" :options="optimizationModeOptions" />
                      </a-form-item>
                    </a-col>
                  </a-row>
                  <a-row :gutter="12">
                    <a-col :span="12">
                      <a-form-item label="max_rewrite_queries">
                        <a-input-number v-model:value="form.max_rewrite_queries" :min="1" :max="10" style="width: 100%" />
                      </a-form-item>
                    </a-col>
                    <a-col :span="12">
                      <a-form-item label="keep_original_query">
                        <a-switch v-model:checked="form.keep_original_query" />
                      </a-form-item>
                    </a-col>
                  </a-row>
                  <a-form-item label="chat_history">
                    <a-textarea
                      v-model:value="historyText"
                      :rows="6"
                      placeholder='[{ "role": "user", "content": "..." }, { "role": "assistant", "content": "..." }]'
                    />
                    <div class="helper-text">JSON 数组格式，检索前会尝试解析为 chat_history。</div>
                  </a-form-item>
                </a-form>
              </a-card>

              <a-card title="Rerank 与引用控制" class="config-card">
                <a-form layout="vertical">
                  <a-row :gutter="12">
                    <a-col :span="12">
                      <a-form-item label="use_rerank">
                        <a-switch v-model:checked="form.use_rerank" />
                      </a-form-item>
                    </a-col>
                    <a-col :span="12">
                      <a-form-item label="rerank_model">
                        <a-input v-model:value="form.rerank_model" placeholder="可留空，读取默认模型" />
                      </a-form-item>
                    </a-col>
                  </a-row>
                  <a-row :gutter="12">
                    <a-col :span="8">
                      <a-form-item label="rerank_top_n">
                        <a-input-number v-model:value="form.rerank_top_n" :min="1" style="width: 100%" />
                      </a-form-item>
                    </a-col>
                    <a-col :span="8">
                      <a-form-item label="final_top_k">
                        <a-input-number v-model:value="form.final_top_k" :min="1" style="width: 100%" />
                      </a-form-item>
                    </a-col>
                    <a-col :span="8">
                      <a-form-item label="rerank_score_threshold">
                        <a-input-number v-model:value="form.rerank_score_threshold" :min="0" :max="1" :step="0.05" style="width: 100%" />
                      </a-form-item>
                    </a-col>
                  </a-row>
                  <a-form-item label="reference_limit">
                    <a-input-number v-model:value="form.reference_limit" :min="1" style="width: 100%" />
                  </a-form-item>
                  <a-row :gutter="12">
                    <a-col :span="12">
                      <a-form-item label="semantic_weight">
                        <a-input-number v-model:value="form.semantic_weight" :min="0" :max="1" :step="0.05" style="width: 100%" />
                      </a-form-item>
                    </a-col>
                    <a-col :span="12">
                      <a-form-item label="keyword_weight">
                        <a-input-number v-model:value="form.keyword_weight" :min="0" :max="1" :step="0.05" style="width: 100%" />
                      </a-form-item>
                    </a-col>
                  </a-row>
                </a-form>
              </a-card>

              <a-card title="测试样例" class="config-card">
                <a-space wrap>
                  <a-tag v-for="item in exampleTags" :key="item.query" class="example-tag" @click="applyExample(item)">{{ item.label }}</a-tag>
                </a-space>
                <div class="helper-text">点击样例可以快速填充常见调参场景。</div>
              </a-card>
            </a-space>
          </a-affix>
        </a-col>

        <a-col :xs="24" :xxl="15" :xl="14" :lg="13">
          <a-space direction="vertical" style="width: 100%" :size="16">
            <a-card title="调试说明" class="info-card">
              <a-collapse ghost>
                <a-collapse-panel key="params" header="参数说明">
                  <a-descriptions bordered size="small" :column="2">
                    <a-descriptions-item label="search_mode">决定走 semantic / fulltext / hybrid</a-descriptions-item>
                    <a-descriptions-item label="top_k">控制候选结果数量</a-descriptions-item>
                    <a-descriptions-item label="similarity_threshold">过滤低语义分</a-descriptions-item>
                    <a-descriptions-item label="keyword_threshold">过滤低关键词分</a-descriptions-item>
                    <a-descriptions-item label="use_query_optimization">检索前改写/扩展 query</a-descriptions-item>
                    <a-descriptions-item label="use_rerank">对候选结果重新排序</a-descriptions-item>
                    <a-descriptions-item label="reference_limit">限制最终引用长度</a-descriptions-item>
                    <a-descriptions-item label="final_top_k">限制最终返回条数</a-descriptions-item>
                  </a-descriptions>
                </a-collapse-panel>
                <a-collapse-panel key="flow" header="结果融合流程">
                  <a-steps direction="vertical" size="small" :current="4">
                    <a-step title="1. Query Optimization" description="可选改写 / 关键词提取 / 扩展问法" />
                    <a-step title="2. 检索候选" description="semantic / fulltext / hybrid 召回候选" />
                    <a-step title="3. Rerank" description="只对前 rerank_top_n 条重排" />
                    <a-step title="4. Reference Limit" description="按排序累积引用长度" />
                    <a-step title="5. 输出结果" description="返回 results、actual_params、debug_info" />
                  </a-steps>
                </a-collapse-panel>
              </a-collapse>
            </a-card>

            <a-card class="result-summary" title="测试结果总览">
              <a-skeleton v-if="testing && !hasResult" active />
              <template v-else>
                <a-descriptions bordered size="small" :column="2">
                  <a-descriptions-item label="original_query">{{ result.original_query || '-' }}</a-descriptions-item>
                  <a-descriptions-item label="used_queries">{{ (result.used_queries || []).join(', ') || '-' }}</a-descriptions-item>
                  <a-descriptions-item label="result_count">{{ result.result_count ?? 0 }}</a-descriptions-item>
                  <a-descriptions-item label="used_reference_length">{{ result.used_reference_length ?? 0 }}</a-descriptions-item>
                  <a-descriptions-item label="search_mode">{{ result.search_mode || form.search_mode || '-' }}</a-descriptions-item>
                  <a-descriptions-item label="status">{{ result.status || (testing ? 'processing' : '-') }}</a-descriptions-item>
                </a-descriptions>
                <a-divider />
                <div class="query-rows">
                  <a-tag color="blue">Query Optimization: {{ yesNo(result.use_query_optimization ?? form.use_query_optimization) }}</a-tag>
                  <a-tag color="purple">Rerank: {{ yesNo(result.use_rerank ?? form.use_rerank) }}</a-tag>
                  <a-tag color="geekblue">Stream: {{ yesNo(result.stream) }}</a-tag>
                  <a-tag color="green">LLM: {{ result.llm_model || '-' }}</a-tag>
                </div>
              </template>
            </a-card>

            <a-card class="result-card" title="召回结果">
              <a-empty v-if="!result.results?.length" description="暂无结果" />
              <a-list v-else :data-source="result.results || []" bordered>
                <template #renderItem="{ item }">
                  <a-list-item>
                    <div class="result-item">
                      <div class="result-head">
                        <div class="result-title">
                          <strong>{{ item.document_name || item.title || '命中结果' }}</strong>
                          <span class="result-subtitle">{{ item.kb_name || item.kb_id || '-' }}</span>
                        </div>
                        <div class="score-tags">
                          <a-tag color="blue">score {{ formatScore(item.score) }}</a-tag>
                          <a-tag color="cyan">semantic {{ formatScore(item.semantic_score) }}</a-tag>
                          <a-tag color="gold">keyword {{ formatScore(item.keyword_score) }}</a-tag>
                          <a-tag color="green">final {{ formatScore(item.final_score) }}</a-tag>
                          <a-tag v-if="item.rerank_score !== undefined && item.rerank_score !== null" color="purple">rerank {{ formatScore(item.rerank_score) }}</a-tag>
                        </div>
                      </div>
                      <div class="result-meta">
                        <span>query: {{ item.matched_query || '-' }}</span>
                        <span>index: {{ item.matched_index_text || '-' }}</span>
                        <span>field: {{ item.matched_field || '-' }}</span>
                        <span>hit: {{ item.hit_type || '-' }}</span>
                      </div>
                      <div class="result-meta">
                        <span>chunk: {{ item.chunk_id || '-' }}</span>
                        <span>index_id: {{ item.chunk_index_id || '-' }}</span>
                        <span>reference_length: {{ item.reference_length ?? '-' }}</span>
                        <span>source: {{ item.source_type || '-' }}</span>
                      </div>
                      <div class="result-content">{{ item.content }}</div>
                    </div>
                  </a-list-item>
                </template>
              </a-list>
            </a-card>

            <a-row :gutter="16">
              <a-col :xs="24" :lg="12">
                <a-card title="Actual Params" class="detail-card">
                  <pre class="json-view">{{ prettyJson(result.actual_params || {}) }}</pre>
                </a-card>
              </a-col>
              <a-col :xs="24" :lg="12">
                <a-card title="Debug Info" class="detail-card">
                  <pre class="json-view">{{ prettyJson(result.debug_info || {}) }}</pre>
                </a-card>
              </a-col>
            </a-row>
          </a-space>
        </a-col>
      </a-row>
    </div>
  </PageWrapper>
</template>

<script lang="ts" setup>
  import { computed, reactive, ref } from 'vue';
  import { useRoute, useRouter } from 'vue-router';
  import { PageWrapper } from '/@/components/Page';
  import { useMessage } from '/@/hooks/web/useMessage';
  import { retrievalTestKnowledgeBase } from '../KnowledgeBase.api';

  defineOptions({
    name: 'KnowledgeBaseRetrievalTestPage',
  });

  const route = useRoute();
  const router = useRouter();
  const { createMessage } = useMessage();
  const kbId = computed(() => String(route.params.kbId || ''));
  const testing = ref(false);
  const result = ref<any>({});
  const historyText = ref('[]');

  const searchModeOptions = [
    { label: 'semantic', value: 'semantic' },
    { label: 'fulltext', value: 'fulltext' },
    { label: 'hybrid', value: 'hybrid' },
  ];
  const optimizationModeOptions = [
    { label: 'rewrite', value: 'rewrite' },
    { label: 'keywords', value: 'keywords' },
    { label: 'expand', value: 'expand' },
    { label: 'hybrid', value: 'hybrid' },
    { label: 'off', value: 'off' },
  ];

  const exampleTags = [
    { label: '角色背景故事怎么写', query: '角色背景故事怎么写', search_mode: 'hybrid', use_query_optimization: true, use_rerank: true },
    { label: 'background_story', query: 'background_story', search_mode: 'fulltext', use_query_optimization: false, use_rerank: false },
    { label: 'submit_role_core_fill', query: 'submit_role_core_fill 参数', search_mode: 'fulltext', use_query_optimization: true, use_rerank: false },
    { label: 'pending / failed', query: 'pending 和 failed 有什么区别？', search_mode: 'semantic', use_query_optimization: true, use_rerank: true },
  ];

  const form = reactive<any>({
    query: '',
    search_mode: 'semantic',
    top_k: 5,
    similarity_threshold: 0.5,
    keyword_threshold: 0.3,
    use_rerank: false,
    rerank_model: '',
    rerank_top_n: 20,
    rerank_score_threshold: 0,
    final_top_k: 5,
    reference_limit: 4000,
    use_query_optimization: false,
    query_optimization_mode: 'rewrite',
    max_rewrite_queries: 3,
    keep_original_query: true,
    semantic_weight: 0.5,
    keyword_weight: 0.5,
  });

  const hasResult = computed(() => !!(result.value && (result.value.results?.length || result.value.answer || result.value.debug_info)));

  function goLogs() {
    router.push(`/super/airag/knowledge-base/${kbId.value}/logs`);
  }

  function resetForm() {
    form.query = '';
    form.search_mode = 'semantic';
    form.top_k = 5;
    form.similarity_threshold = 0.5;
    form.keyword_threshold = 0.3;
    form.use_rerank = false;
    form.rerank_model = '';
    form.rerank_top_n = 20;
    form.rerank_score_threshold = 0;
    form.final_top_k = 5;
    form.reference_limit = 4000;
    form.use_query_optimization = false;
    form.query_optimization_mode = 'rewrite';
    form.max_rewrite_queries = 3;
    form.keep_original_query = true;
    form.semantic_weight = 0.5;
    form.keyword_weight = 0.5;
    historyText.value = '[]';
    result.value = {};
  }

  function applyExample(item: Recordable) {
    form.query = item.query || '';
    form.search_mode = item.search_mode || 'semantic';
    form.use_query_optimization = Boolean(item.use_query_optimization);
    form.use_rerank = Boolean(item.use_rerank);
  }

  function yesNo(value: any) {
    return value ? '是' : '否';
  }

  function formatScore(value: any) {
    if (value === undefined || value === null || value === '') {
      return '-';
    }
    const num = Number(value);
    return Number.isNaN(num) ? String(value) : num.toFixed(4);
  }

  function prettyJson(value: any) {
    try {
      return JSON.stringify(value ?? {}, null, 2);
    } catch (error) {
      return String(value ?? '');
    }
  }

  function buildParams() {
    const params: Record<string, any> = {
      ...form,
    };
    if (historyText.value && historyText.value.trim()) {
      try {
        const parsed = JSON.parse(historyText.value);
        if (!Array.isArray(parsed)) {
          throw new Error('chat_history 必须是数组');
        }
        params.chat_history = parsed;
      } catch (error: any) {
        throw new Error(error?.message || 'chat_history 格式非法');
      }
    }
    return params;
  }

  async function runTest() {
    if (!kbId.value || !form.query) {
      createMessage.warning('请输入 query');
      return;
    }
    testing.value = true;
    result.value = {};
    try {
      const params = buildParams();
      const res = await retrievalTestKnowledgeBase(kbId.value, params);
      result.value = res?.result || res || {};
      if (!result.value?.results?.length) {
        createMessage.info('本次测试没有命中结果');
      }
    } catch (error: any) {
      createMessage.error(error?.message || '检索测试失败');
    } finally {
      testing.value = false;
    }
  }
</script>

<style scoped lang="less">
  .test-page {
    padding: 16px;
  }

  .page-header {
    display: flex;
    align-items: flex-start;
    justify-content: space-between;
    gap: 16px;
    margin-bottom: 16px;
  }

  .title {
    margin: 0;
    font-size: 22px;
    font-weight: 600;
  }

  .desc {
    margin: 6px 0 0;
    color: #8f959e;
  }

  .panel-grid {
    align-items: flex-start;
  }

  .config-card {
    box-shadow: 0 2px 8px rgba(16, 24, 40, 0.04);
    border-radius: 12px;
  }

  .helper-text {
    margin-top: 8px;
    color: #8f959e;
    font-size: 12px;
    line-height: 1.6;
  }

  .example-tag {
    cursor: pointer;
    padding: 4px 10px;
    margin-bottom: 6px;
  }

  .result-summary,
  .result-card,
  .detail-card,
  .info-card {
    border-radius: 12px;
    box-shadow: 0 2px 8px rgba(16, 24, 40, 0.04);
  }

  .query-rows {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
  }

  .result-item {
    width: 100%;
    padding: 4px 0;
  }

  .result-head {
    display: flex;
    justify-content: space-between;
    gap: 12px;
    flex-wrap: wrap;
  }

  .result-title {
    display: flex;
    flex-direction: column;
    gap: 4px;
  }

  .result-subtitle {
    color: #8f959e;
    font-size: 12px;
  }

  .score-tags {
    display: flex;
    flex-wrap: wrap;
    gap: 6px;
  }

  .result-meta {
    display: flex;
    justify-content: space-between;
    gap: 12px;
    flex-wrap: wrap;
    margin-top: 8px;
    color: #8f959e;
    font-size: 12px;
  }

  .result-content {
    margin-top: 10px;
    white-space: pre-wrap;
    line-height: 1.7;
    color: #344767;
  }

  .json-view {
    margin: 0;
    max-height: 320px;
    overflow: auto;
    white-space: pre-wrap;
    word-break: break-word;
    background: #0f172a;
    color: #e2e8f0;
    padding: 12px;
    border-radius: 8px;
  }
</style>
