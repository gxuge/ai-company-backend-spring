<template>
  <PageWrapper contentFullHeight fixedHeight>
    <div class="knowledge-base-page">
      <div class="page-header">
        <div>
          <h2 class="title">知识库中心</h2>
          <p class="desc">新知识库前端平行目录，后续对接 /kb/* 接口，不影响旧的 aiknowledge 模块。</p>
        </div>
        <a-space>
          <a-input v-model:value="keyword" allow-clear placeholder="搜索知识库名称" style="width: 240px" @press-enter="reload" />
          <a-button type="primary" @click="handleCreate">新建知识库</a-button>
        </a-space>
      </div>

      <a-spin :spinning="loading">
        <a-row :gutter="[16, 16]">
          <a-col :xs="24" :sm="12" :md="8" :lg="6" v-for="item in listData" :key="item.id">
            <a-card class="kb-card" hoverable @click="openWorkspace(item)">
              <div class="kb-card-header">
                <div class="kb-title ellipsis" :title="item.name">{{ item.name }}</div>
                <a-tag :color="Number(item.status) === 1 ? 'green' : 'red'">
                  {{ Number(item.status) === 1 ? '启用' : '禁用' }}
                </a-tag>
              </div>
              <div class="kb-desc ellipsis-2">{{ item.description || '暂无描述' }}</div>
              <div class="kb-meta">
                <span>类型：{{ item.biz_type || '-' }}</span>
              </div>
              <div class="kb-actions" @click.stop>
                <a-button type="link" size="small" @click="openWorkspace(item)">工作台</a-button>
                <a-divider type="vertical" />
                <a-button type="link" size="small" @click="goDocuments(item)">文档</a-button>
                <a-divider type="vertical" />
                <a-button type="link" size="small" @click="goImport(item)">导入</a-button>
                <a-divider type="vertical" />
                <a-button type="link" size="small" @click="goRetrievalTest(item)">检索测试</a-button>
                <a-button type="link" size="small" @click="handleEdit(item)">编辑</a-button>
                <a-divider type="vertical" />
                <a-button type="link" size="small" @click="handleDelete(item)">删除</a-button>
              </div>
            </a-card>
          </a-col>
          <a-col :xs="24" :sm="12" :md="8" :lg="6">
            <a-card class="add-card" @click="handleCreate">
              <div class="add-inner">
                <a-avatar :size="52" style="background: #e8f3ff; color: #1677ff">+</a-avatar>
                <div class="add-text">创建知识库</div>
                <div class="add-sub">进入知识库工作台后，可再继续接入文档、chunk、检索和 RAG。</div>
              </div>
            </a-card>
          </a-col>
        </a-row>
      </a-spin>
    </div>

    <KnowledgeBaseModal @register="registerModal" @success="reload" />
  </PageWrapper>
</template>

<script lang="ts" setup>
  import { onMounted, ref } from 'vue';
  import { useModal } from '/@/components/Modal';
  import { PageWrapper } from '/@/components/Page';
  import { useRouter } from 'vue-router';
  import KnowledgeBaseModal from './components/KnowledgeBaseModal.vue';
  import { deleteKnowledgeBase, listKnowledgeBase } from './KnowledgeBase.api';

  defineOptions({
    name: 'KnowledgeBaseIndex',
  });

  const router = useRouter();
  const [registerModal, { openModal }] = useModal();

  const loading = ref(false);
  const keyword = ref('');
  const listData = ref<Recordable[]>([]);

  onMounted(() => {
    reload();
  });

  async function reload() {
    loading.value = true;
    try {
      const params = {
        name: keyword.value,
        pageNo: 1,
        pageSize: 50,
        column: 'createTime',
        order: 'desc',
      };
      const res = await listKnowledgeBase(params);
      if (res?.success && res?.result?.records) {
        listData.value = res.result.records;
      } else if (Array.isArray(res?.result)) {
        listData.value = res.result;
      } else {
        listData.value = [];
      }
    } finally {
      loading.value = false;
    }
  }

  function handleCreate() {
    openModal(true, { isUpdate: false });
  }

  function handleEdit(record: Recordable) {
    openModal(true, { isUpdate: true, id: record.id, record });
  }

  function handleDelete(record: Recordable) {
    deleteKnowledgeBase(record, reload);
  }

  function openWorkspace(record: Recordable) {
    router.push(`/super/airag/knowledge-base/${record.id}`);
  }

  function goDocuments(record: Recordable) {
    router.push(`/super/airag/knowledge-base/${record.id}/documents`);
  }

  function goImport(record: Recordable) {
    router.push(`/super/airag/knowledge-base/${record.id}/import`);
  }

  function goRetrievalTest(record: Recordable) {
    router.push(`/super/airag/knowledge-base/${record.id}/retrieval-test`);
  }
</script>

<style scoped lang="less">
  .knowledge-base-page {
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

  .kb-card,
  .add-card {
    min-height: 170px;
    border-radius: 12px;
  }

  .kb-card {
    position: relative;
  }

  .kb-card-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 8px;
  }

  .kb-title {
    font-size: 16px;
    font-weight: 600;
  }

  .kb-desc {
    margin-top: 10px;
    min-height: 44px;
    color: #676f83;
  }

  .kb-meta {
    margin-top: 12px;
    color: #8f959e;
    font-size: 12px;
  }

  .kb-actions {
    margin-top: 12px;
    text-align: right;
  }

  .add-card {
    cursor: pointer;
    border: 1px dashed #91caff;
    background: linear-gradient(180deg, #f6fbff 0%, #ffffff 100%);
  }

  .add-inner {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    height: 100%;
    text-align: center;
    gap: 8px;
  }

  .add-text {
    font-size: 16px;
    font-weight: 600;
  }

  .add-sub {
    color: #8f959e;
    font-size: 12px;
  }

  .ellipsis {
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .ellipsis-2 {
    display: -webkit-box;
    -webkit-box-orient: vertical;
    -webkit-line-clamp: 2;
    overflow: hidden;
  }
</style>
