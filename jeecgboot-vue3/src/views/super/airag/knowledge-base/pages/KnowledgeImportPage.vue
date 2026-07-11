<template>
  <PageWrapper contentFullHeight fixedHeight>
    <div class="import-page">
      <div class="page-header">
        <div>
          <h2 class="title">文档导入</h2>
          <p class="desc">当前知识库：{{ kbId || '-' }}，支持文本预览、文件预览、确认导入的第一版入口。</p>
        </div>
        <a-space>
          <a-button @click="goWorkspace">返回工作台</a-button>
          <a-button @click="goDocuments">文档管理</a-button>
        </a-space>
      </div>

      <a-card>
        <a-tabs v-model:activeKey="mode">
          <a-tab-pane key="text" tab="手动文本导入">
            <a-form layout="vertical">
              <a-form-item label="文档名称" required>
                <a-input v-model:value="textForm.document_name" placeholder="请输入文档名称" />
              </a-form-item>
              <a-form-item label="内容" required>
                <a-textarea v-model:value="textForm.content" :rows="10" placeholder="请输入待切分内容" />
              </a-form-item>
              <a-row :gutter="16">
                <a-col :span="12">
                  <a-form-item label="chunk_size">
                    <a-input-number v-model:value="textForm.chunk_size" :min="1" style="width: 100%" />
                  </a-form-item>
                </a-col>
                <a-col :span="12">
                  <a-form-item label="chunk_overlap">
                    <a-input-number v-model:value="textForm.chunk_overlap" :min="0" style="width: 100%" />
                  </a-form-item>
                </a-col>
              </a-row>
              <a-space>
                <a-button type="primary" @click="previewText">预览切分</a-button>
                <a-button type="primary" ghost @click="confirmTextImport">确认导入</a-button>
              </a-space>
            </a-form>
          </a-tab-pane>

          <a-tab-pane key="file" tab="文件上传导入">
            <a-form layout="vertical">
              <a-form-item label="文档名称" required>
                <a-input v-model:value="fileForm.document_name" placeholder="请输入文档名称" />
              </a-form-item>
              <a-form-item label="上传文件" required>
                <a-upload :showUploadList="false" :beforeUpload="beforeUpload">
                  <a-button>选择文件</a-button>
                </a-upload>
                <div v-if="selectedFileName" class="file-name">已选择：{{ selectedFileName }}</div>
              </a-form-item>
              <a-row :gutter="16">
                <a-col :span="12">
                  <a-form-item label="chunk_size">
                    <a-input-number v-model:value="fileForm.chunk_size" :min="1" style="width: 100%" />
                  </a-form-item>
                </a-col>
                <a-col :span="12">
                  <a-form-item label="chunk_overlap">
                    <a-input-number v-model:value="fileForm.chunk_overlap" :min="0" style="width: 100%" />
                  </a-form-item>
                </a-col>
              </a-row>
              <a-space>
                <a-button type="primary" @click="previewFile">预览切分</a-button>
                <a-button type="primary" ghost @click="confirmFileImport">确认导入</a-button>
              </a-space>
            </a-form>
          </a-tab-pane>
        </a-tabs>
      </a-card>

      <a-card class="preview-card" title="切分预览 / 导入结果">
        <a-empty v-if="!previewList.length" description="暂无预览内容" />
        <a-list v-else :data-source="previewList" bordered>
          <template #renderItem="{ item }">
            <a-list-item>
              <div class="preview-item">
                <div class="preview-head">
                  <strong>Chunk {{ item.sort_no || item.sortNo || '-' }}</strong>
                  <span>token: {{ item.token_count || item.tokenCount || '-' }}</span>
                </div>
                <div class="preview-content">{{ item.content }}</div>
              </div>
            </a-list-item>
          </template>
        </a-list>
      </a-card>
    </div>
  </PageWrapper>
</template>

<script lang="ts" setup>
  import { computed, reactive, ref } from 'vue';
  import { useRoute, useRouter } from 'vue-router';
  import { PageWrapper } from '/@/components/Page';
  import { useMessage } from '/@/hooks/web/useMessage';
  import {
    confirmKnowledgeBaseImport,
    previewKnowledgeBaseFileChunk,
    previewKnowledgeBaseTextChunk,
  } from '../KnowledgeBase.api';

  const route = useRoute();
  const router = useRouter();
  const { createMessage } = useMessage();
  const kbId = computed(() => String(route.params.kbId || ''));
  const mode = ref('text');
  const previewList = ref<any[]>([]);
  const selectedFile: any = ref(null);
  const selectedFileName = ref('');

  const textForm = reactive<any>({
    document_name: '',
    content: '',
    chunk_size: 800,
    chunk_overlap: 100,
  });

  const fileForm = reactive<any>({
    document_name: '',
    chunk_size: 800,
    chunk_overlap: 100,
  });

  function goWorkspace() {
    router.push(`/super/airag/knowledge-base/${kbId.value}`);
  }

  function goDocuments() {
    router.push(`/super/airag/knowledge-base/${kbId.value}/documents`);
  }

  function beforeUpload(file: File) {
    selectedFile.value = file;
    selectedFileName.value = file.name;
    return false;
  }

  async function previewText() {
    if (!kbId.value) return;
    const res = await previewKnowledgeBaseTextChunk(kbId.value, textForm);
    previewList.value = res?.result?.chunks || res?.result || [];
  }

  async function previewFile() {
    if (!kbId.value || !selectedFile.value) {
      createMessage.warning('请先选择文件');
      return;
    }
    const res = await previewKnowledgeBaseFileChunk(
      kbId.value,
      {
        file: selectedFile.value,
        filename: selectedFile.value.name,
        data: {
          document_name: fileForm.document_name,
          chunk_size: fileForm.chunk_size,
          chunk_overlap: fileForm.chunk_overlap,
        },
      },
    );
    previewList.value = res?.result?.chunks || res?.result || [];
  }

  async function confirmTextImport() {
    if (!kbId.value) return;
    if (!previewList.value.length) {
      createMessage.warning('请先预览切分结果');
      return;
    }
    const res = await confirmKnowledgeBaseImport(kbId.value, {
      document_name: textForm.document_name,
      source_type: 'text',
      chunks: previewList.value,
    });
    if (res?.success) {
      createMessage.success('导入成功');
      goDocuments();
    }
  }

  async function confirmFileImport() {
    if (!kbId.value || !selectedFile.value) {
      createMessage.warning('请先选择文件');
      return;
    }
    if (!previewList.value.length) {
      createMessage.warning('请先预览切分结果');
      return;
    }
    const res = await confirmKnowledgeBaseImport(kbId.value, {
      document_name: fileForm.document_name,
      source_type: 'file',
      file_type: selectedFile.value.name?.split('.').pop()?.toLowerCase() || '',
      chunks: previewList.value,
    });
    if (res?.success) {
      createMessage.success('导入成功');
      goDocuments();
    }
  }
</script>

<style scoped lang="less">
  .import-page {
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

  .preview-card {
    margin-top: 16px;
  }

  .preview-item {
    width: 100%;
  }

  .preview-head {
    display: flex;
    justify-content: space-between;
    margin-bottom: 8px;
  }

  .preview-content {
    white-space: pre-wrap;
    color: #4b5563;
  }

  .file-name {
    margin-top: 8px;
    color: #1677ff;
  }
</style>
