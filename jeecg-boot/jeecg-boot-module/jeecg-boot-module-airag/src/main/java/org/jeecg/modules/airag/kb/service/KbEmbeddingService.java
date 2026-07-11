package org.jeecg.modules.airag.kb.service;

import org.jeecg.modules.airag.kb.vo.EmbeddingBatchResultVO;
import org.jeecg.modules.airag.kb.vo.EmbeddingStatusVO;

/**
 * KB embedding编排服务。
 */
public interface KbEmbeddingService {
    /**
     * 对整个知识库执行embedding。
     *
     * @param kbId 知识库ID
     * @param overrideSuccess 是否覆盖成功数据
     * @return 处理结果
     */
    EmbeddingBatchResultVO embedKb(String kbId, boolean overrideSuccess);

    /**
     * 对单个文档执行embedding。
     *
     * @param documentId 文档ID
     * @param overrideSuccess 是否覆盖成功数据
     * @return 处理结果
     */
    EmbeddingBatchResultVO embedDocument(String documentId, boolean overrideSuccess);

    /**
     * 对单个chunk执行embedding。
     *
     * @param chunkId chunk ID
     * @param overrideSuccess 是否覆盖成功数据
     * @return 处理结果
     */
    EmbeddingBatchResultVO embedChunk(String chunkId, boolean overrideSuccess);

    /**
     * 对单个chunk_index执行embedding。
     *
     * @param indexId chunk_index ID
     * @param overrideSuccess 是否覆盖成功数据
     * @return 处理结果
     */
    EmbeddingBatchResultVO embedIndex(String indexId, boolean overrideSuccess);

    /**
     * 查询知识库embedding进度。
     *
     * @param kbId 知识库ID
     * @return 统计结果
     */
    EmbeddingStatusVO getStatus(String kbId);

    /**
     * 删除知识库下所有向量。
     *
     * @param kbId 知识库ID
     */
    void deleteByKbId(String kbId);

    /**
     * 删除文档下所有向量。
     *
     * @param documentId 文档ID
     */
    void deleteByDocumentId(String documentId);

    /**
     * 删除chunk下所有向量。
     *
     * @param chunkId chunk ID
     */
    void deleteByChunkId(String chunkId);

    /**
     * 删除chunk_index对应向量。
     *
     * @param indexId chunk_index ID
     */
    void deleteByIndexId(String indexId);

    /**
     * 重新计算文档embedding状态。
     *
     * @param documentId 文档ID
     */
    void refreshDocumentEmbedStatus(String documentId);

    /**
     * 重置超时的processing数据为pending。
     *
     * @param kbId 知识库ID
     */
    void resetTimeoutProcessing(String kbId);
}
