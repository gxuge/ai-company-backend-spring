package org.jeecg.modules.airag.kb.service;

import org.jeecg.modules.airag.kb.vo.VectorStoreMetadataVO;

import java.util.List;

/**
 * 向量库服务抽象。
 */
public interface VectorStoreService {
    /**
     * 写入或覆盖向量。
     *
     * @param vectorId 向量ID
     * @param vector 向量值
     * @param metadata 元数据
     */
    void upsert(String vectorId, List<Float> vector, VectorStoreMetadataVO metadata);

    /**
     * 删除单个向量。
     *
     * @param vectorId 向量ID
     */
    void delete(String vectorId);

    /**
     * 按知识库删除向量。
     *
     * @param kbId 知识库ID
     */
    void deleteByKbId(String kbId);

    /**
     * 按文档删除向量。
     *
     * @param documentId 文档ID
     */
    void deleteByDocumentId(String documentId);

    /**
     * 按chunk删除向量。
     *
     * @param chunkId chunk ID
     */
    void deleteByChunkId(String chunkId);

    /**
     * 按chunk_index删除向量。
     *
     * @param chunkIndexId chunk_index ID
     */
    void deleteByChunkIndexId(String chunkIndexId);
}
