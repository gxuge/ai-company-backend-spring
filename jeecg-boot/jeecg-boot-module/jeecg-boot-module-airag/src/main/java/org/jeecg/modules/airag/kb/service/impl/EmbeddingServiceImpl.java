package org.jeecg.modules.airag.kb.service.impl;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.embedding.EmbeddingModel;
import org.jeecg.ai.factory.AiModelFactory;
import org.jeecg.ai.factory.AiModelOptions;
import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.common.util.oConvertUtils;
import org.jeecg.modules.airag.kb.config.KbEmbeddingProperties;
import org.jeecg.modules.airag.kb.service.EmbeddingService;
import org.jeecg.modules.airag.kb.vo.EmbeddingResultVO;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;

/**
 * embedding模型服务实现。
 */
@Service
public class EmbeddingServiceImpl implements EmbeddingService {
    /**
     * embedding配置。
     */
    private final KbEmbeddingProperties properties;

    /**
     * 真实embedding模型，未配置时为null。
     */
    private volatile EmbeddingModel embeddingModel;

    /**
     * 构造方法。
     *
     * @param properties embedding配置
     */
    public EmbeddingServiceImpl(KbEmbeddingProperties properties) {
        this.properties = properties;
    }

    /**
     * 单条embedding。
     *
     * @param text 文本
     * @return embedding结果
     */
    @Override
    public EmbeddingResultVO embed(String text) {
        String normalized = normalizeText(text);
        long start = System.currentTimeMillis();
        if (useRealModel()) {
            EmbeddingModel model = resolveEmbeddingModel();
            Embedding embedding = model.embed(normalized).content();
            EmbeddingResultVO vo = new EmbeddingResultVO();
            vo.setVector(embedding.vectorAsList());
            vo.setModelName(model.modelName());
            vo.setVectorDimension(embedding.dimension());
            vo.setDurationMs(System.currentTimeMillis() - start);
            vo.setFallback(Boolean.FALSE);
            vo.setTruncated(Boolean.valueOf(isTruncated(text)));
            return vo;
        }
        EmbeddingResultVO vo = new EmbeddingResultVO();
        List<Float> vector = buildFallbackVector(normalized, resolveFallbackDimension());
        vo.setVector(vector);
        vo.setModelName("hash-embedding");
        vo.setVectorDimension(vector.size());
        vo.setDurationMs(System.currentTimeMillis() - start);
        vo.setFallback(Boolean.TRUE);
        vo.setTruncated(Boolean.valueOf(isTruncated(text)));
        return vo;
    }

    /**
     * 批量embedding。
     *
     * @param texts 文本列表
     * @return 结果列表
     */
    @Override
    public List<EmbeddingResultVO> embedBatch(List<String> texts) {
        List<EmbeddingResultVO> result = new ArrayList<>();
        if (texts == null || texts.isEmpty()) {
            return result;
        }
        for (String text : texts) {
            result.add(embed(text));
        }
        return result;
    }

    /**
     * 判断是否使用真实模型。
     *
     * @return 是否使用真实模型
     */
    private boolean useRealModel() {
        return oConvertUtils.isNotEmpty(properties.getProvider())
                && oConvertUtils.isNotEmpty(properties.getModelName())
                && (oConvertUtils.isNotEmpty(properties.getBaseUrl())
                || oConvertUtils.isNotEmpty(properties.getApiKey())
                || oConvertUtils.isNotEmpty(properties.getSecretKey()));
    }

    /**
     * 解析真实embedding模型。
     *
     * @return embedding模型
     */
    private EmbeddingModel resolveEmbeddingModel() {
        if (embeddingModel != null) {
            return embeddingModel;
        }
        synchronized (this) {
            if (embeddingModel == null) {
                try {
                    AiModelOptions.AiModelOptionsBuilder builder = AiModelOptions.builder()
                            .provider(properties.getProvider())
                            .modelName(properties.getModelName())
                            .baseUrl(properties.getBaseUrl());
                    if (oConvertUtils.isNotEmpty(properties.getApiKey())) {
                        builder.apiKey(properties.getApiKey());
                    }
                    if (oConvertUtils.isNotEmpty(properties.getSecretKey())) {
                        builder.secretKey(properties.getSecretKey());
                    }
                    embeddingModel = AiModelFactory.createEmbeddingModel(builder.build());
                } catch (Exception e) {
                    throw new JeecgBootException("初始化embedding模型失败：" + e.getMessage());
                }
            }
        }
        return embeddingModel;
    }

    /**
     * 规范化文本并按配置截断。
     *
     * @param text 原始文本
     * @return 规范化文本
     */
    private String normalizeText(String text) {
        if (oConvertUtils.isEmpty(text)) {
            throw new JeecgBootException("index_text不能为空");
        }
        String normalized = text.trim();
        Integer maxTextLength = properties.getMaxTextLength();
        if (maxTextLength != null && maxTextLength > 0 && normalized.length() > maxTextLength) {
            return normalized.substring(0, maxTextLength);
        }
        return normalized;
    }

    /**
     * 判断是否被截断。
     *
     * @param original 原始文本
     * @return 是否截断
     */
    private boolean isTruncated(String original) {
        if (original == null) {
            return false;
        }
        Integer maxTextLength = properties.getMaxTextLength();
        return maxTextLength != null && maxTextLength > 0 && original.trim().length() > maxTextLength;
    }

    /**
     * 构建回退向量。
     *
     * @param text 文本
     * @param dimension 维度
     * @return 向量
     */
    private List<Float> buildFallbackVector(String text, int dimension) {
        List<Float> vector = new ArrayList<>(dimension);
        byte[] seed;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            seed = digest.digest(text.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            seed = text.getBytes(StandardCharsets.UTF_8);
        }
        for (int i = 0; i < dimension; i++) {
            int left = seed[i % seed.length] & 0xFF;
            int right = seed[(i * 7) % seed.length] & 0xFF;
            float value = ((left << 8) + right) / 65535.0f;
            vector.add(value);
        }
        return vector;
    }

    /**
     * 解析回退维度。
     *
     * @return 维度
     */
    private int resolveFallbackDimension() {
        Integer dimension = properties.getFallbackDimension();
        return dimension == null || dimension < 1 ? 256 : dimension;
    }
}
