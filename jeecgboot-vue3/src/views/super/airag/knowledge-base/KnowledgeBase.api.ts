import { Modal } from 'ant-design-vue';
import { defHttp } from '/@/utils/http/axios';

export interface KnowledgeBaseRecord {
  id?: string;
  name: string;
  description?: string;
  biz_type?: string;
  status?: number | string;
  [key: string]: any;
}

enum Api {
  list = '/kb/list',
  create = '/kb/create',
  detail = '/kb',
}

export function listKnowledgeBase(params: Recordable) {
  return defHttp.get({ url: Api.list, params }, { isTransformResponse: false });
}

export function createKnowledgeBase(params: KnowledgeBaseRecord) {
  return defHttp.post({ url: Api.create, params });
}

export function updateKnowledgeBase(params: KnowledgeBaseRecord) {
  return defHttp.put({ url: `${Api.detail}/${params.id}`, params });
}

export function getKnowledgeBaseDetail(id: string) {
  return defHttp.get({ url: `${Api.detail}/${id}` }, { isTransformResponse: false });
}

export function deleteKnowledgeBase(record: KnowledgeBaseRecord, onSuccess: () => void) {
  Modal.confirm({
    title: '确认删除',
    content: `是否删除知识库「${record.name}」？`,
    okText: '确认',
    cancelText: '取消',
    onOk: () => {
      return defHttp.delete({ url: `${Api.detail}/${record.id}` }, { joinParamsToUrl: false }).then(() => {
        onSuccess();
      });
    },
  });
}

export function getKnowledgeBaseSearchConfig(kbId: string) {
  return defHttp.get({ url: `/kb/${kbId}/search-config` }, { isTransformResponse: false });
}

export function saveKnowledgeBaseSearchConfig(kbId: string, params: Recordable) {
  return defHttp.post({ url: `/kb/${kbId}/search-config`, params });
}

export function listKnowledgeBaseDocuments(kbId: string, params?: Recordable) {
  return defHttp.get({ url: `/kb/${kbId}/documents`, params }, { isTransformResponse: false });
}

export function createKnowledgeBaseDocument(kbId: string, params: Recordable) {
  return defHttp.post({ url: `/kb/${kbId}/documents`, params });
}

export function deleteKnowledgeBaseDocument(documentId: string) {
  return defHttp.delete({ url: `/kb/documents/${documentId}` });
}

export function listKnowledgeBaseChunks(kbId: string, params?: Recordable) {
  return defHttp.get({ url: `/kb/${kbId}/chunks`, params }, { isTransformResponse: false });
}

export function createKnowledgeBaseChunk(kbId: string, params: Recordable) {
  return defHttp.post({ url: `/kb/${kbId}/chunks`, params });
}

export function updateKnowledgeBaseChunk(chunkId: string, params: Recordable) {
  return defHttp.put({ url: `/kb/chunks/${chunkId}`, params });
}

export function deleteKnowledgeBaseChunk(chunkId: string) {
  return defHttp.delete({ url: `/kb/chunks/${chunkId}` });
}

export function listKnowledgeBaseChunkIndexes(kbId: string, params?: Recordable) {
  return defHttp.get({ url: `/kb/${kbId}/indexes`, params }, { isTransformResponse: false });
}

export function listKnowledgeBaseChunkIndexesByChunk(kbId: string, chunkId: string, params?: Recordable) {
  return defHttp.get({ url: `/kb/${kbId}/chunks/${chunkId}/indexes`, params }, { isTransformResponse: false });
}

export function createKnowledgeBaseChunkIndex(kbId: string, chunkId: string, params: Recordable) {
  return defHttp.post({ url: `/kb/${kbId}/chunks/${chunkId}/indexes`, params });
}

export function createKnowledgeBaseChunkIndexesBatch(kbId: string, chunkId: string, params: Recordable[]) {
  return defHttp.post({ url: `/kb/${kbId}/chunks/${chunkId}/indexes/batch`, params });
}

export function updateKnowledgeBaseChunkIndex(indexId: string, params: Recordable) {
  return defHttp.put({ url: `/kb/indexes/${indexId}`, params });
}

export function deleteKnowledgeBaseChunkIndex(indexId: string) {
  return defHttp.delete({ url: `/kb/indexes/${indexId}` });
}

