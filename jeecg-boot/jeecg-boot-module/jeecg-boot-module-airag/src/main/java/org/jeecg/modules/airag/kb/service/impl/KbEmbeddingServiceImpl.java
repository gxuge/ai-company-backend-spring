package org.jeecg.modules.airag.kb.service.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.common.util.oConvertUtils;
import org.jeecg.modules.airag.kb.consts.KbConstants;
import org.jeecg.modules.airag.kb.config.KbEmbeddingProperties;
import org.jeecg.modules.airag.kb.entity.KbBase;
import org.jeecg.modules.airag.kb.entity.KbChunk;
import org.jeecg.modules.airag.kb.entity.KbChunkIndex;
import org.jeecg.modules.airag.kb.entity.KbDocument;
import org.jeecg.modules.airag.kb.mapper.KbBaseMapper;
import org.jeecg.modules.airag.kb.mapper.KbChunkIndexMapper;
import org.jeecg.modules.airag.kb.mapper.KbChunkMapper;
import org.jeecg.modules.airag.kb.mapper.KbDocumentMapper;
import org.jeecg.modules.airag.kb.service.KbEmbeddingService;
import org.jeecg.modules.airag.kb.service.EmbeddingService;
import org.jeecg.modules.airag.kb.service.VectorStoreService;
import org.jeecg.modules.airag.kb.vo.EmbeddingBatchResultVO;
import org.jeecg.modules.airag.kb.vo.EmbeddingItemResultVO;
import org.jeecg.modules.airag.kb.vo.EmbeddingResultVO;
import org.jeecg.modules.airag.kb.vo.EmbeddingStatusVO;
import org.jeecg.modules.airag.kb.vo.VectorStoreMetadataVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * KB embedding编排服务实现。
 */
@Service
public class KbEmbeddingServiceImpl implements KbEmbeddingService {
    /**
     * 知识库主表Mapper。
     */
    private final KbBaseMapper kbBaseMapper;

    /**
     * 文档Mapper。
     */
    private final KbDocumentMapper kbDocumentMapper;

    /**
     * chunk Mapper。
     */
    private final KbChunkMapper kbChunkMapper;

    /**
     * chunk_index Mapper。
     */
    private final KbChunkIndexMapper kbChunkIndexMapper;

    /**
     * embedding模型服务。
     */
    private final EmbeddingService embeddingService;

    /**
     * 向量库服务。
     */
    private final VectorStoreService vectorStoreService;

    /**
     * embedding配置。
     */
    private final KbEmbeddingProperties properties;

    /**
     * 需要独立提交的事务模板。
     */
    private final TransactionTemplate requiresNewTemplate;

