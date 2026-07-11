<template>
  <PageWrapper contentFullHeight fixedHeight>
    <div class="rag-page">
      <div class="page-header">
        <div>
          <h2 class="title">RAG 问答</h2>
          <p class="desc">支持多知识库、外部知识库、检索优化、Rerank、引用和流式输出。</p>
        </div>
        <a-space wrap>
          <a-button @click="goWorkspace">返回工作台</a-button>
          <a-button @click="goRetrievalTest">检索测试</a-button>
          <a-button @click="clearAnswer">清空结果</a-button>
          <a-button type="primary" :loading="asking" @click="askQuestion">生成答案</a-button>
        </a-space>
      </div>

      <a-row :gutter="16" class="panel-grid">
        <a-col :xs="24" :xxl="9" :xl="10" :lg="11">
          <a-affix :offset-top="16">
            <a-space direction="vertical" style="width: 100%" :size="16">
              <a-card title="问答配置" class="config-card">
                <a-form layout="vertical">
                  <a-form-item label="query" required>
                    <a-textarea v-model:value="form.query" :rows="4" placeholder="请输入问题" />
                  </a-form-item>
                  <a-row :gutter="12">
                    <a-col :span="12">
                      <a-form-item label="answer_mode">
                        <a-radio-group v-model:value="form.answer_mode" button-style="solid">
                          <a-radio-button v-for="item in answerModeOptions" :key="item.value" :value="item.value">
                            {{ item.label }}
                          </a-radio-button>
                        </a-radio-group>
                      </a-form-item>
                    </a-col>
                    <a-col :span="12">
                      <a-form-item label="search_mode">
                        <a-segmented v-model:value="form.search_mode" :options="searchModeOptions" block />
                      </a-form-item>
                    </a-col>
                  </a-row>

                  <a-row :gutter="12">
                    <a-col :span="12">
                      <a-form-item label="cite_sources">
                        <a-switch v-model:checked="form.cite_sources" />
                      </a-form-item>
                    </a-col>
                    <a-col :span="12">
                      <a-form-item label="stream">
                        <a-switch v-model:checked="form.stream" />
                      </a-form-item>
                    </a-col>
                  </a-row>

                  <a-form-item label="kb_ids">
                    <a-select v-model:value="form.kb_ids" mode="multiple" :options="kbOptions" placeholder="选择内部知识库" />
                  </a-form-item>
                  <a-form-item label="external_kb_ids">
                    <a-select v-model:value="form.external_kb_ids" mode="multiple" :options="externalKbOptions" placeholder="选择外部知识库" />
                  </a-form-item>

                  <a-row :gutter="12">
                    <a-col :span="12">
                      <a-form-item label="top_k">
                        <a-input-number v-model:value="form.top_k" :min="1" style="width: 100%" />
                      </a-form-item>
                    </a-col>
                    <a-col :span="12">
                      <a-form-item label="final_top_k">
                        <a-input-number v-model:value="form.final_top_k" :min="1" style="width: 100%" />
                      </a-form-item>
                    </a-col>
                  </a-row>
                  <a-row :gutter="12">
                    <a-col :span="12">
                      <a-form-item label="reference_limit">
                        <a-input-number v-model:value="form.reference_limit" :min="1" style="width: 100%" />
                      </a-form-item>
                    </a-col>
                    <a-col :span="12">
                      <a-form-item label="metadata_filter">
                        <a-input v-model:value="metadataFilterText" placeholder='{"key":"value"}' />
                      </a-form-item>
                    </a-col>
                  </a-row>
                </a-form>
              </a-card>

              <a-card title="检索增强" class="config-card">
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
                      <a-form-item label="rerank_score_threshold">
                        <a-input-number v-model:value="form.rerank_score_threshold" :min="0" :max="1" :step="0.05" style="width: 100%" />
                      </a-form-item>
                    </a-col>
                    <a-col :span="8">
                      <a-form-item label="max_rewrite_queries">
                        <a-input-number v-model:value="form.max_rewrite_queries" :min="1" :max="10" style="width: 100%" />
                      </a-form-item>
                    </a-col>
                  </a-row>
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

              <a-card title="上下文输入" class="config-card">
                <a-form layout="vertical">
                  <a-form-item label="chat_history">
                    <a-textarea
                      v-model:value="chatHistoryText"
                      :rows="6"
                      placeholder='[{ "role": "user", "content": "..." }, { "role": "assistant", "content": "..." }]'
                    />
                    <div class="helper-text">用于指代消解和上下文补全。JSON 数组格式。</div>
                  </a-form-item>
                </a-form>
              </a-card>

              <a-card title="快捷场景" class="config-card">
                <a-space wrap>
                  <a-tag v-for="item in exampleTags" :key="item.label" class="example-tag" @click="applyExample(item)">{{ item.label }}</a-tag>
                </a-space>
              </a-card>
            </a-space>
          </a-affix>
        </a-col>

        <a-col :xs="24" :xxl="15" :xl="14" :lg="13">
          <a-space direction="vertical" style="width: 100%" :size="16">
              <a-card class="answer-card" :title="answerCardTitle">
                <template #extra>
                  <span class="card-extra-meta">{{ answerCardExtra }}</span>
                </template>
                <a-skeleton v-if="asking && !answerResult.answer" active />
                <template v-else>
                  <div class="answer-text">{{ answerResult.answer || '暂无答案' }}</div>
                  <a-space wrap class="answer-actions">
                    <a-button size="small" @click="copyAnswer">复制答案</a-button>
                    <a-button size="small" @click="copyCitations">复制引用</a-button>
                    <a-button size="small" @click="copyContext">复制上下文</a-button>
                  </a-space>
                  <a-divider />
                  <a-descriptions bordered size="small" :column="2">
                  <a-descriptions-item label="status">{{ answerResult.status || (streaming ? 'streaming' : '-') }}</a-descriptions-item>
                  <a-descriptions-item label="answer_mode">{{ answerResult.answer_mode || form.answer_mode }}</a-descriptions-item>
                  <a-descriptions-item label="search_mode">{{ answerResult.search_mode || form.search_mode }}</a-descriptions-item>
                  <a-descriptions-item label="result_count">{{ answerResult.result_count ?? 0 }}</a-descriptions-item>
                  <a-descriptions-item label="used_reference_length">{{ answerResult.used_reference_length ?? 0 }}</a-descriptions-item>
                  <a-descriptions-item label="llm_model">{{ answerResult.llm_model || '-' }}</a-descriptions-item>
                </a-descriptions>
                <a-divider v-if="queryTags.length" />
                <a-space wrap>
                  <a-tag v-for="tag in queryTags" :key="tag" color="blue">{{ tag }}</a-tag>
                </a-space>
              </template>
            </a-card>

            <a-card class="detail-card">
              <a-tabs v-model:activeKey="activeTab" size="small">
                <a-tab-pane key="citations" tab="引用来源">
                  <a-empty v-if="!answerResult.citations?.length" description="暂无引用" />
                  <a-list v-else :data-source="answerResult.citations || []" bordered>
                    <template #renderItem="{ item, index }">
                      <a-list-item>
                        <div class="source-item">
                          <div class="source-head">
                            <div>
                              <strong>#{{ index + 1 }} {{ item.document_name || item.kb_name || item.external_kb_name || '引用' }}</strong>
                              <div class="source-subtitle">
                                {{ item.kb_name || item.external_kb_name || item.kb_id || item.external_kb_id || '-' }}
                              </div>
                            </div>
                            <div class="source-score">
                              <a-tag color="green">score {{ formatScore(item.score) }}</a-tag>
                              <a-tag v-if="item.rerank_score !== undefined && item.rerank_score !== null" color="purple">rerank {{ formatScore(item.rerank_score) }}</a-tag>
                            </div>
                          </div>
                          <div class="source-meta">
                            <span>source_id: {{ item.citation_id || item.external_result_id || item.chunk_index_id || '-' }}</span>
                            <span>chunk: {{ item.chunk_id || '-' }}</span>
                            <span>index: {{ item.chunk_index_id || '-' }}</span>
                          </div>
                          <div class="citation-number">[{{ index + 1 }}]</div>
                          <div class="source-preview">{{ item.content_preview || item.source_url || '-' }}</div>
                        </div>
                      </a-list-item>
                    </template>
                  </a-list>
                </a-tab-pane>

                <a-tab-pane key="context" tab="上下文">
                  <a-empty v-if="!answerResult.used_context?.length" description="暂无上下文" />
                  <a-list v-else :data-source="answerResult.used_context || []" bordered>
                    <template #renderItem="{ item, index }">
                      <a-list-item>
                        <div class="context-item">
                          <div class="context-head">
                            <div>
                              <strong>#{{ index + 1 }} {{ item.document_name || item.title || item.kb_name || '上下文' }}</strong>
                              <div class="source-subtitle">
                                {{ item.source_scope || item.source_type || '-' }} / {{ item.kb_name || item.external_kb_name || '-' }}
                              </div>
                            </div>
                            <div class="source-score">
                              <a-tag color="blue">score {{ formatScore(item.score) }}</a-tag>
                              <a-tag color="cyan">semantic {{ formatScore(item.semantic_score) }}</a-tag>
                              <a-tag color="gold">keyword {{ formatScore(item.keyword_score) }}</a-tag>
                              <a-tag color="green">final {{ formatScore(item.final_score) }}</a-tag>
                            </div>
                          </div>
                          <div class="source-meta">
                            <span>query: {{ item.matched_query || '-' }}</span>
                            <span>index: {{ item.matched_index_text || '-' }}</span>
                            <span>field: {{ item.matched_field || '-' }}</span>
                            <span>hit: {{ item.hit_type || '-' }}</span>
                          </div>
                          <div class="source-preview">{{ item.content || '-' }}</div>
                        </div>
                      </a-list-item>
                    </template>
                  </a-list>
                </a-tab-pane>

                <a-tab-pane key="debug" tab="调试信息">
                  <a-row :gutter="16">
                    <a-col :xs="24" :lg="12">
                      <a-card size="small" title="Actual Params" class="inner-card">
                        <pre class="json-view">{{ prettyJson(answerResult.actual_params || {}) }}</pre>
                      </a-card>
                    </a-col>
                    <a-col :xs="24" :lg="12">
                      <a-card size="small" title="Debug Info" class="inner-card">
                        <pre class="json-view">{{ prettyJson(answerResult.debug_info || {}) }}</pre>
                      </a-card>
                    </a-col>
                  </a-row>
                </a-tab-pane>

                <a-tab-pane key="raw" tab="原始响应">
                  <pre class="json-view">{{ prettyJson(answerResult) }}</pre>
                </a-tab-pane>
              </a-tabs>
            </a-card>
          </a-space>
        </a-col>
      </a-row>
    </div>
  </PageWrapper>