export function getKnowledgeBaseChunkIndexDetail(indexId: string) {
  return defHttp.get({ url: `/kb/indexes/${indexId}` }, { isTransformResponse: false });
}

export function previewKnowledgeBaseTextChunk(kbId: string, params: Recordable) {
  return defHttp.post({ url: `/kb/${kbId}/chunks/preview-text`, params }, { isTransformResponse: false });
}

export function previewKnowledgeBaseFileChunk(kbId: string, params: Recordable) {
  return defHttp.uploadFile({ url: `/kb/${kbId}/chunks/preview-file` }, params, { success: () => undefined });
}

export function importKnowledgeBaseText(kbId: string, params: Recordable) {
  return defHttp.post({ url: `/kb/${kbId}/import/text`, params });
}

export function importKnowledgeBaseFile(kbId: string, params: Recordable) {
  return defHttp.uploadFile({ url: `/kb/${kbId}/import/file` }, params, { success: () => undefined });
}

export function confirmKnowledgeBaseImport(kbId: string, params: Recordable) {
  return defHttp.post({ url: `/kb/${kbId}/import/confirm`, params });
}

export function triggerKnowledgeBaseEmbedding(kbId: string, params?: Recordable) {
  return defHttp.post({ url: `/kb/${kbId}/embedding`, params });
}

export function triggerKnowledgeBaseDocumentEmbedding(documentId: string, params?: Recordable) {
  return defHttp.post({ url: `/kb/documents/${documentId}/embedding`, params });
}

export function triggerKnowledgeBaseChunkEmbedding(chunkId: string, params?: Recordable) {
  return defHttp.post({ url: `/kb/chunks/${chunkId}/embedding`, params });
}

export function triggerKnowledgeBaseChunkIndexEmbedding(indexId: string, params?: Recordable) {
  return defHttp.post({ url: `/kb/chunk-index/${indexId}/embedding`, params });
}

export function getKnowledgeBaseEmbeddingStatus(kbId: string) {
  return defHttp.get({ url: `/kb/${kbId}/embedding/status` }, { isTransformResponse: false });
}

export function searchKnowledgeBase(kbId: string, params: Recordable) {
  return defHttp.post({ url: `/kb/${kbId}/search`, params }, { isTransformResponse: false });
}

export function retrievalTestKnowledgeBase(kbId: string, params: Recordable) {
  return defHttp.post({ url: `/kb/retrieval-test/${kbId}`, params }, { isTransformResponse: false });
}

export function listRetrievalTestLogs(params?: Recordable) {
  return defHttp.get({ url: '/kb/retrieval-test/logs', params }, { isTransformResponse: false });
}

export function getRetrievalTestLogDetail(id: string) {
  return defHttp.get({ url: `/kb/retrieval-test/logs/${id}` }, { isTransformResponse: false });
}

export function listExternalKnowledgeBases(params?: Recordable) {
  return defHttp.get({ url: '/kb/external/list', params }, { isTransformResponse: false });
}

export function createExternalKnowledgeBase(params: Recordable) {
  return defHttp.post({ url: '/kb/external', params });
}

export function updateExternalKnowledgeBase(id: string, params: Recordable) {
  return defHttp.put({ url: `/kb/external/${id}`, params });
}

export function deleteExternalKnowledgeBase(id: string) {
  return defHttp.delete({ url: `/kb/external/${id}` });
}

export function getExternalKnowledgeBaseDetail(id: string) {
  return defHttp.get({ url: `/kb/external/${id}` }, { isTransformResponse: false });
}

export function testExternalKnowledgeBaseConnection(id: string, params?: Recordable) {
  return defHttp.post({ url: `/kb/external/${id}/test-connection`, params }, { isTransformResponse: false });
}

export function askRag(params: Recordable) {
  return defHttp.post({ url: '/kb/rag/ask', params }, { isTransformResponse: false });
}

export function streamRag(params: Recordable) {
  return defHttp.post(
    {
      url: '/kb/rag/stream',
      params,
      adapter: 'fetch',
      responseType: 'stream',
      timeout: 5 * 60 * 1000,
    },
    { isTransformResponse: false }
  );
}

export function listRagLogs(params?: Recordable) {
  return defHttp.get({ url: '/kb/rag/logs', params }, { isTransformResponse: false });
}

export function getRagLogDetail(id: string) {
  return defHttp.get({ url: `/kb/rag/logs/${id}` }, { isTransformResponse: false });
}