    /**
     * 构造方法。
     *
     * @param kbBaseMapper 知识库主表Mapper
     * @param kbDocumentMapper 文档Mapper
     * @param kbChunkMapper chunk Mapper
     * @param kbChunkIndexMapper chunk_index Mapper
     * @param embeddingService embedding模型服务
     * @param vectorStoreService 向量库服务
     * @param transactionManager 事务管理器
     */
    public KbEmbeddingServiceImpl(KbBaseMapper kbBaseMapper,
                                  KbDocumentMapper kbDocumentMapper,
                                  KbChunkMapper kbChunkMapper,
                                  KbChunkIndexMapper kbChunkIndexMapper,
                                  EmbeddingService embeddingService,
                                  VectorStoreService vectorStoreService,
                                  KbEmbeddingProperties properties,
                                  PlatformTransactionManager transactionManager) {
        this.kbBaseMapper = kbBaseMapper;
        this.kbDocumentMapper = kbDocumentMapper;
        this.kbChunkMapper = kbChunkMapper;
        this.kbChunkIndexMapper = kbChunkIndexMapper;
        this.embeddingService = embeddingService;
        this.vectorStoreService = vectorStoreService;
        this.properties = properties;
        this.requiresNewTemplate = new TransactionTemplate(transactionManager);
        this.requiresNewTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    /**
     * 对整个知识库执行embedding。
     *
     * @param kbId 知识库ID
     * @param overrideSuccess 是否覆盖成功数据
     * @return 处理结果
     */
    @Override
    public EmbeddingBatchResultVO embedKb(String kbId, boolean overrideSuccess) {
        KbBase kb = ensureKbEnabled(kbId);
        resetTimeoutProcessing(kbId);
        List<KbChunkIndex> indexes = kbChunkIndexMapper.selectList(new LambdaQueryWrapper<KbChunkIndex>()
                .eq(KbChunkIndex::getKbId, kbId)
                .eq(KbChunkIndex::getStatus, KbConstants.STATUS_ENABLE));
        return processBatch(indexes, overrideSuccess, kb.getId());
    }

    /**
     * 对单个文档执行embedding。
     *
     * @param documentId 文档ID
     * @param overrideSuccess 是否覆盖成功数据
     * @return 处理结果
     */
    @Override
    public EmbeddingBatchResultVO embedDocument(String documentId, boolean overrideSuccess) {
        KbDocument document = ensureDocumentEnabled(documentId);
        resetTimeoutProcessing(document.getKbId());
        List<KbChunk> chunks = kbChunkMapper.selectList(new LambdaQueryWrapper<KbChunk>()
                .eq(KbChunk::getDocumentId, documentId)
                .eq(KbChunk::getStatus, KbConstants.STATUS_ENABLE));
        if (chunks.isEmpty()) {
            return emptyBatch();
        }
        List<String> chunkIds = chunks.stream().map(KbChunk::getId).collect(Collectors.toList());
        List<KbChunkIndex> indexes = kbChunkIndexMapper.selectList(new LambdaQueryWrapper<KbChunkIndex>()
                .in(KbChunkIndex::getChunkId, chunkIds)
                .eq(KbChunkIndex::getStatus, KbConstants.STATUS_ENABLE));
        return processBatch(indexes, overrideSuccess, document.getKbId());
    }

    /**
     * 对单个chunk执行embedding。
     *
     * @param chunkId chunk ID
     * @param overrideSuccess 是否覆盖成功数据
     * @return 处理结果
     */
    @Override
    public EmbeddingBatchResultVO embedChunk(String chunkId, boolean overrideSuccess) {
        KbChunk chunk = ensureChunkEnabled(chunkId);
        resetTimeoutProcessing(chunk.getKbId());
        List<KbChunkIndex> indexes = kbChunkIndexMapper.selectList(new LambdaQueryWrapper<KbChunkIndex>()
                .eq(KbChunkIndex::getChunkId, chunkId)
                .eq(KbChunkIndex::getStatus, KbConstants.STATUS_ENABLE));
        return processBatch(indexes, overrideSuccess, chunk.getKbId());
    }

    /**
     * 对单个chunk_index执行embedding。
     *
     * @param indexId chunk_index ID
     * @param overrideSuccess 是否覆盖成功数据
     * @return 处理结果
     */
    @Override
    public EmbeddingBatchResultVO embedIndex(String indexId, boolean overrideSuccess) {
        KbChunkIndex index = ensureIndexEnabled(indexId);
        resetTimeoutProcessing(index.getKbId());
        EmbeddingBatchResultVO batchResult = new EmbeddingBatchResultVO();
        EmbeddingItemResultVO itemResult = processSingleIndex(index, overrideSuccess);
        batchResult.setTotalCount(1);
        batchResult.setSuccessCount("success".equals(itemResult.getStatus()) ? 1 : 0);
        batchResult.setFailedCount("failed".equals(itemResult.getStatus()) ? 1 : 0);
        batchResult.setSkippedCount("skipped".equals(itemResult.getStatus()) ? 1 : 0);
        batchResult.getItemList().add(itemResult);
        return batchResult;
    }

    /**
     * 查询知识库embedding进度。
     *
     * @param kbId 知识库ID
     * @return 统计结果
     */
    @Override
    public EmbeddingStatusVO getStatus(String kbId) {
        ensureKbEnabled(kbId);
        LambdaQueryWrapper<KbChunkIndex> wrapper = new LambdaQueryWrapper<KbChunkIndex>()
                .eq(KbChunkIndex::getKbId, kbId)
                .eq(KbChunkIndex::getStatus, KbConstants.STATUS_ENABLE);
        long total = kbChunkIndexMapper.selectCount(wrapper);
        long pending = kbChunkIndexMapper.selectCount(new LambdaQueryWrapper<KbChunkIndex>().eq(KbChunkIndex::getKbId, kbId)
                .eq(KbChunkIndex::getStatus, KbConstants.STATUS_ENABLE)
                .eq(KbChunkIndex::getEmbeddingStatus, KbConstants.PROCESS_STATUS_PENDING));
        long processing = kbChunkIndexMapper.selectCount(new LambdaQueryWrapper<KbChunkIndex>().eq(KbChunkIndex::getKbId, kbId)
                .eq(KbChunkIndex::getStatus, KbConstants.STATUS_ENABLE)
                .eq(KbChunkIndex::getEmbeddingStatus, KbConstants.PROCESS_STATUS_PROCESSING));
        long success = kbChunkIndexMapper.selectCount(new LambdaQueryWrapper<KbChunkIndex>().eq(KbChunkIndex::getKbId, kbId)
                .eq(KbChunkIndex::getStatus, KbConstants.STATUS_ENABLE)
                .eq(KbChunkIndex::getEmbeddingStatus, KbConstants.PROCESS_STATUS_SUCCESS));
        long failed = kbChunkIndexMapper.selectCount(new LambdaQueryWrapper<KbChunkIndex>().eq(KbChunkIndex::getKbId, kbId)
                .eq(KbChunkIndex::getStatus, KbConstants.STATUS_ENABLE)
                .eq(KbChunkIndex::getEmbeddingStatus, KbConstants.PROCESS_STATUS_FAILED));
        EmbeddingStatusVO vo = new EmbeddingStatusVO();
        vo.setTotal((int) total);
        vo.setPending((int) pending);
        vo.setProcessing((int) processing);
        vo.setSuccess((int) success);
        vo.setFailed((int) failed);
        return vo;
    }

    /**
     * 删除知识库下所有向量。
     *
     * @param kbId 知识库ID
     */
    @Override
    public void deleteByKbId(String kbId) {
        vectorStoreService.deleteByKbId(kbId);
    }

    /**
     * 删除文档下所有向量。
     *
     * @param documentId 文档ID
     */
    @Override
    public void deleteByDocumentId(String documentId) {
        vectorStoreService.deleteByDocumentId(documentId);
    }

    /**
     * 删除chunk下所有向量。
     *
     * @param chunkId chunk ID
     */
    @Override
    public void deleteByChunkId(String chunkId) {
        vectorStoreService.deleteByChunkId(chunkId);
    }

    /**
     * 删除chunk_index对应向量。
     *
     * @param indexId chunk_index ID
     */
    @Override
    public void deleteByIndexId(String indexId) {
        vectorStoreService.deleteByChunkIndexId(indexId);
    }

    /**
     * 重新计算文档embedding状态。
     *
     * @param documentId 文档ID
     */
    @Override
    public void refreshDocumentEmbedStatus(String documentId) {
        KbDocument document = kbDocumentMapper.selectById(documentId);
        if (document == null || KbConstants.STATUS_DISABLE.equals(document.getStatus())) {
            return;
        }
        List<KbChunk> chunks = kbChunkMapper.selectList(new LambdaQueryWrapper<KbChunk>()
                .eq(KbChunk::getDocumentId, documentId)
                .eq(KbChunk::getStatus, KbConstants.STATUS_ENABLE));
        if (chunks.isEmpty()) {
            updateDocumentEmbedStatus(documentId, KbConstants.PROCESS_STATUS_PENDING);
            return;
        }
        List<String> chunkIds = chunks.stream().map(KbChunk::getId).collect(Collectors.toList());
        List<KbChunkIndex> indexes = kbChunkIndexMapper.selectList(new LambdaQueryWrapper<KbChunkIndex>()
                .in(KbChunkIndex::getChunkId, chunkIds)
                .eq(KbChunkIndex::getStatus, KbConstants.STATUS_ENABLE));
        if (indexes.isEmpty()) {
            updateDocumentEmbedStatus(documentId, KbConstants.PROCESS_STATUS_PENDING);
            return;
        }
        boolean hasProcessing = false;
        boolean hasPending = false;
        boolean hasFailed = false;
        boolean hasSuccess = false;
        for (KbChunkIndex index : indexes) {
            if (KbConstants.PROCESS_STATUS_PROCESSING.equals(index.getEmbeddingStatus())) {
                hasProcessing = true;
            } else if (KbConstants.PROCESS_STATUS_PENDING.equals(index.getEmbeddingStatus())) {
                hasPending = true;
            } else if (KbConstants.PROCESS_STATUS_FAILED.equals(index.getEmbeddingStatus())) {
                hasFailed = true;
            } else if (KbConstants.PROCESS_STATUS_SUCCESS.equals(index.getEmbeddingStatus())) {
                hasSuccess = true;
            }
        }
        String status;
        if (hasProcessing || hasPending) {
            status = hasProcessing ? KbConstants.PROCESS_STATUS_PROCESSING : KbConstants.PROCESS_STATUS_PENDING;
        } else if (hasFailed && hasSuccess) {
            status = KbConstants.PROCESS_STATUS_FAILED;
        } else if (hasFailed) {
            status = KbConstants.PROCESS_STATUS_FAILED;
        } else if (hasSuccess) {
            status = KbConstants.PROCESS_STATUS_SUCCESS;
        } else {
            status = KbConstants.PROCESS_STATUS_PENDING;
        }
        updateDocumentEmbedStatus(documentId, status);
    }

    /**
     * 重置超时的processing数据为pending。
     *
     * @param kbId 知识库ID
     */
    @Override
    public void resetTimeoutProcessing(String kbId) {
        Date threshold = new Date(System.currentTimeMillis() - safeTimeoutMinutes() * 60_000L);
        kbChunkIndexMapper.update(null, new LambdaUpdateWrapper<KbChunkIndex>()
                .eq(KbChunkIndex::getKbId, kbId)
                .eq(KbChunkIndex::getStatus, KbConstants.STATUS_ENABLE)
                .eq(KbChunkIndex::getEmbeddingStatus, KbConstants.PROCESS_STATUS_PROCESSING)
                .lt(KbChunkIndex::getUpdatedAt, threshold)
                .set(KbChunkIndex::getEmbeddingStatus, KbConstants.PROCESS_STATUS_PENDING)
                .set(KbChunkIndex::getUpdatedAt, new Date()));
    }

    /**
     * 处理批量embedding。
     *
     * @param indexes 索引列表
     * @param overrideSuccess 是否覆盖成功
     * @param kbId 知识库ID
     * @return 结果
     */
    private EmbeddingBatchResultVO processBatch(List<KbChunkIndex> indexes, boolean overrideSuccess, String kbId) {
        EmbeddingBatchResultVO result = new EmbeddingBatchResultVO();
        if (indexes == null || indexes.isEmpty()) {
            return emptyBatch();
        }
        int success = 0;
        int failed = 0;
        int skipped = 0;
        for (KbChunkIndex index : indexes) {
            if (!overrideSuccess && KbConstants.PROCESS_STATUS_SUCCESS.equals(index.getEmbeddingStatus())) {
                EmbeddingItemResultVO skip = buildSkipResult(index, "已存在成功向量，已跳过");
                result.getItemList().add(skip);
                skipped++;
                continue;
            }
            EmbeddingItemResultVO item = processSingleIndex(index, overrideSuccess);
            result.getItemList().add(item);
            if ("success".equals(item.getStatus())) {
                success++;
            } else if ("failed".equals(item.getStatus())) {
                failed++;
            } else {
                skipped++;
            }
        }
        result.setTotalCount(result.getItemList().size());
        result.setSuccessCount(success);
        result.setFailedCount(failed);
        result.setSkippedCount(skipped);
        return result;
    }

    /**
     * 处理单个chunk_index embedding。
     *
     * @param index 索引实体
     * @param forceOverride 是否强制覆盖
     * @return 处理结果
     */
    private EmbeddingItemResultVO processSingleIndex(KbChunkIndex index, boolean forceOverride) {
        return requiresNewTemplate.execute(status -> {
            KbChunkIndex current = kbChunkIndexMapper.selectById(index.getId());
            EmbeddingItemResultVO itemResult = new EmbeddingItemResultVO();
            itemResult.setKbId(index.getKbId());
            itemResult.setChunkId(index.getChunkId());
            itemResult.setChunkIndexId(index.getId());
            itemResult.setVectorId(index.getId());
            if (current == null || KbConstants.STATUS_DISABLE.equals(current.getStatus())) {
                itemResult.setStatus("failed");
                itemResult.setErrorMessage("未找到对应chunk_index");
                return itemResult;
            }
            if (!forceOverride && KbConstants.PROCESS_STATUS_SUCCESS.equals(current.getEmbeddingStatus())) {
                itemResult.setStatus("skipped");
                itemResult.setErrorMessage("已存在成功向量，已跳过");
                return itemResult;
            }
            KbChunk chunk = kbChunkMapper.selectById(current.getChunkId());
            if (chunk == null || KbConstants.STATUS_DISABLE.equals(chunk.getStatus()) || !current.getKbId().equals(chunk.getKbId())) {
                itemResult.setStatus("failed");
                itemResult.setErrorMessage("chunk无效或不属于当前知识库");
                markIndexFailed(current.getId(), itemResult.getErrorMessage());
                refreshDocumentEmbedStatus(resolveDocumentId(current.getChunkId()));
                return itemResult;
            }
            KbDocument document = kbDocumentMapper.selectById(chunk.getDocumentId());
            if (document == null || KbConstants.STATUS_DISABLE.equals(document.getStatus()) || !current.getKbId().equals(document.getKbId())) {
                itemResult.setStatus("failed");
                itemResult.setErrorMessage("文档无效或不属于当前知识库");
                markIndexFailed(current.getId(), itemResult.getErrorMessage());
                refreshDocumentEmbedStatus(chunk.getDocumentId());
                return itemResult;
            }
            KbBase kb = kbBaseMapper.selectById(current.getKbId());
            if (kb == null || KbConstants.STATUS_DISABLE.equals(kb.getStatus())) {
                itemResult.setStatus("failed");
                itemResult.setErrorMessage("知识库无效或已禁用");
                markIndexFailed(current.getId(), itemResult.getErrorMessage());
                return itemResult;
            }
            String text = current.getIndexText();
            if (oConvertUtils.isEmpty(text)) {
                itemResult.setStatus("failed");
                itemResult.setErrorMessage("index_text不能为空");
                markIndexFailed(current.getId(), itemResult.getErrorMessage());
                refreshDocumentEmbedStatus(document.getId());
                return itemResult;
            }
            Date now = new Date();
            markIndexProcessing(current.getId(), now);
            try {
                EmbeddingResultVO embeddingResult = embeddingService.embed(text);
                VectorStoreMetadataVO metadata = new VectorStoreMetadataVO();
                metadata.setKbId(current.getKbId());
                metadata.setDocumentId(document.getId());
                metadata.setChunkId(chunk.getId());
                metadata.setChunkIndexId(current.getId());
                metadata.setIndexType(current.getIndexType());
                metadata.setSourceType(document.getSourceType());
                metadata.setFileType(document.getFileType());
                metadata.setContentPreview(buildContentPreview(text));
                metadata.setEmbeddingModel(embeddingResult.getModelName());
                metadata.setVectorDimension(embeddingResult.getVectorDimension());
                metadata.setEmbeddingDurationMs(embeddingResult.getDurationMs());
                metadata.setTruncated(embeddingResult.getTruncated());
                metadata.setMetadataJson(buildEmbeddingMetadata(current, document, chunk, embeddingResult));
                vectorStoreService.upsert(current.getId(), embeddingResult.getVector(), metadata);
                markIndexSuccess(current.getId(), embeddingResult, metadata.getMetadataJson(), now);
                refreshDocumentEmbedStatus(document.getId());
                itemResult.setStatus("success");
                itemResult.setModelName(embeddingResult.getModelName());
                itemResult.setVectorDimension(embeddingResult.getVectorDimension());
                itemResult.setDurationMs(embeddingResult.getDurationMs());
                return itemResult;
            } catch (Exception e) {
                vectorStoreService.deleteByChunkIndexId(current.getId());
                markIndexFailed(current.getId(), e.getMessage());
                refreshDocumentEmbedStatus(document.getId());
                itemResult.setStatus("failed");
                itemResult.setErrorMessage(e.getMessage());
                return itemResult;
            }
        });
    }

    /**
     * 生成跳过结果。
     *
     * @param index 索引实体
     * @param message 提示信息
     * @return 结果
     */
    private EmbeddingItemResultVO buildSkipResult(KbChunkIndex index, String message) {
        EmbeddingItemResultVO result = new EmbeddingItemResultVO();
        result.setStatus("skipped");
        result.setKbId(index.getKbId());
        result.setChunkId(index.getChunkId());
        result.setChunkIndexId(index.getId());
        result.setVectorId(index.getId());
        result.setErrorMessage(message);
        return result;
    }

    /**
     * 标记索引处理中。
     *
     * @param indexId 索引ID
     * @param now 当前时间
     */
    private void markIndexProcessing(String indexId, Date now) {
        kbChunkIndexMapper.update(null, new LambdaUpdateWrapper<KbChunkIndex>()
                .eq(KbChunkIndex::getId, indexId)
                .set(KbChunkIndex::getEmbeddingStatus, KbConstants.PROCESS_STATUS_PROCESSING)
                .set(KbChunkIndex::getUpdatedAt, now));
    }

    /**
     * 标记索引成功。
     *
     * @param indexId 索引ID
     * @param embeddingResult embedding结果
     * @param metadataJson 元数据JSON
     * @param now 当前时间
     */
    private void markIndexSuccess(String indexId, EmbeddingResultVO embeddingResult, String metadataJson, Date now) {
        KbChunkIndex update = new KbChunkIndex();
        update.setId(indexId);
        update.setEmbeddingStatus(KbConstants.PROCESS_STATUS_SUCCESS);
        update.setMetadataJson(metadataJson);
        update.setUpdatedAt(now);
        kbChunkIndexMapper.updateById(update);
    }

    /**
     * 标记索引失败。
     *
     * @param indexId 索引ID
     * @param errorMessage 错误信息
     */
    private void markIndexFailed(String indexId, String errorMessage) {
        KbChunkIndex index = kbChunkIndexMapper.selectById(indexId);
        if (index == null) {
            return;
        }
        JSONObject metadata = parseMetadata(index.getMetadataJson());
        metadata.put("embeddingStatus", KbConstants.PROCESS_STATUS_FAILED);
        metadata.put("embeddingError", errorMessage);
        metadata.put("embeddingFailedAt", new Date());
        KbChunkIndex update = new KbChunkIndex();
        update.setId(indexId);
        update.setEmbeddingStatus(KbConstants.PROCESS_STATUS_FAILED);
        update.setMetadataJson(metadata.toJSONString());
        update.setUpdatedAt(new Date());
        kbChunkIndexMapper.updateById(update);
    }

    /**
     * 构建embedding元数据。
     *
     * @param index 索引实体
     * @param document 文档实体
     * @param chunk chunk实体
     * @param embeddingResult embedding结果
     * @return JSON字符串
     */
    private String buildEmbeddingMetadata(KbChunkIndex index, KbDocument document, KbChunk chunk, EmbeddingResultVO embeddingResult) {
        JSONObject json = parseMetadata(index.getMetadataJson());
        json.put("embeddingStatus", KbConstants.PROCESS_STATUS_SUCCESS);
        json.put("embeddingModel", embeddingResult.getModelName());
        json.put("embeddingDimension", embeddingResult.getVectorDimension());
        json.put("embeddingDurationMs", embeddingResult.getDurationMs());
        json.put("vectorId", index.getId());
        json.put("contentPreview", buildContentPreview(index.getIndexText()));
        json.put("embeddingSucceededAt", new Date());
        json.put("kbId", document.getKbId());
        json.put("documentId", document.getId());
        json.put("chunkId", chunk.getId());
        json.put("chunkIndexId", index.getId());
        return json.toJSONString();
    }

    /**
     * 构建内容预览。
     *
     * @param text 文本
     * @return 预览
     */
    private String buildContentPreview(String text) {
        if (text == null) {
            return null;
        }
        String value = text.trim();
        return value.length() <= 200 ? value : value.substring(0, 200);
    }

    /**
     * 解析元数据。
     *
     * @param metadataJson 元数据JSON
     * @return JSONObject
     */
    private JSONObject parseMetadata(String metadataJson) {
        if (oConvertUtils.isEmpty(metadataJson)) {
            return new JSONObject();
        }
        try {
            return JSON.parseObject(metadataJson);
        } catch (Exception e) {
            return new JSONObject();
        }
    }

    /**
     * 计算文档ID。
     *
     * @param chunkId chunk ID
     * @return 文档ID
     */
    private String resolveDocumentId(String chunkId) {
        KbChunk chunk = kbChunkMapper.selectById(chunkId);
        return chunk == null ? null : chunk.getDocumentId();
    }

    /**
     * 更新文档embedding状态。
     *
     * @param documentId 文档ID
     */
    private void updateDocumentEmbedStatus(String documentId, String status) {
        if (oConvertUtils.isEmpty(documentId)) {
            return;
        }
        KbDocument update = new KbDocument();
        update.setId(documentId);
        update.setEmbedStatus(status);
        update.setUpdatedAt(new Date());
        kbDocumentMapper.updateById(update);
    }

    /**
     * 确保知识库存在且启用。
     *
     * @param kbId 知识库ID
     * @return 知识库实体
     */
    private KbBase ensureKbEnabled(String kbId) {
        KbBase kb = kbBaseMapper.selectById(kbId);
        if (kb == null || KbConstants.STATUS_DISABLE.equals(kb.getStatus())) {
            throw new JeecgBootException("未找到对应知识库");
        }
        return kb;
    }

    /**
     * 确保文档存在且启用。
     *
     * @param documentId 文档ID
     * @return 文档实体
     */
    private KbDocument ensureDocumentEnabled(String documentId) {
        KbDocument document = kbDocumentMapper.selectById(documentId);
        if (document == null || KbConstants.STATUS_DISABLE.equals(document.getStatus())) {
            throw new JeecgBootException("未找到对应文档");
        }
        return document;
    }

    /**
     * 确保chunk存在且启用。
     *
     * @param chunkId chunk ID
     * @return chunk实体
     */
    private KbChunk ensureChunkEnabled(String chunkId) {
        KbChunk chunk = kbChunkMapper.selectById(chunkId);
        if (chunk == null || KbConstants.STATUS_DISABLE.equals(chunk.getStatus())) {
            throw new JeecgBootException("未找到对应chunk");
        }
        return chunk;
    }

    /**
     * 确保chunk_index存在且启用。
     *
     * @param indexId chunk_index ID
     * @return 索引实体
     */
    private KbChunkIndex ensureIndexEnabled(String indexId) {
        KbChunkIndex index = kbChunkIndexMapper.selectById(indexId);
        if (index == null || KbConstants.STATUS_DISABLE.equals(index.getStatus())) {
            throw new JeecgBootException("未找到对应chunk_index");
        }
        return index;
    }

    /**
     * 解析超时分钟数。
     *
     * @return 分钟数
     */
    private long safeTimeoutMinutes() {
        Integer timeout = properties.getProcessingTimeoutMinutes();
        return timeout == null || timeout < 1 ? 30L : timeout.longValue();
    }

    /**
     * 返回空批处理结果。
     *
     * @return 结果
     */
    private EmbeddingBatchResultVO emptyBatch() {
        EmbeddingBatchResultVO result = new EmbeddingBatchResultVO();
        result.setTotalCount(0);
        result.setSuccessCount(0);
        result.setFailedCount(0);
        result.setSkippedCount(0);
        return result;
    }
}