</template>

<script lang="ts" setup>
  import { computed, onMounted, reactive, ref } from 'vue';
  import { useRoute, useRouter } from 'vue-router';
  import { PageWrapper } from '/@/components/Page';
  import { copyTextToClipboard } from '/@/hooks/web/useCopyToClipboard';
  import { useMessage } from '/@/hooks/web/useMessage';
  import { askRag, listExternalKnowledgeBases, listKnowledgeBase, streamRag } from '../KnowledgeBase.api';

  defineOptions({
    name: 'KnowledgeBaseRagPage',
  });

  const route = useRoute();
  const router = useRouter();
  const { createMessage } = useMessage();
  const kbId = computed(() => String(route.params.kbId || ''));
  const asking = ref(false);
  const streaming = ref(false);
  const answerResult = ref<any>({});
  const activeTab = ref('citations');
  const chatHistoryText = ref('[]');
  const metadataFilterText = ref('{}');
  const kbOptions = ref<any[]>([]);
  const externalKbOptions = ref<any[]>([]);
  const streamDraftAnswer = ref('');

  const answerModeOptions = [
    { label: 'strict', value: 'strict' },
    { label: 'balanced', value: 'balanced' },
    { label: 'creative', value: 'creative' },
  ];
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
    answer_mode: 'balanced',
    search_mode: 'semantic',
    cite_sources: true,
    stream: false,
    kb_ids: [],
    external_kb_ids: [],
    top_k: 5,
    final_top_k: 5,
    reference_limit: 4000,
    use_rerank: false,
    rerank_model: '',
    rerank_top_n: 20,
    rerank_score_threshold: 0,
    use_query_optimization: false,
    query_optimization_mode: 'rewrite',
    max_rewrite_queries: 3,
    keep_original_query: true,
    semantic_weight: 0.5,
    keyword_weight: 0.5,
    similarity_threshold: 0.5,
    keyword_threshold: 0.3,
    chat_history: [],
    metadata_filter: {},
  });

  const answerCardTitle = computed(() => (streaming.value ? '答案生成中' : '答案'));
  const answerCardExtra = computed(() => [
    answerResult.value?.status ? `状态: ${answerResult.value.status}` : null,
    answerResult.value?.llm_model ? `模型: ${answerResult.value.llm_model}` : null,
  ].filter(Boolean).join(' | '));
  const queryTags = computed(() => {
    const tags: string[] = [];
    if (answerResult.value?.original_query) {
      tags.push(`original: ${answerResult.value.original_query}`);
    }
    if (answerResult.value?.optimized_queries?.length) {
      tags.push(`optimized: ${answerResult.value.optimized_queries.join(' / ')}`);
    }
    if (answerResult.value?.used_queries?.length) {
      tags.push(`used: ${answerResult.value.used_queries.join(' / ')}`);
    }
    return tags;
  });

  onMounted(() => {
    resetForm();
    loadOptions();
  });

  async function loadOptions() {
    try {
      const [kbRes, externalRes] = await Promise.all([
        listKnowledgeBase({ pageNo: 1, pageSize: 200 }),
        listExternalKnowledgeBases({ pageNo: 1, pageSize: 200 }),
      ]);
      kbOptions.value = normalizeOptions(kbRes, 'name', 'id')
        .filter((item) => item.enabled !== false)
        .map((item) => ({ label: item.label, value: item.value }));
      externalKbOptions.value = normalizeOptions(externalRes, 'name', 'external_kb_id')
        .filter((item) => item.enabled !== false)
        .map((item) => ({ label: item.label, value: item.value }));
      if (!form.kb_ids.length && kbId.value) {
        form.kb_ids = [kbId.value];
      }
      if (!form.external_kb_ids.length) {
        form.external_kb_ids = [];
      }
      if (!kbOptions.value.length && kbId.value) {
      kbOptions.value = [{ label: kbId.value, value: kbId.value }];
      }
    } catch (error) {
      console.warn('加载知识库选项失败', error);
    }
  }

  function normalizeOptions(response: any, labelField: string, valueField: string) {
    const records = response?.result?.records || response?.result || [];
    const list = Array.isArray(records) ? records : [];
    return list
      .map((item: any) => ({
        label: item?.[labelField] || item?.name || item?.title || item?.id || item?.external_kb_id || '-',
        value: item?.[valueField] || item?.id || item?.external_kb_id,
        enabled: item?.enabled !== undefined ? item.enabled : Number(item?.status) === 1,
      }))
      .filter((item: any) => item.value);
  }

  function goWorkspace() {
    router.push(`/super/airag/knowledge-base/${kbId.value}`);
  }

  function goRetrievalTest() {
    router.push(`/super/airag/knowledge-base/${kbId.value}/retrieval-test`);
  }

  function resetForm() {
    form.query = '';
    form.answer_mode = 'balanced';
    form.search_mode = 'semantic';
    form.cite_sources = true;
    form.stream = false;
    form.kb_ids = kbId.value ? [kbId.value] : [];
    form.external_kb_ids = [];
    form.top_k = 5;
    form.final_top_k = 5;
    form.reference_limit = 4000;
    form.use_rerank = false;
    form.rerank_model = '';
    form.rerank_top_n = 20;
    form.rerank_score_threshold = 0;
    form.use_query_optimization = false;
    form.query_optimization_mode = 'rewrite';
    form.max_rewrite_queries = 3;
    form.keep_original_query = true;
    form.semantic_weight = 0.5;
    form.keyword_weight = 0.5;
    form.similarity_threshold = 0.5;
    form.keyword_threshold = 0.3;
    form.chat_history = [];
    form.metadata_filter = {};
    chatHistoryText.value = '[]';
    metadataFilterText.value = '{}';
    clearAnswer();
  }

  function clearAnswer() {
    answerResult.value = {};
    streamDraftAnswer.value = '';
    activeTab.value = 'citations';
  }

  function buildCopyBundle() {
    const citations = answerResult.value?.citations || [];
    const context = answerResult.value?.used_context || [];
    return [
      `Answer:\n${answerResult.value?.answer || '-'}`,
      '',
      `Citations:\n${citations
        .map((item: any, index: number) => {
          return [`[${index + 1}] ${item.document_name || item.kb_name || item.external_kb_name || '引用'}`, `score: ${item.score ?? '-'}`, `preview: ${item.content_preview || item.source_url || '-'}`].join('\n');
        })
        .join('\n\n') || '-'}`,
      '',
      `Context:\n${context
        .map((item: any, index: number) => {
          return [`[${index + 1}] ${item.document_name || item.title || item.kb_name || '上下文'}`, `query: ${item.matched_query || '-'}`, `content: ${item.content || '-'}`].join('\n');
        })
        .join('\n\n') || '-'}`,
    ].join('\n');
  }

  function copyAnswer() {
    const success = copyTextToClipboard(answerResult.value?.answer || '');
    createMessage[success ? 'success' : 'error'](success ? '答案已复制' : '复制失败');
  }

  function copyCitations() {
    const citations = answerResult.value?.citations || [];
    const text = citations
      .map((item: any, index: number) => {
        return [`[${index + 1}] ${item.document_name || item.kb_name || item.external_kb_name || '引用'}`, `score: ${item.score ?? '-'}`, `preview: ${item.content_preview || item.source_url || '-'}`].join('\n');
      })
      .join('\n\n');
    const success = copyTextToClipboard(text || '');
    createMessage[success ? 'success' : 'error'](success ? '引用已复制' : '复制失败');
  }

  function copyContext() {
    const success = copyTextToClipboard(buildCopyBundle());
    createMessage[success ? 'success' : 'error'](success ? '上下文已复制' : '复制失败');
  }

  function applyExample(item: Recordable) {
    form.query = item.query || '';
    form.search_mode = item.search_mode || 'semantic';
    form.use_query_optimization = Boolean(item.use_query_optimization);
    form.use_rerank = Boolean(item.use_rerank);
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

  function buildPayload() {
    const payload: Record<string, any> = {
      ...form,
      kb_ids: form.kb_ids?.length ? form.kb_ids : kbId.value ? [kbId.value] : [],
    };

    if (!payload.kb_ids.length && !payload.external_kb_ids?.length) {
      throw new Error('请至少选择一个知识库');
    }

    if (chatHistoryText.value && chatHistoryText.value.trim()) {
      payload.chat_history = JSON.parse(chatHistoryText.value);
      if (!Array.isArray(payload.chat_history)) {
        throw new Error('chat_history 必须是数组');
      }
    } else {
      payload.chat_history = [];
    }

    if (metadataFilterText.value && metadataFilterText.value.trim()) {
      payload.metadata_filter = JSON.parse(metadataFilterText.value);
      if (payload.metadata_filter === null || Array.isArray(payload.metadata_filter) || typeof payload.metadata_filter !== 'object') {
        throw new Error('metadata_filter 必须是对象');
      }
    } else {
      payload.metadata_filter = {};
    }

    return payload;
  }

  async function askQuestion() {
    if (!form.query) {
      createMessage.warning('请输入 query');
      return;
    }

    asking.value = true;
    streaming.value = false;
    clearAnswer();

    try {
      const payload = buildPayload();
      if (form.stream) {
        streaming.value = true;
        await runStream(payload);
        return;
      }

      const res = await askRag(payload);
      answerResult.value = normalizeResult(res?.result || res || {});
      activeTab.value = 'citations';
    } catch (error: any) {
      createMessage.error(error?.message || 'RAG 问答失败');
      answerResult.value = normalizeError(error);
    } finally {
      asking.value = false;
      streaming.value = false;
    }
  }

  async function runStream(payload: Record<string, any>) {
    try {
      const readableStream: any = await streamRag(payload);
      if (!readableStream || typeof readableStream.getReader !== 'function') {
        throw new Error('流式响应不可用');
      }
      await consumeStream(readableStream);
    } catch (error: any) {
      throw new Error(error?.message || '流式问答失败');
    }
  }

  async function consumeStream(readableStream: ReadableStream) {
    const reader = readableStream.getReader();
    const decoder = new TextDecoder('UTF-8');
    let buffer = '';
    let answerDraft = '';
    while (true) {
      const { done, value } = await reader.read();
      if (done) {
        break;
      }
      buffer += decoder.decode(value, { stream: true });
      const blocks = buffer.split('\n\n');
      buffer = blocks.pop() || '';
      for (const block of blocks) {
        handleStreamBlock(block, (payload) => {
          if (payload.type === 'delta') {
            answerDraft += payload.delta || '';
            answerResult.value = {
              ...answerResult.value,
              answer: answerDraft,
              status: 'streaming',
            };
            return;
          }
          if (payload.type === 'complete') {
            answerDraft = payload.data?.answer || answerDraft;
            answerResult.value = normalizeResult({
              ...(payload.data || {}),
              answer: answerDraft,
            });
            activeTab.value = 'citations';
            return;
          }
          if (payload.type === 'start') {
            answerResult.value = {
              ...normalizeResult(answerResult.value),
              ...payload.data,
              answer: answerDraft,
              status: 'streaming',
            };
            return;
          }
          if (payload.type === 'error') {
            answerResult.value = normalizeError(payload.data || payload.message || 'stream error');
            throw new Error(payload.message || payload.data?.message || '流式问答失败');
          }
        });
      }
    }
    if (buffer) {
      handleStreamBlock(buffer, (payload) => {
        if (payload.type === 'delta') {
          answerDraft += payload.delta || '';
          answerResult.value = {
            ...answerResult.value,
            answer: answerDraft,
            status: 'streaming',
          };
        }
      });
    }
    if (!answerResult.value?.answer && answerDraft) {
      answerResult.value = {
        ...normalizeResult(answerResult.value),
        answer: answerDraft,
      };
    }
  }

  function handleStreamBlock(block: string, onPayload: (payload: any) => void) {
    const lines = block.split('\n');
    const dataLines: string[] = [];
    let eventName = '';
    for (const line of lines) {
      if (line.startsWith('event:')) {
        eventName = line.replace('event:', '').trim();
      } else if (line.startsWith('data:')) {
        dataLines.push(line.replace('data:', '').trim());
      }
    }
    const dataText = dataLines.join('\n').trim();
    if (!dataText) {
      return;
    }
    const payloadData = parsePossibleJson(dataText);
    if (eventName === 'rag.delta') {
      onPayload({ type: 'delta', delta: payloadData?.delta ?? (typeof payloadData === 'string' ? payloadData : dataText), data: payloadData });
      return;
    }
    if (eventName === 'rag.complete') {
      onPayload({ type: 'complete', data: payloadData });
      return;
    }
    if (eventName === 'rag.error') {
      onPayload({
        type: 'error',
        message: payloadData?.message || (typeof payloadData === 'string' ? payloadData : dataText),
        data: payloadData,
      });
      return;
    }
    if (eventName === 'rag.start') {
      onPayload({ type: 'start', data: payloadData });
      return;
    }
    onPayload({ type: 'delta', delta: typeof payloadData === 'string' ? payloadData : dataText, data: payloadData });
  }

  function parsePossibleJson(value: string) {
    try {
      return JSON.parse(value);
    } catch (error) {
      return value;
    }
  }

  function normalizeResult(payload: any) {
    const next = { ...(payload || {}) };
    next.citations = Array.isArray(next.citations) ? next.citations : [];
    next.used_context = Array.isArray(next.used_context) ? next.used_context : [];
    next.used_queries = Array.isArray(next.used_queries) ? next.used_queries : [];
    next.optimized_queries = Array.isArray(next.optimized_queries) ? next.optimized_queries : [];
    next.actual_params = next.actual_params || {};
    next.debug_info = next.debug_info || {};
    next.status = next.status || 'success';
    return next;
  }

  function normalizeError(error: any) {
    return {
      answer: '',
      citations: [],
      used_context: [],
      used_queries: [],
      optimized_queries: [],
      actual_params: {},
      debug_info: {},
      status: 'failed',
      error_message: error?.message || error?.error_message || String(error || '问答失败'),
    };
  }
</script>

<style scoped lang="less">
  .rag-page {
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

  .config-card,
  .answer-card,
  .detail-card {
    border-radius: 12px;
    box-shadow: 0 2px 8px rgba(16, 24, 40, 0.04);
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

  .answer-text {
    white-space: pre-wrap;
    line-height: 1.8;
    font-size: 15px;
    color: #1f2937;
    min-height: 80px;
  }

  .answer-actions {
    margin-bottom: 4px;
  }

  .card-extra-meta {
    color: #8f959e;
    font-size: 12px;
  }

  .source-item,
  .context-item {
    width: 100%;
  }

  .source-head,
  .context-head {
    display: flex;
    justify-content: space-between;
    gap: 12px;
    flex-wrap: wrap;
  }

  .source-subtitle {
    margin-top: 4px;
    color: #8f959e;
    font-size: 12px;
  }

  .source-score {
    display: flex;
    gap: 6px;
    flex-wrap: wrap;
  }

  .source-meta {
    display: flex;
    justify-content: space-between;
    gap: 12px;
    flex-wrap: wrap;
    margin-top: 8px;
    color: #8f959e;
    font-size: 12px;
  }

  .source-preview {
    margin-top: 10px;
    white-space: pre-wrap;
    line-height: 1.7;
    color: #344767;
  }

  .citation-number {
    margin-top: 8px;
    color: @primary-color;
    font-weight: 600;
  }

  .inner-card {
    height: 100%;
  }

  .json-view {
    margin: 0;
    max-height: 360px;
    overflow: auto;
    white-space: pre-wrap;
    word-break: break-word;
    background: #0f172a;
    color: #e2e8f0;
    padding: 12px;
    border-radius: 8px;
  }

  :deep(.ant-radio-button-wrapper) {
    border-radius: 8px;
  }
</style>
