package org.jeecg.modules.airag.kb.service;

import org.jeecg.modules.airag.kb.vo.EmbeddingResultVO;

import java.util.List;

/**
 * embedding模型服务。
 */
public interface EmbeddingService {
    /**
     * 对单段文本生成向量。
     *
     * @param text 文本
     * @return embedding结果
     */
    EmbeddingResultVO embed(String text);

    /**
     * 批量生成向量。
     *
     * @param texts 文本列表
     * @return embedding结果列表
     */
    List<EmbeddingResultVO> embedBatch(List<String> texts);
}
