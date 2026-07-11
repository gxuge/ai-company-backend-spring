package org.jeecg.modules.airag.kb.service.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.common.util.oConvertUtils;
import org.jeecg.modules.airag.kb.consts.KbConstants;
import org.jeecg.modules.airag.kb.dto.KbQueryOptimizationHistoryDTO;
import org.jeecg.modules.airag.kb.dto.KbSemanticSearchQueryDTO;
import org.jeecg.modules.airag.kb.entity.KbBase;
import org.jeecg.modules.airag.kb.entity.KbChunk;
import org.jeecg.modules.airag.kb.entity.KbChunkIndex;
import org.jeecg.modules.airag.kb.entity.KbDocument;
import org.jeecg.modules.airag.kb.entity.KbVectorRecord;
import org.jeecg.modules.airag.kb.mapper.KbBaseMapper;
import org.jeecg.modules.airag.kb.mapper.KbChunkIndexMapper;
import org.jeecg.modules.airag.kb.mapper.KbChunkMapper;
import org.jeecg.modules.airag.kb.mapper.KbDocumentMapper;
import org.jeecg.modules.airag.kb.mapper.KbVectorRecordMapper;
import org.jeecg.modules.airag.kb.service.EmbeddingService;
import org.jeecg.modules.airag.kb.service.IKbSearchConfigService;
import org.jeecg.modules.airag.kb.service.IKbSemanticSearchService;
import org.jeecg.modules.airag.kb.vo.EmbeddingResultVO;
import org.jeecg.modules.airag.kb.vo.KbSearchConfigVo;
import org.jeecg.modules.airag.kb.vo.KbSemanticSearchItemVO;
import org.jeecg.modules.airag.kb.vo.KbSemanticSearchResultVO;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.concurrent.TimeUnit;

/**
 * 知识库语义检索服务实现。
 */
@Service
public class KbSemanticSearchServiceImpl implements IKbSemanticSearchService {
    /**
     * 最近一次检索快照。
     */
    private static final ThreadLocal<SearchTraceContext> SEARCH_TRACE_HOLDER = new ThreadLocal<>();

    /**
     * 最近一次检索debug收集器。
     */
    private static final ThreadLocal<RetrievalDebugCollector> DEBUG_COLLECTOR_HOLDER = new ThreadLocal<>();

    /**
     * 知识库主表Mapper。
     */
    private final KbBaseMapper kbBaseMapper;

    /**
     * 检索配置服务。
     */
    private final IKbSearchConfigService kbSearchConfigService;

    /**
     * 向量记录Mapper。
     */
    private final KbVectorRecordMapper kbVectorRecordMapper;

    /**
     * chunk索引Mapper。
     */
    private final KbChunkIndexMapper kbChunkIndexMapper;

    /**
     * chunk Mapper。
     */
    private final KbChunkMapper kbChunkMapper;

    /**
     * 文档Mapper。
     */
    private final KbDocumentMapper kbDocumentMapper;

    /**
     * embedding模型服务。
     */
    private final EmbeddingService embeddingService;

    /**
     * 构造方法。
     *
     * @param kbBaseMapper 知识库主表Mapper
     * @param kbSearchConfigService 检索配置服务
     * @param kbVectorRecordMapper 向量记录Mapper
     * @param kbChunkIndexMapper chunk索引Mapper
     * @param kbChunkMapper chunk Mapper
     * @param kbDocumentMapper 文档Mapper
     * @param embeddingService embedding模型服务
     */
    public KbSemanticSearchServiceImpl(KbBaseMapper kbBaseMapper,
                                       IKbSearchConfigService kbSearchConfigService,
                                       KbVectorRecordMapper kbVectorRecordMapper,
                                       KbChunkIndexMapper kbChunkIndexMapper,
                                       KbChunkMapper kbChunkMapper,
                                       KbDocumentMapper kbDocumentMapper,
                                       EmbeddingService embeddingService) {
        this.kbBaseMapper = kbBaseMapper;
        this.kbSearchConfigService = kbSearchConfigService;
        this.kbVectorRecordMapper = kbVectorRecordMapper;
        this.kbChunkIndexMapper = kbChunkIndexMapper;
        this.kbChunkMapper = kbChunkMapper;
        this.kbDocumentMapper = kbDocumentMapper;
        this.embeddingService = embeddingService;
    }

    /**
     * 取出最近一次检索快照并清理。
     *
     * @return 检索快照
     */
    @Override
    public Map<String, Object> consumeLastSearchTrace() {
        SearchTraceContext trace = SEARCH_TRACE_HOLDER.get();
        SEARCH_TRACE_HOLDER.remove();
        DEBUG_COLLECTOR_HOLDER.remove();
        if (trace == null) {
            return Collections.emptyMap();
        }
        return trace.toMap();
    }

    /**
     * 获取当前debug收集器。
     *
     * @return debug收集器
     */
    private RetrievalDebugCollector debugCollector() {
        return DEBUG_COLLECTOR_HOLDER.get();
    }

    /**
     * 执行语义检索。
     *
     * @param kbId 知识库ID
     * @param dto 检索请求
     * @return 检索结果
     */
    @Override
    public KbSemanticSearchResultVO search(String kbId, KbSemanticSearchQueryDTO dto) {
        SearchTraceContext trace = new SearchTraceContext();
        SEARCH_TRACE_HOLDER.set(trace);
        RetrievalDebugCollector collector = new RetrievalDebugCollector();
        DEBUG_COLLECTOR_HOLDER.set(collector);
        long totalStart = System.nanoTime();
        try {
            ensureKbEnabled(kbId);
            String originalQuery = dto == null ? null : dto.getQuery();
            if (oConvertUtils.isEmpty(originalQuery)) {
                throw new JeecgBootException("query不能为空");
            }

            KbSearchConfigVo searchConfig = kbSearchConfigService.getByKbId(kbId);
            SearchRuntimeSettings settings = resolveSearchRuntimeSettings(dto, searchConfig);
            trace.setKbId(kbId);
            trace.setOriginalQuery(originalQuery);
            trace.setActualParams(settings.toActualParams(kbId, originalQuery, dto == null || dto.getChatHistory() == null ? 0 : dto.getChatHistory().size()));
            long queryOptimizationStart = System.nanoTime();
            QueryOptimizationPlan optimizationPlan = buildQueryOptimizationPlan(originalQuery, dto == null ? null : dto.getChatHistory(), settings);
            collector.recordDuration("query_optimization_ms", queryOptimizationStart);
            trace.setOptimizedQueries(optimizationPlan.getOptimizedQueries());
            trace.setUsedQueries(optimizationPlan.getUsedQueries());
            collector.markExecuted("query_optimization", settings.useQueryOptimization);
            collector.setCandidateCount("optimized_query_count", optimizationPlan.getOptimizedQueries().size());
            collector.setCandidateCount("used_query_count", optimizationPlan.getUsedQueries().size());

            SearchContext context = loadSearchContext(kbId);
            Map<String, QueryMergeAggregate> mergedAggregates = new LinkedHashMap<>();
            boolean hasSuccess = false;
            JeecgBootException lastError = null;
            for (String usedQuery : optimizationPlan.getUsedQueries()) {
                if (oConvertUtils.isEmpty(usedQuery)) {
                    continue;
                }
                try {
                    List<KbSemanticSearchItemVO> candidates = searchSingleQuery(kbId, usedQuery, settings, context);
                    tagMatchedQuery(candidates, usedQuery);
                    mergeQueryResults(mergedAggregates, candidates);
                    if (!candidates.isEmpty()) {
                        hasSuccess = true;
                    }
                } catch (JeecgBootException ex) {
                    lastError = ex;
                    if (optimizationPlan.getUsedQueries().size() <= 1) {
                        throw ex;
                    }
                }
            }
            if (!hasSuccess && lastError != null) {
                throw lastError;
            }

            List<KbSemanticSearchItemVO> candidates = buildMergedQueryResults(mergedAggregates);
            collector.setCandidateCount("merged_candidate_count", candidates.size());
            if (settings.topK > 0 && candidates.size() > settings.topK) {
                candidates = new ArrayList<>(candidates.subList(0, settings.topK));
            }
            List<KbSemanticSearchItemVO> results = applyRerankAndReferenceLimit(originalQuery, candidates, settings.useRerank, settings.rerankModel, settings.rerankTopN, settings.rerankScoreThreshold, settings.finalTopK, settings.referenceLimit);
            collector.setCandidateCount("final_candidate_count", results.size());
            collector.recordDuration("total_ms", totalStart);

            KbSemanticSearchResultVO vo = new KbSemanticSearchResultVO();
            vo.setQuery(originalQuery);
            vo.setOriginalQuery(originalQuery);
            vo.setOptimizedQueries(optimizationPlan.getOptimizedQueries());
            vo.setUsedQueries(optimizationPlan.getUsedQueries());
            vo.setActualParams(trace.getActualParams());
            vo.setDebugInfo(collector.snapshot());
            vo.setSearchMode(settings.searchMode);
            vo.setQueryOptimizationMode(settings.queryOptimizationMode);
            vo.setUseQueryOptimization(settings.useQueryOptimization);
            vo.setTopK(settings.topK);
            vo.setUseRerank(settings.useRerank);
            vo.setRerankModel(settings.rerankModel);
            vo.setRerankTopN(settings.rerankTopN);
            vo.setFinalTopK(settings.finalTopK);
            vo.setSimilarityThreshold(settings.similarityThreshold);
            vo.setKeywordThreshold(settings.keywordThreshold);
            vo.setSemanticWeight(settings.weightPair.semanticWeight);
            vo.setKeywordWeight(settings.weightPair.keywordWeight);
            vo.setReferenceLimit(settings.referenceLimit);
            vo.setUsedReferenceLength(results.stream().mapToInt(item -> item.getReferenceLength() == null ? 0 : item.getReferenceLength()).sum());
            vo.setResultCount(results.size());
            vo.setResults(results);
            trace.setDebugInfo(vo.getDebugInfo());
            trace.setResultCount(vo.getResultCount());
            return vo;
        } catch (RuntimeException ex) {
            trace.setDebugInfo(collector.snapshot());
            throw ex;
        } finally {
            DEBUG_COLLECTOR_HOLDER.remove();
        }
    }

    /**
     * 构建语义候选结果。
     *
     * @param queryVector query向量
     * @param vectorRecords 向量记录
     * @param context 查询上下文
     * @param similarityThreshold 相似度阈值
     * @param aggregates 聚合结果
     */
    private void buildSemanticCandidates(List<Float> queryVector,
                                         List<KbVectorRecord> vectorRecords,
                                         SearchContext context,
                                         BigDecimal similarityThreshold,
                                         Map<String, SearchAggregate> aggregates) {
        RetrievalDebugCollector collector = debugCollector();
        for (KbVectorRecord record : vectorRecords) {
            if (record == null || oConvertUtils.isEmpty(record.getChunkIndexId())) {
                if (collector != null) {
                    collector.addFilter("invalid_status");
                }
                continue;
            }
            KbChunkIndex index = context.indexMap.get(record.getChunkIndexId());
            if (index == null || KbConstants.STATUS_DISABLE.equals(index.getStatus())) {
                if (collector != null) {
                    collector.addFilter("invalid_status");
                }
                continue;
            }
            if (!KbConstants.PROCESS_STATUS_SUCCESS.equals(index.getEmbeddingStatus())) {
                if (collector != null) {
                    collector.addFilter("embedding_not_success");
                }
                continue;
            }
            KbChunk chunk = context.chunkMap.get(index.getChunkId());
            if (chunk == null || KbConstants.STATUS_DISABLE.equals(chunk.getStatus())) {
                if (collector != null) {
                    collector.addFilter("invalid_status");
                }
                continue;
            }
            if (!index.getKbId().equals(chunk.getKbId())) {
                if (collector != null) {
                    collector.addFilter("cross_kb_filtered");
                }
                continue;
            }
            KbDocument document = context.documentMap.get(chunk.getDocumentId());
            if (document == null || KbConstants.STATUS_DISABLE.equals(document.getStatus())) {
                if (collector != null) {
                    collector.addFilter("invalid_status");
                }
                continue;
            }
            if (!index.getKbId().equals(document.getKbId())) {
                if (collector != null) {
                    collector.addFilter("cross_kb_filtered");
                }
                continue;
            }
            List<Float> vector = parseVector(record.getVectorJson());
            if (vector.size() != queryVector.size()) {
                throw new JeecgBootException("向量维度不匹配");
            }
            BigDecimal score = normalizeScore(cosineSimilarity(queryVector, vector));
            if (score.compareTo(similarityThreshold) < 0) {
                if (collector != null) {
                    collector.addFilter("below_similarity_threshold");
                }
                continue;
            }
            KbSemanticSearchItemVO item = buildResultItem(record, index, chunk, document, score,
                    KbConstants.MATCHED_FIELD_INDEX_TEXT, index.getIndexText(), KbConstants.HIT_TYPE_SEMANTIC,
                    score, BigDecimal.ZERO, score);
            mergeSemanticCandidate(aggregates, chunk.getId(), item);
            if (collector != null) {
                collector.addCandidateCount("semantic_candidate_count", 1);
            }
        }
    }

    /**
     * 构建全文候选结果。
     *
     * @param query 查询内容
     * @param context 查询上下文
     * @param keywordThreshold 关键词阈值
     * @param aggregates 聚合结果
     */
    private void buildFulltextCandidates(String query,
                                         SearchContext context,
                                         BigDecimal keywordThreshold,
                                         Map<String, SearchAggregate> aggregates) {
        RetrievalDebugCollector collector = debugCollector();
        List<String> terms = buildKeywordTerms(query);
        if (terms.isEmpty()) {
            return;
        }
        for (KbChunkIndex index : context.indexMap.values()) {
            if (index == null || KbConstants.STATUS_DISABLE.equals(index.getStatus())) {
                if (collector != null) {
                    collector.addFilter("invalid_status");
                }
                continue;
            }
            KbChunk chunk = context.chunkMap.get(index.getChunkId());
            if (chunk == null || KbConstants.STATUS_DISABLE.equals(chunk.getStatus())) {
                if (collector != null) {
                    collector.addFilter("invalid_status");
                }
                continue;
            }
            if (!index.getKbId().equals(chunk.getKbId())) {
                if (collector != null) {
                    collector.addFilter("cross_kb_filtered");
                }
                continue;
            }
            KbDocument document = context.documentMap.get(chunk.getDocumentId());
            if (document == null || KbConstants.STATUS_DISABLE.equals(document.getStatus())) {
                if (collector != null) {
                    collector.addFilter("invalid_status");
                }
                continue;
            }
            if (!index.getKbId().equals(document.getKbId())) {
                if (collector != null) {
                    collector.addFilter("cross_kb_filtered");
                }
                continue;
            }
            KeywordMatch match = evaluateKeywordMatch(query, terms, index.getIndexText(), chunk.getContent());
            if (match == null || match.score.compareTo(keywordThreshold) < 0) {
                if (collector != null) {
                    collector.addFilter("below_keyword_threshold");
                }
                continue;
            }
            KbSemanticSearchItemVO item = buildResultItem(null, index, chunk, document, match.score,
                    match.matchedField, match.matchedText, KbConstants.HIT_TYPE_FULLTEXT,
                    BigDecimal.ZERO, match.score, match.score);
            mergeKeywordCandidate(aggregates, chunk.getId(), item);
            if (collector != null) {
                collector.addCandidateCount("fulltext_candidate_count", 1);
            }
        }
    }

    /**
     * 构建最终结果列表。
     *
     * @param searchMode 搜索模式
     * @param weightPair 权重
     * @param aggregates 聚合结果
     * @param topK 返回数量
     * @return 结果列表
     */
    private List<KbSemanticSearchItemVO> buildResults(String searchMode,
                                                      WeightPair weightPair,
                                                      Map<String, SearchAggregate> aggregates) {
        List<KbSemanticSearchItemVO> results = new ArrayList<>();
        for (SearchAggregate aggregate : aggregates.values()) {
            KbSemanticSearchItemVO item = aggregate.build(searchMode, weightPair);
            if (item != null) {
                results.add(item);
            }
        }
        sortSearchResults(results);
        return results;
    }

    /**
     * 执行单个query的检索，不包含Query Optimization聚合。
     *
     * @param kbId 知识库ID
     * @param query 检索词
     * @param settings 检索参数
     * @param context 查询上下文
     * @return 候选结果
     */
    private List<KbSemanticSearchItemVO> searchSingleQuery(String kbId,
                                                           String query,
                                                           SearchRuntimeSettings settings,
                                                           SearchContext context) {
        Map<String, SearchAggregate> aggregates = new LinkedHashMap<>();
        RetrievalDebugCollector collector = debugCollector();

        if (KbConstants.SEARCH_MODE_SEMANTIC.equals(settings.searchMode) || KbConstants.SEARCH_MODE_HYBRID.equals(settings.searchMode)) {
            if (collector != null) {
                collector.markExecuted("semantic_search", true);
            }
            long semanticStart = System.nanoTime();
            EmbeddingResultVO queryEmbedding;
            try {
                queryEmbedding = embeddingService.embed(query);
            } catch (Exception e) {
                throw new JeecgBootException("query embedding失败：" + e.getMessage());
            }
            List<Float> queryVector = queryEmbedding.getVector();
            if (queryVector == null || queryVector.isEmpty()) {
                throw new JeecgBootException("query embedding失败：向量为空");
            }
            List<KbVectorRecord> vectorRecords = kbVectorRecordMapper.selectList(new LambdaQueryWrapper<KbVectorRecord>()
                    .eq(KbVectorRecord::getKbId, kbId));
            if (!vectorRecords.isEmpty()) {
                buildSemanticCandidates(queryVector, vectorRecords, context, settings.similarityThreshold, aggregates);
            }
            if (collector != null) {
                collector.recordDuration("semantic_search_ms", semanticStart);
            }
        }

        if (KbConstants.SEARCH_MODE_FULLTEXT.equals(settings.searchMode) || KbConstants.SEARCH_MODE_HYBRID.equals(settings.searchMode)) {
            if (collector != null) {
                collector.markExecuted("fulltext_search", true);
            }
            long fulltextStart = System.nanoTime();
            buildFulltextCandidates(query, context, settings.keywordThreshold, aggregates);
            if (collector != null) {
                collector.recordDuration("fulltext_search_ms", fulltextStart);
            }
        }
        long fusionStart = System.nanoTime();
        List<KbSemanticSearchItemVO> results = buildResults(settings.searchMode, settings.weightPair, aggregates);
        if (collector != null) {
            collector.recordDuration(KbConstants.SEARCH_MODE_HYBRID.equals(settings.searchMode) ? "hybrid_fusion_ms" : "result_build_ms", fusionStart);
            collector.setCandidateCount("query_candidate_count", results.size());
            if (KbConstants.SEARCH_MODE_HYBRID.equals(settings.searchMode)) {
                collector.markExecuted("hybrid_fusion", true);
                collector.setCandidateCount("hybrid_fusion_candidate_count", results.size());
            }
        }
        return results;
    }

    /**
     * 给结果打上命中的query。
     *
     * @param items 结果列表
     * @param matchedQuery 命中query
     */
    private void tagMatchedQuery(List<KbSemanticSearchItemVO> items, String matchedQuery) {
        if (items == null || items.isEmpty()) {
            return;
        }
        for (KbSemanticSearchItemVO item : items) {
            if (item != null) {
                item.setMatchedQuery(matchedQuery);
            }
        }
    }

    /**
     * 合并多个query的结果。
     *
     * @param mergedAggregates 合并结果
     * @param queryResults 单query结果
     */
    private void mergeQueryResults(Map<String, QueryMergeAggregate> mergedAggregates, List<KbSemanticSearchItemVO> queryResults) {
        if (mergedAggregates == null || queryResults == null || queryResults.isEmpty()) {
            return;
        }
        RetrievalDebugCollector collector = debugCollector();
        for (KbSemanticSearchItemVO item : queryResults) {
            if (item == null) {
                continue;
            }
            String mergeKey = oConvertUtils.isNotEmpty(item.getChunkId()) ? item.getChunkId() : item.getChunkIndexId();
            if (oConvertUtils.isEmpty(mergeKey)) {
                continue;
            }
            QueryMergeAggregate aggregate = mergedAggregates.computeIfAbsent(mergeKey, key -> new QueryMergeAggregate());
            boolean accepted = aggregate.add(item);
            if (!accepted && collector != null) {
                collector.addFilter("duplicated_chunk_filtered");
            }
        }
    }

    /**
     * 构建合并后的结果列表。
     *
     * @param mergedAggregates 合并结果
     * @return 结果列表
     */
    private List<KbSemanticSearchItemVO> buildMergedQueryResults(Map<String, QueryMergeAggregate> mergedAggregates) {
        if (mergedAggregates == null || mergedAggregates.isEmpty()) {
            return Collections.emptyList();
        }
        List<KbSemanticSearchItemVO> results = new ArrayList<>();
        for (QueryMergeAggregate aggregate : mergedAggregates.values()) {
            KbSemanticSearchItemVO item = aggregate.build();
            if (item != null) {
                results.add(item);
            }
        }
        sortSearchResults(results);
        RetrievalDebugCollector collector = debugCollector();
        if (collector != null) {
            collector.setCandidateCount("merged_candidate_count", results.size());
        }
        return results;
    }

    /**
     * 排序结果。
     *
     * @param results 结果列表
     */
    private void sortSearchResults(List<KbSemanticSearchItemVO> results) {
        if (results == null || results.isEmpty()) {
            return;
        }
        results.sort((left, right) -> {
            BigDecimal leftScore = left.getFinalScore() == null ? left.getScore() : left.getFinalScore();
            BigDecimal rightScore = right.getFinalScore() == null ? right.getScore() : right.getFinalScore();
            int scoreCompare = rightScore.compareTo(leftScore);
            if (scoreCompare != 0) {
                return scoreCompare;
            }
            int leftSort = left.getSortNo() == null ? Integer.MAX_VALUE : left.getSortNo();
            int rightSort = right.getSortNo() == null ? Integer.MAX_VALUE : right.getSortNo();
            if (leftSort != rightSort) {
                return Integer.compare(leftSort, rightSort);
            }
            return compareText(left.getChunkIndexId(), right.getChunkIndexId());
        });
    }

    /**
     * 解析搜索运行参数。
     *
     * @param dto 请求参数
     * @param searchConfig 配置
     * @return 运行参数
     */
    private SearchRuntimeSettings resolveSearchRuntimeSettings(KbSemanticSearchQueryDTO dto, KbSearchConfigVo searchConfig) {
        String searchMode = resolveSearchMode(dto == null ? null : dto.getSearchMode(), searchConfig == null ? null : searchConfig.getSearchMode());
        Integer requestTopK = dto == null ? null : dto.getTopK();
        int topK = resolveTopK(requestTopK, searchConfig == null ? null : searchConfig.getTopK());
        int finalTopK = resolveFinalTopK(dto == null ? null : dto.getFinalTopK(),
                requestTopK,
                searchConfig == null ? null : searchConfig.getFinalTopK(),
                searchConfig == null ? null : searchConfig.getTopK());
        BigDecimal similarityThreshold = resolveThreshold(dto == null ? null : dto.getSimilarityThreshold(), searchConfig == null ? null : searchConfig.getSimilarityThreshold());
        BigDecimal keywordThreshold = resolveKeywordThreshold(dto == null ? null : dto.getKeywordThreshold());
        WeightPair weightPair = resolveWeights(searchMode, dto == null ? null : dto.getSemanticWeight(), dto == null ? null : dto.getKeywordWeight());
        boolean useRerank = resolveUseRerank(dto == null ? null : dto.getUseRerank(), searchConfig == null ? null : searchConfig.getUseRerank());
        String rerankModel = resolveRerankModel(dto == null ? null : dto.getRerankModel(), searchConfig);
        int rerankTopN = resolveRerankTopN(dto == null ? null : dto.getRerankTopN(), searchConfig == null ? null : searchConfig.getRerankTopN());
        BigDecimal rerankScoreThreshold = resolveRerankScoreThreshold(dto == null ? null : dto.getRerankScoreThreshold(), searchConfig == null ? null : searchConfig.getRerankScoreThreshold());
        int referenceLimit = resolveReferenceLimit(dto == null ? null : dto.getReferenceLimit(), searchConfig == null ? null : searchConfig.getReferenceLimit());
        boolean useQueryOptimization = resolveUseQueryOptimization(dto == null ? null : dto.getUseQueryOptimization(), searchConfig == null ? null : searchConfig.getUseQueryOptimization());
        String queryOptimizationMode = resolveQueryOptimizationMode(dto == null ? null : dto.getQueryOptimizationMode(), searchConfig == null ? null : searchConfig.getQueryOptimizationMode());
        int maxRewriteQueries = resolveMaxRewriteQueries(dto == null ? null : dto.getMaxRewriteQueries(), searchConfig == null ? null : searchConfig.getMaxRewriteQueries());
        boolean keepOriginalQuery = resolveKeepOriginalQuery(dto == null ? null : dto.getKeepOriginalQuery(), searchConfig == null ? null : searchConfig.getKeepOriginalQuery());
        if (KbConstants.QUERY_OPTIMIZATION_MODE_OFF.equals(queryOptimizationMode)) {
            useQueryOptimization = false;
        }
        return new SearchRuntimeSettings(searchMode, topK, finalTopK, similarityThreshold, keywordThreshold, weightPair,
                useRerank, rerankModel, rerankTopN, rerankScoreThreshold, referenceLimit,
                useQueryOptimization, queryOptimizationMode, maxRewriteQueries, keepOriginalQuery);
    }

    /**
     * 构建查询优化计划。
     *
     * @param originalQuery 原始query
     * @param chatHistory 历史消息
     * @param settings 运行参数
     * @return 优化计划
     */
    private QueryOptimizationPlan buildQueryOptimizationPlan(String originalQuery,
                                                             List<KbQueryOptimizationHistoryDTO> chatHistory,
                                                             SearchRuntimeSettings settings) {
        List<String> optimizedQueries = Collections.emptyList();
        if (settings.useQueryOptimization) {
            optimizedQueries = generateOptimizedQueries(originalQuery, chatHistory, settings.queryOptimizationMode, settings.maxRewriteQueries);
        }
        optimizedQueries = normalizeQueryList(optimizedQueries, originalQuery, settings.maxRewriteQueries);
        List<String> usedQueries = new ArrayList<>();
        if (settings.keepOriginalQuery || optimizedQueries.isEmpty()) {
            usedQueries.add(originalQuery);
        }
        usedQueries.addAll(optimizedQueries);
        usedQueries = normalizeUsedQueryList(usedQueries, settings.maxRewriteQueries + 1);
        if (usedQueries.isEmpty()) {
            usedQueries.add(originalQuery);
        }
        List<String> finalOptimizedQueries = usedQueries.stream()
                .filter(query -> !isSameQuery(query, originalQuery))
                .collect(Collectors.toList());
        return new QueryOptimizationPlan(finalOptimizedQueries, usedQueries);
    }

    /**
     * 生成优化后的query列表。
     *
     * @param originalQuery 原始query
     * @param chatHistory 历史
     * @param mode 模式
     * @param maxRewriteQueries 最大数量
     * @return 优化query列表
     */
    private List<String> generateOptimizedQueries(String originalQuery,
                                                  List<KbQueryOptimizationHistoryDTO> chatHistory,
                                                  String mode,
                                                  int maxRewriteQueries) {
        if (maxRewriteQueries <= 0) {
            return Collections.emptyList();
        }
        List<String> candidates = new ArrayList<>();
        String normalizedMode = oConvertUtils.isEmpty(mode) ? KbConstants.DEFAULT_QUERY_OPTIMIZATION_MODE : mode;
        if (KbConstants.QUERY_OPTIMIZATION_MODE_OFF.equals(normalizedMode)) {
            return candidates;
        }
        if (KbConstants.DEFAULT_QUERY_OPTIMIZATION_MODE.equals(normalizedMode) || KbConstants.SEARCH_MODE_HYBRID.equals(normalizedMode)) {
            candidates.add(buildRewriteQuery(originalQuery, chatHistory));
        }
        if ("keywords".equals(normalizedMode) || KbConstants.SEARCH_MODE_HYBRID.equals(normalizedMode)) {
            candidates.addAll(buildKeywordOptimizationQueries(originalQuery, chatHistory));
        }
        if ("expand".equals(normalizedMode) || KbConstants.SEARCH_MODE_HYBRID.equals(normalizedMode)) {
            candidates.addAll(buildExpandQueries(originalQuery, chatHistory));
        }
        if ("rewrite".equals(normalizedMode)) {
            if (candidates.isEmpty()) {
                candidates.add(buildRewriteQuery(originalQuery, chatHistory));
            }
        }
        return normalizeQueryList(candidates, originalQuery, maxRewriteQueries);
    }

    /**
     * 生成重写query。
     *
     * @param originalQuery 原始query
     * @param chatHistory 历史
     * @return 重写query
     */
    private String buildRewriteQuery(String originalQuery, List<KbQueryOptimizationHistoryDTO> chatHistory) {
        String normalizedQuery = normalizeText(originalQuery);
        String historyText = collectChatHistoryText(chatHistory);
        Set<String> protectedTokens = extractProtectedTokens(originalQuery);
        protectedTokens.addAll(extractProtectedTokens(historyText));
        String historyTopic = extractHistoryTopic(chatHistory, protectedTokens);
        if (oConvertUtils.isNotEmpty(normalizedQuery) && normalizedQuery.contains("背景故事")) {
            return truncateOptimizedQuery("background_story 字段的填写要求是什么");
        }
        if (oConvertUtils.isNotEmpty(normalizedQuery) && normalizedQuery.contains("人设")) {
            return truncateOptimizedQuery("角色设定、角色画像、人物设定、AI伴侣角色应该如何设计");
        }
        if (oConvertUtils.isNotEmpty(normalizedQuery) && (normalizedQuery.contains("第二个字段") || normalizedQuery.matches(".*第[二2].*字段.*"))) {
            if (oConvertUtils.isNotEmpty(historyTopic)) {
                return truncateOptimizedQuery(historyTopic + " 字段应该怎么填写");
            }
            return truncateOptimizedQuery("第二个字段应该怎么填写");
        }
        if (containsVagueReference(normalizedQuery) && oConvertUtils.isNotEmpty(historyTopic)) {
            return truncateOptimizedQuery(historyTopic + " " + originalQuery);
        }
        if (!protectedTokens.isEmpty()) {
            return truncateOptimizedQuery(String.join(" ", protectedTokens) + " " + originalQuery);
        }
        if (oConvertUtils.isNotEmpty(historyTopic)) {
            return truncateOptimizedQuery(historyTopic + " " + originalQuery);
        }
        return truncateOptimizedQuery(originalQuery);
    }

    /**
     * 生成关键词query。
     *
     * @param originalQuery 原始query
     * @param chatHistory 历史
     * @return 关键词query列表
     */
    private List<String> buildKeywordOptimizationQueries(String originalQuery, List<KbQueryOptimizationHistoryDTO> chatHistory) {
        Set<String> candidates = new LinkedHashSet<>();
        candidates.addAll(buildKeywordTerms(originalQuery));
        candidates.addAll(extractProtectedTokens(collectChatHistoryText(chatHistory)));
        for (String token : extractProtectedTokens(originalQuery)) {
            candidates.add(token);
        }
        List<String> result = new ArrayList<>();
        for (String candidate : candidates) {
            String value = truncateOptimizedQuery(candidate);
            if (oConvertUtils.isNotEmpty(value)) {
                result.add(value);
            }
        }
        return result;
    }

    /**
     * 生成扩展query。
     *
     * @param originalQuery 原始query
     * @param chatHistory 历史
     * @return 扩展query列表
     */
    private List<String> buildExpandQueries(String originalQuery, List<KbQueryOptimizationHistoryDTO> chatHistory) {
        Set<String> result = new LinkedHashSet<>();
        String normalizedQuery = normalizeText(originalQuery);
        Map<String, List<String>> synonymMap = buildQuerySynonymMap();
        for (Map.Entry<String, List<String>> entry : synonymMap.entrySet()) {
            if (oConvertUtils.isEmpty(entry.getKey()) || oConvertUtils.isEmpty(normalizedQuery) || !normalizedQuery.contains(entry.getKey().toLowerCase(Locale.ROOT))) {
                continue;
            }
            result.addAll(entry.getValue());
        }
        Set<String> protectedTokens = extractProtectedTokens(originalQuery);
        protectedTokens.addAll(extractProtectedTokens(collectChatHistoryText(chatHistory)));
        result.addAll(protectedTokens);
        List<String> items = new ArrayList<>();
        for (String candidate : result) {
            String value = truncateOptimizedQuery(candidate);
            if (oConvertUtils.isNotEmpty(value)) {
                items.add(value);
            }
        }
        return items;
    }

    /**
     * 规范化并去重query列表。
     *
     * @param queries query列表
     * @param originalQuery 原始query
     * @param maxCount 最大保留数量
     * @return 规范化结果
     */
    private List<String> normalizeQueryList(List<String> queries, String originalQuery, int maxCount) {
        if (queries == null || queries.isEmpty()) {
            return Collections.emptyList();
        }
        LinkedHashMap<String, String> unique = new LinkedHashMap<>();
        for (String query : queries) {
            String normalized = truncateOptimizedQuery(query);
            if (oConvertUtils.isEmpty(normalized)) {
                continue;
            }
            String key = normalizeText(normalized);
            if (oConvertUtils.isEmpty(key)) {
                continue;
            }
            if (isSameQuery(normalized, originalQuery)) {
                continue;
            }
            if (unique.containsKey(key)) {
                continue;
            }
            unique.put(key, normalized);
            if (unique.size() >= maxCount) {
                break;
            }
        }
        return new ArrayList<>(unique.values());
    }

    /**
     * 规范化并去重实际执行query列表。
     *
     * @param queries query列表
     * @param maxCount 最大保留数量
     * @return 规范化结果
     */
    private List<String> normalizeUsedQueryList(List<String> queries, int maxCount) {
        if (queries == null || queries.isEmpty()) {
            return Collections.emptyList();
        }
        LinkedHashMap<String, String> unique = new LinkedHashMap<>();
        for (String query : queries) {
            String normalized = truncateOptimizedQuery(query);
            if (oConvertUtils.isEmpty(normalized)) {
                continue;
            }
            String key = normalizeText(normalized);
            if (oConvertUtils.isEmpty(key) || unique.containsKey(key)) {
                continue;
            }
            unique.put(key, normalized);
            if (unique.size() >= maxCount) {
                break;
            }
        }
        return new ArrayList<>(unique.values());
    }

    /**
     * 判断两个query是否一致。
     *
     * @param left 左侧
     * @param right 右侧
     * @return 是否一致
     */
    private boolean isSameQuery(String left, String right) {
        String leftValue = normalizeText(left);
        String rightValue = normalizeText(right);
        if (oConvertUtils.isEmpty(leftValue) && oConvertUtils.isEmpty(rightValue)) {
            return true;
        }
        return leftValue != null && leftValue.equals(rightValue);
    }

    /**
     * 截断优化后的query。
     *
     * @param query query
     * @return 截断结果
     */
    private String truncateOptimizedQuery(String query) {
        if (oConvertUtils.isEmpty(query)) {
            return null;
        }
        String value = query.trim();
        if (value.length() > 512) {
            value = value.substring(0, 512);
        }
        return value;
    }

    /**
     * 是否是模糊指代。
     *
     * @param query query
     * @return 是否模糊
     */
    private boolean containsVagueReference(String query) {
        if (oConvertUtils.isEmpty(query)) {
            return false;
        }
        return query.contains("这个") || query.contains("那个") || query.contains("它") || query.contains("这样") || query.contains("再冷一点") || query.contains("更冷") || query.contains("更高冷");
    }

    /**
     * 收集历史消息。
     *
     * @param chatHistory 历史
     * @return 拼接文本
     */
    private String collectChatHistoryText(List<KbQueryOptimizationHistoryDTO> chatHistory) {
        if (chatHistory == null || chatHistory.isEmpty()) {
            return null;
        }
        StringBuilder builder = new StringBuilder();
        int count = 0;
        for (int i = chatHistory.size() - 1; i >= 0 && count < 6; i--) {
            KbQueryOptimizationHistoryDTO item = chatHistory.get(i);
            if (item == null || oConvertUtils.isEmpty(item.getContent())) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append('\n');
            }
            builder.append(item.getContent());
            count++;
        }
        return builder.toString();
    }

    /**
     * 提取历史主题。
     *
     * @param chatHistory 历史
     * @param protectedTokens 保护词
     * @return 历史主题
     */
    private String extractHistoryTopic(List<KbQueryOptimizationHistoryDTO> chatHistory, Set<String> protectedTokens) {
        String historyText = collectChatHistoryText(chatHistory);
        if (oConvertUtils.isEmpty(historyText)) {
            return null;
        }
        Set<String> tokens = new LinkedHashSet<>();
        if (protectedTokens != null) {
            tokens.addAll(protectedTokens);
        }
        tokens.addAll(extractProtectedTokens(historyText));
        for (String token : tokens) {
            if (oConvertUtils.isNotEmpty(token) && historyText.contains(token)) {
                return token;
            }
        }
        List<String> segments = extractChineseSegments(historyText);
        if (segments.isEmpty()) {
            return null;
        }
        segments.sort((left, right) -> Integer.compare(right.length(), left.length()));
        return segments.get(0);
    }

    /**
     * 提取受保护的精确词。
     *
     * @param text 文本
     * @return 受保护词集合
     */
    private Set<String> extractProtectedTokens(String text) {
        Set<String> tokens = new LinkedHashSet<>();
        if (oConvertUtils.isEmpty(text)) {
            return tokens;
        }
        Matcher matcher = Pattern.compile("(?<![A-Za-z0-9_])(?:[A-Za-z_][A-Za-z0-9_]*::[A-Za-z0-9_]+|[A-Za-z_][A-Za-z0-9_]+)(?![A-Za-z0-9_])").matcher(text);
        while (matcher.find()) {
            String token = matcher.group();
            if (oConvertUtils.isNotEmpty(token)) {
                tokens.add(token);
            }
        }
        if (text.contains("pending")) {
            tokens.add("pending");
        }
        if (text.contains("failed")) {
            tokens.add("failed");
        }
        if (text.contains("processing")) {
            tokens.add("processing");
        }
        if (text.contains("success")) {
            tokens.add("success");
        }
        return tokens;
    }

    /**
     * 查询同义词映射。
     *
     * @return 同义词映射
     */
    private Map<String, List<String>> buildQuerySynonymMap() {
        Map<String, List<String>> synonymMap = new LinkedHashMap<>();
        synonymMap.put("人设", java.util.Arrays.asList("角色设定", "角色画像", "人物设定", "AI伴侣角色"));
        synonymMap.put("背景故事", java.util.Arrays.asList("background_story", "story_intro", "角色背景", "背景故事"));
        synonymMap.put("字段", java.util.Arrays.asList("字段", "参数", "属性"));
        synonymMap.put("工具", java.util.Arrays.asList("工具名", "接口名", "function_name"));
        synonymMap.put("模板", java.util.Arrays.asList("模板名", "template_name"));
        synonymMap.put("错误码", java.util.Arrays.asList("错误码", "error_code", "code"));
        return synonymMap;
    }

    /**
     * 搜索运行参数。
     */
    private static class SearchRuntimeSettings {
        private final String searchMode;
        private final int topK;
        private final int finalTopK;
        private final BigDecimal similarityThreshold;
        private final BigDecimal keywordThreshold;
        private final WeightPair weightPair;
        private final boolean useRerank;
        private final String rerankModel;
        private final int rerankTopN;
        private final BigDecimal rerankScoreThreshold;
        private final int referenceLimit;
        private final boolean useQueryOptimization;
        private final String queryOptimizationMode;
        private final int maxRewriteQueries;
        private final boolean keepOriginalQuery;

        private SearchRuntimeSettings(String searchMode,
                                      int topK,
                                      int finalTopK,
                                      BigDecimal similarityThreshold,
                                      BigDecimal keywordThreshold,
                                      WeightPair weightPair,
                                      boolean useRerank,
                                      String rerankModel,
                                      int rerankTopN,
                                      BigDecimal rerankScoreThreshold,
                                      int referenceLimit,
                                      boolean useQueryOptimization,
                                      String queryOptimizationMode,
                                      int maxRewriteQueries,
                                      boolean keepOriginalQuery) {
            this.searchMode = searchMode;
            this.topK = topK;
            this.finalTopK = finalTopK;
            this.similarityThreshold = similarityThreshold;
            this.keywordThreshold = keywordThreshold;
            this.weightPair = weightPair;
            this.useRerank = useRerank;
            this.rerankModel = rerankModel;
            this.rerankTopN = rerankTopN;
            this.rerankScoreThreshold = rerankScoreThreshold;
            this.referenceLimit = referenceLimit;
            this.useQueryOptimization = useQueryOptimization;
            this.queryOptimizationMode = queryOptimizationMode;
            this.maxRewriteQueries = maxRewriteQueries;
            this.keepOriginalQuery = keepOriginalQuery;
        }

        private Map<String, Object> toActualParams(String kbId, String query, int chatHistorySize) {
            Map<String, Object> actual = new LinkedHashMap<>();
            actual.put("kb_id", kbId);
            actual.put("query", query);
            actual.put("search_mode", searchMode);
            actual.put("top_k", topK);
            actual.put("final_top_k", finalTopK);
            actual.put("similarity_threshold", similarityThreshold);
            actual.put("keyword_threshold", keywordThreshold);
            actual.put("semantic_weight", weightPair == null ? null : weightPair.semanticWeight);
            actual.put("keyword_weight", weightPair == null ? null : weightPair.keywordWeight);
            actual.put("use_rerank", useRerank);
            actual.put("rerank_model", rerankModel);
            actual.put("rerank_top_n", rerankTopN);
            actual.put("rerank_score_threshold", rerankScoreThreshold);
            actual.put("reference_limit", referenceLimit);
            actual.put("use_query_optimization", useQueryOptimization);
            actual.put("query_optimization_mode", queryOptimizationMode);
            actual.put("max_rewrite_queries", maxRewriteQueries);
            actual.put("keep_original_query", keepOriginalQuery);
            actual.put("chat_history_size", chatHistorySize);
            return actual;
        }
    }

    /**
     * 查询优化计划。
     */
    private static class QueryOptimizationPlan {
        private final List<String> optimizedQueries;
        private final List<String> usedQueries;

        private QueryOptimizationPlan(List<String> optimizedQueries, List<String> usedQueries) {
            this.optimizedQueries = optimizedQueries == null ? Collections.emptyList() : optimizedQueries;
            this.usedQueries = usedQueries == null ? Collections.emptyList() : usedQueries;
        }

        private List<String> getOptimizedQueries() {
            return optimizedQueries;
        }

        private List<String> getUsedQueries() {
            return usedQueries;
        }
    }

    /**
     * 检索快照。
     */
    private static class SearchTraceContext {
        private String kbId;
        private String originalQuery;
        private List<String> optimizedQueries = Collections.emptyList();
        private List<String> usedQueries = Collections.emptyList();
        private Map<String, Object> actualParams = new LinkedHashMap<>();
        private Map<String, Object> debugInfo = new LinkedHashMap<>();
        private Integer resultCount;

        private void setKbId(String kbId) {
            this.kbId = kbId;
        }

        private void setOriginalQuery(String originalQuery) {
            this.originalQuery = originalQuery;
        }

        private void setOptimizedQueries(List<String> optimizedQueries) {
            this.optimizedQueries = optimizedQueries == null ? Collections.emptyList() : new ArrayList<>(optimizedQueries);
        }

        private void setUsedQueries(List<String> usedQueries) {
            this.usedQueries = usedQueries == null ? Collections.emptyList() : new ArrayList<>(usedQueries);
        }

        private void setActualParams(Map<String, Object> actualParams) {
            this.actualParams = actualParams == null ? new LinkedHashMap<>() : new LinkedHashMap<>(actualParams);
        }

        private Map<String, Object> getActualParams() {
            return actualParams;
        }

        private void setDebugInfo(Map<String, Object> debugInfo) {
            this.debugInfo = debugInfo == null ? new LinkedHashMap<>() : new LinkedHashMap<>(debugInfo);
        }

        private void setResultCount(Integer resultCount) {
            this.resultCount = resultCount;
        }

        private Map<String, Object> toMap() {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("kb_id", kbId);
            map.put("original_query", originalQuery);
            map.put("optimized_queries", optimizedQueries);
            map.put("used_queries", usedQueries);
            map.put("actual_params", actualParams);
            map.put("debug_info", debugInfo);
            map.put("result_count", resultCount);
            return map;
        }
    }

    /**
     * 检索debug收集器。
     */
    private static class RetrievalDebugCollector {
        private final long startNano = System.nanoTime();
        private final Map<String, Boolean> executedStages = new LinkedHashMap<>();
        private final Map<String, Integer> candidateCounts = new LinkedHashMap<>();
        private final Map<String, Long> stageCosts = new LinkedHashMap<>();
        private final Map<String, Integer> filterReasons = new LinkedHashMap<>();

        private void markExecuted(String stage, boolean executed) {
            executedStages.put(stage, executed);
        }

        private void setCandidateCount(String stage, int count) {
            candidateCounts.put(stage, count);
        }

        private void addCandidateCount(String stage, int delta) {
            candidateCounts.put(stage, candidateCounts.getOrDefault(stage, 0) + delta);
        }

        private void recordDuration(String stage, long startNanoValue) {
            stageCosts.put(stage, TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanoValue));
        }

        private void addFilter(String reason) {
            filterReasons.put(reason, filterReasons.getOrDefault(reason, 0) + 1);
        }

        private Map<String, Object> snapshot() {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("query_optimization_executed", Boolean.TRUE.equals(executedStages.get("query_optimization")));
            map.put("semantic_search_executed", Boolean.TRUE.equals(executedStages.get("semantic_search")));
            map.put("fulltext_search_executed", Boolean.TRUE.equals(executedStages.get("fulltext_search")));
            map.put("hybrid_fusion_executed", Boolean.TRUE.equals(executedStages.get("hybrid_fusion")));
            map.put("rerank_executed", Boolean.TRUE.equals(executedStages.get("rerank")));
            map.put("reference_limit_executed", Boolean.TRUE.equals(executedStages.get("reference_limit")));
            map.put("candidate_counts", new LinkedHashMap<>(candidateCounts));
            map.put("stage_cost_ms", new LinkedHashMap<>(stageCosts));
            map.put("filter_reasons", new LinkedHashMap<>(filterReasons));
            map.put("total_cost_ms", TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNano));
            return map;
        }
    }

    /**
     * 多query合并结果。
     */
    private static class QueryMergeAggregate {
        private KbSemanticSearchItemVO item;
        private BigDecimal score = BigDecimal.ZERO;

        private boolean add(KbSemanticSearchItemVO candidate) {
            if (candidate == null) {
                return false;
            }
            BigDecimal candidateScore = candidate.getFinalScore() == null ? (candidate.getScore() == null ? BigDecimal.ZERO : candidate.getScore()) : candidate.getFinalScore();
            if (item == null || candidateScore.compareTo(score) > 0 || (candidateScore.compareTo(score) == 0 && betterOrder(candidate, item))) {
                item = candidate;
                score = candidateScore;
                return true;
            }
            return false;
        }

        private KbSemanticSearchItemVO build() {
            if (item == null) {
                return null;
            }
            KbSemanticSearchItemVO copy = new KbSemanticSearchItemVO();
            BeanUtils.copyProperties(item, copy);
            if (oConvertUtils.isEmpty(copy.getMatchedQuery())) {
                copy.setMatchedQuery(item.getMatchedQuery());
            }
            return copy;
        }

        private boolean betterOrder(KbSemanticSearchItemVO left, KbSemanticSearchItemVO right) {
            if (left == null) {
                return false;
            }
            if (right == null) {
                return true;
            }
            int leftSort = left.getSortNo() == null ? Integer.MAX_VALUE : left.getSortNo();
            int rightSort = right.getSortNo() == null ? Integer.MAX_VALUE : right.getSortNo();
            if (leftSort != rightSort) {
                return leftSort < rightSort;
            }
            return compareText(left.getChunkIndexId(), right.getChunkIndexId()) < 0;
        }
    }

    /**
     * 构建结果项。
     *
     * @param record 向量记录，可为空
     * @param index chunk索引
     * @param chunk chunk
     * @param document 文档
     * @param score 得分
     * @param matchedField 命中字段
     * @param matchedText 命中文本
     * @param hitType 命中来源
     * @param semanticScore 语义分数
     * @param keywordScore 关键词分数
     * @param finalScore 融合分数
     * @return 结果项
     */
    private KbSemanticSearchItemVO buildResultItem(KbVectorRecord record,
                                                   KbChunkIndex index,
                                                   KbChunk chunk,
                                                   KbDocument document,
                                                   BigDecimal score,
                                                   String matchedField,
                                                   String matchedText,
                                                   String hitType,
                                                   BigDecimal semanticScore,
                                                   BigDecimal keywordScore,
                                                   BigDecimal finalScore) {
        KbSemanticSearchItemVO item = new KbSemanticSearchItemVO();
        item.setKbId(document.getKbId());
        item.setDocumentId(document.getId());
        item.setDocumentName(document.getName());
        item.setChunkId(chunk.getId());
        item.setChunkIndexId(index.getId());
        item.setContent(chunk.getContent());
        item.setIndexText(index.getIndexText());
        item.setMatchedIndexText(index.getIndexText());
        item.setMatchedIndexType(index.getIndexType());
        item.setMatchedField(matchedField);
        item.setMatchedText(matchedText);
        item.setScore(finalScore == null ? score : finalScore);
        item.setSemanticScore(semanticScore);
        item.setKeywordScore(keywordScore);
        item.setFinalScore(finalScore == null ? score : finalScore);
        item.setHitType(hitType);
        item.setSourceType(document.getSourceType());
        item.setFileType(document.getFileType());
        item.setSortNo(index.getSortNo());
        item.setMetadataJson(record != null && oConvertUtils.isNotEmpty(record.getMetadataJson()) ? record.getMetadataJson() : index.getMetadataJson());
        item.setCreatedAt(record != null ? record.getCreatedAt() : index.getCreatedAt());
        return item;
    }

    /**
     * 合并语义候选。
     *
     * @param aggregates 聚合结果
     * @param chunkId chunk ID
     * @param item 候选结果
     */
    private void mergeSemanticCandidate(Map<String, SearchAggregate> aggregates, String chunkId, KbSemanticSearchItemVO item) {
        SearchAggregate aggregate = aggregates.computeIfAbsent(chunkId, key -> new SearchAggregate());
        boolean accepted = aggregate.addSemantic(item);
        if (!accepted) {
            RetrievalDebugCollector collector = debugCollector();
            if (collector != null) {
                collector.addFilter("duplicated_chunk_filtered");
            }
        }
    }

    /**
     * 合并全文候选。
     *
     * @param aggregates 聚合结果
     * @param chunkId chunk ID
     * @param item 候选结果
     */
    private void mergeKeywordCandidate(Map<String, SearchAggregate> aggregates, String chunkId, KbSemanticSearchItemVO item) {
        SearchAggregate aggregate = aggregates.computeIfAbsent(chunkId, key -> new SearchAggregate());
        boolean accepted = aggregate.addKeyword(item);
        if (!accepted) {
            RetrievalDebugCollector collector = debugCollector();
            if (collector != null) {
                collector.addFilter("duplicated_chunk_filtered");
            }
        }
    }

    /**
     * 解析向量JSON。
     *
     * @param vectorJson 向量JSON
     * @return 向量列表
     */
    private List<Float> parseVector(String vectorJson) {
        if (oConvertUtils.isEmpty(vectorJson)) {
            throw new JeecgBootException("向量库数据异常：vector_json为空");
        }
        try {
            return JSON.parseArray(vectorJson, Float.class);
        } catch (Exception e) {
            throw new JeecgBootException("向量库数据异常：vector_json解析失败");
        }
    }

    /**
     * 计算余弦相似度并映射到0~1区间。
     *
     * @param left 左向量
     * @param right 右向量
     * @return 相似度
     */
    private double cosineSimilarity(List<Float> left, List<Float> right) {
        double dot = 0d;
        double leftNorm = 0d;
        double rightNorm = 0d;
        for (int i = 0; i < left.size(); i++) {
            double l = left.get(i);
            double r = right.get(i);
            dot += l * r;
            leftNorm += l * l;
            rightNorm += r * r;
        }
        if (leftNorm <= 0d || rightNorm <= 0d) {
            return 0d;
        }
        double cosine = dot / (Math.sqrt(leftNorm) * Math.sqrt(rightNorm));
        if (Double.isNaN(cosine) || Double.isInfinite(cosine)) {
            cosine = 0d;
        }
        double score = (cosine + 1d) / 2d;
        if (score < 0d) {
            return 0d;
        }
        if (score > 1d) {
            return 1d;
        }
        return score;
    }

    /**
     * 规整相似度分数。
     *
     * @param similarity 相似度
     * @return 分数
     */
    private BigDecimal normalizeScore(double similarity) {
        return BigDecimal.valueOf(similarity).setScale(6, java.math.RoundingMode.HALF_UP);
    }

    /**
     * 解析检索模式。
     *
     * @param requestMode 请求模式
     * @param configMode 配置模式
     * @return 有效模式
     */
    private String resolveSearchMode(String requestMode, String configMode) {
        String mode = normalizeSearchMode(requestMode);
        if (mode == null) {
            mode = normalizeSearchMode(configMode);
        }
        return mode == null ? KbConstants.SEARCH_MODE_SEMANTIC : mode;
    }

    /**
     * 标准化检索模式。
     *
     * @param mode 模式
     * @return 标准化结果
     */
    private String normalizeSearchMode(String mode) {
        if (oConvertUtils.isEmpty(mode)) {
            return null;
        }
        String value = mode.trim().toLowerCase(Locale.ROOT);
        if (KbConstants.SEARCH_MODE_SEMANTIC.equals(value) || KbConstants.SEARCH_MODE_FULLTEXT.equals(value) || KbConstants.SEARCH_MODE_HYBRID.equals(value)) {
            return value;
        }
        return null;
    }

    /**
     * 规整全文阈值。
     *
     * @param requestThreshold 请求阈值
     * @return 阈值
     */
    private BigDecimal resolveKeywordThreshold(BigDecimal requestThreshold) {
        BigDecimal threshold = requestThreshold == null ? BigDecimal.ZERO : requestThreshold;
        if (threshold.compareTo(BigDecimal.ZERO) < 0 || threshold.compareTo(BigDecimal.ONE) > 0) {
            throw new JeecgBootException("keyword_threshold不合法");
        }
        return threshold;
    }

    /**
     * 规整权重。
     *
     * @param searchMode 搜索模式
     * @param semanticWeight 请求语义权重
     * @param keywordWeight 请求关键词权重
     * @return 权重对
     */
    private WeightPair resolveWeights(String searchMode, BigDecimal semanticWeight, BigDecimal keywordWeight) {
        BigDecimal semantic = semanticWeight == null ? new BigDecimal("0.5") : semanticWeight;
        BigDecimal keyword = keywordWeight == null ? new BigDecimal("0.5") : keywordWeight;
        if (semantic.compareTo(BigDecimal.ZERO) < 0) {
            throw new JeecgBootException("semantic_weight不能小于0");
        }
        if (keyword.compareTo(BigDecimal.ZERO) < 0) {
            throw new JeecgBootException("keyword_weight不能小于0");
        }
        if (semantic.compareTo(BigDecimal.ZERO) == 0 && keyword.compareTo(BigDecimal.ZERO) == 0) {
            throw new JeecgBootException("semantic_weight和keyword_weight不能同时为0");
        }
        if (KbConstants.SEARCH_MODE_SEMANTIC.equals(searchMode)) {
            return new WeightPair(BigDecimal.ONE, BigDecimal.ZERO);
        }
        if (KbConstants.SEARCH_MODE_FULLTEXT.equals(searchMode)) {
            return new WeightPair(BigDecimal.ZERO, BigDecimal.ONE);
        }
        BigDecimal sum = semantic.add(keyword);
        if (sum.compareTo(BigDecimal.ONE) != 0) {
            semantic = semantic.divide(sum, 6, java.math.RoundingMode.HALF_UP);
            keyword = keyword.divide(sum, 6, java.math.RoundingMode.HALF_UP);
        }
        return new WeightPair(semantic, keyword);
    }

    /**
     * 加载检索上下文。
     *
     * @param kbId 知识库ID
     * @return 上下文
     */
    private SearchContext loadSearchContext(String kbId) {
        List<KbChunkIndex> indexes = kbChunkIndexMapper.selectList(new LambdaQueryWrapper<KbChunkIndex>()
                .eq(KbChunkIndex::getKbId, kbId)
                .eq(KbChunkIndex::getStatus, KbConstants.STATUS_ENABLE));
        Map<String, KbChunkIndex> indexMap = indexes.stream().collect(Collectors.toMap(KbChunkIndex::getId, item -> item, (left, right) -> left, LinkedHashMap::new));

        Set<String> chunkIds = indexes.stream()
                .map(KbChunkIndex::getChunkId)
                .filter(oConvertUtils::isNotEmpty)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        List<KbChunk> chunks = chunkIds.isEmpty() ? Collections.emptyList() : kbChunkMapper.selectList(new LambdaQueryWrapper<KbChunk>()
                .in(KbChunk::getId, chunkIds)
                .eq(KbChunk::getKbId, kbId)
                .eq(KbChunk::getStatus, KbConstants.STATUS_ENABLE));
        Map<String, KbChunk> chunkMap = chunks.stream().collect(Collectors.toMap(KbChunk::getId, item -> item, (left, right) -> left, LinkedHashMap::new));

        Set<String> documentIds = chunks.stream()
                .map(KbChunk::getDocumentId)
                .filter(oConvertUtils::isNotEmpty)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        List<KbDocument> documents = documentIds.isEmpty() ? Collections.emptyList() : kbDocumentMapper.selectList(new LambdaQueryWrapper<KbDocument>()
                .in(KbDocument::getId, documentIds)
                .eq(KbDocument::getKbId, kbId)
                .eq(KbDocument::getStatus, KbConstants.STATUS_ENABLE));
        Map<String, KbDocument> documentMap = documents.stream().collect(Collectors.toMap(KbDocument::getId, item -> item, (left, right) -> left, LinkedHashMap::new));

        return new SearchContext(indexMap, chunkMap, documentMap);
    }

    /**
     * 构建关键词候选词。
     *
     * @param query 查询内容
     * @return 候选词列表
     */
    private List<String> buildKeywordTerms(String query) {
        String normalized = normalizeText(query);
        if (oConvertUtils.isEmpty(normalized)) {
            return Collections.emptyList();
        }
        Set<String> terms = new LinkedHashSet<>();
        terms.add(normalized);
        String[] parts = normalized.split("[\\s,，。.!?！？；;、/\\\\|()\\[\\]{}<>\"'`~@#$%^&+=]+");
        for (String part : parts) {
            addKeywordTerm(terms, part);
            if (terms.size() >= 48) {
                break;
            }
        }
        return new ArrayList<>(terms);
    }

    /**
     * 增加关键词候选词。
     *
     * @param terms 候选词集合
     * @param rawTerm 原始词
     */
    private void addKeywordTerm(Set<String> terms, String rawTerm) {
        String term = normalizeText(rawTerm);
        if (oConvertUtils.isEmpty(term)) {
            return;
        }
        terms.add(term);
        List<String> chineseSegments = extractChineseSegments(term);
        for (String segment : chineseSegments) {
            terms.add(segment);
            if (segment.length() <= 6) {
                continue;
            }
            for (int size = 2; size <= 6; size++) {
                for (int i = 0; i + size <= segment.length(); i++) {
                    terms.add(segment.substring(i, i + size));
                    if (terms.size() >= 48) {
                        return;
                    }
                }
            }
        }
    }

    /**
     * 提取中文片段。
     *
     * @param text 文本
     * @return 中文片段
     */
    private List<String> extractChineseSegments(String text) {
        List<String> result = new ArrayList<>();
        Matcher matcher = Pattern.compile("[\\u4e00-\\u9fa5]+").matcher(text);
        while (matcher.find()) {
            String segment = matcher.group();
            if (segment.length() >= 2) {
                result.add(segment);
            }
        }
        return result;
    }

    /**
     * 归一化检索文本。
     *
     * @param text 文本
     * @return 归一化结果
     */
    private String normalizeText(String text) {
        if (oConvertUtils.isEmpty(text)) {
            return null;
        }
        String value = text.trim().toLowerCase(Locale.ROOT);
        value = value.replaceAll("\\s+", " ");
        return value;
    }

    /**
     * 评估关键词命中。
     *
     * @param query 查询内容
     * @param terms 候选词
     * @param indexText 索引文本
     * @param content chunk内容
     * @return 命中结果
     */
    private KeywordMatch evaluateKeywordMatch(String query, List<String> terms, String indexText, String content) {
        KeywordMatch indexMatch = evaluateKeywordField(query, terms, indexText, KbConstants.MATCHED_FIELD_INDEX_TEXT);
        KeywordMatch contentMatch = evaluateKeywordField(query, terms, content, KbConstants.MATCHED_FIELD_CONTENT);
        if (indexMatch == null) {
            return contentMatch;
        }
        if (contentMatch == null) {
            return indexMatch;
        }
        return indexMatch.score.compareTo(contentMatch.score) >= 0 ? indexMatch : contentMatch;
    }

    /**
     * 评估单字段关键词命中。
     *
     * @param query 查询内容
     * @param terms 候选词
     * @param text 字段文本
     * @param field 字段名
     * @return 命中结果
     */
    private KeywordMatch evaluateKeywordField(String query, List<String> terms, String text, String field) {
        String normalizedText = normalizeText(text);
        if (oConvertUtils.isEmpty(normalizedText)) {
            return null;
        }
        String normalizedQuery = normalizeText(query);
        String bestTerm = null;
        String bestMatchedText = null;
        BigDecimal bestScore = BigDecimal.ZERO;
        int bestPosition = -1;

        if (normalizedText.contains(normalizedQuery)) {
            bestTerm = normalizedQuery;
            bestScore = BigDecimal.ONE;
            bestPosition = normalizedText.indexOf(normalizedQuery);
            bestMatchedText = buildSnippet(text, bestPosition, normalizedQuery.length());
        }

        for (String term : terms) {
            if (oConvertUtils.isEmpty(term)) {
                continue;
            }
            int position = normalizedText.indexOf(term);
            if (position < 0) {
                continue;
            }
            BigDecimal score = buildKeywordScore(normalizedQuery, term);
            if (score.compareTo(bestScore) > 0) {
                bestScore = score;
                bestTerm = term;
                bestPosition = position;
                bestMatchedText = buildSnippet(text, position, term.length());
            }
        }

        if (bestScore.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }
        KeywordMatch match = new KeywordMatch();
        match.matchedField = field;
        match.matchedTerm = bestTerm;
        match.matchedText = bestMatchedText == null ? text : bestMatchedText;
        match.score = bestScore;
        match.position = bestPosition;
        return match;
    }

    /**
     * 构建关键词得分。
     *
     * @param query 查询内容
     * @param term 命中词
     * @return 分数
     */
    private BigDecimal buildKeywordScore(String query, String term) {
        if (oConvertUtils.isEmpty(term)) {
            return BigDecimal.ZERO;
        }
        if (oConvertUtils.isNotEmpty(query) && query.equals(term)) {
            return BigDecimal.ONE;
        }
        int queryLength = oConvertUtils.isEmpty(query) ? term.length() : query.length();
        int termLength = term.length();
        double ratio = (double) termLength / Math.max(queryLength, termLength);
        double score = 0.35d + 0.65d * ratio;
        if (score > 1d) {
            score = 1d;
        }
        if (score < 0d) {
            score = 0d;
        }
        return BigDecimal.valueOf(score).setScale(6, java.math.RoundingMode.HALF_UP);
    }

    /**
     * 构建命中片段。
     *
     * @param text 原始文本
     * @param position 命中位置
     * @param termLength 命中词长度
     * @return 片段
     */
    private String buildSnippet(String text, int position, int termLength) {
        if (oConvertUtils.isEmpty(text)) {
            return null;
        }
        int start = Math.max(0, position - 16);
        int end = Math.min(text.length(), position + termLength + 16);
        return text.substring(start, end);
    }

    /**
     * 比较文本。
     *
     * @param left 左侧
     * @param right 右侧
     * @return 比较结果
     */
    private static int compareText(String left, String right) {
        if (left == null && right == null) {
            return 0;
        }
        if (left == null) {
            return 1;
        }
        if (right == null) {
            return -1;
        }
        return left.compareTo(right);
    }

    /**
     * 判断搜索模式是否为语义。
     *
     * @param searchMode 搜索模式
     * @return 是否语义
     */
    private boolean isSemanticMode(String searchMode) {
        return KbConstants.SEARCH_MODE_SEMANTIC.equals(searchMode);
    }

    /**
     * 判断搜索模式是否为全文。
     *
     * @param searchMode 搜索模式
     * @return 是否全文
     */
    private boolean isFulltextMode(String searchMode) {
        return KbConstants.SEARCH_MODE_FULLTEXT.equals(searchMode);
    }

    /**
     * 判断搜索模式是否为混合。
     *
     * @param searchMode 搜索模式
     * @return 是否混合
     */
    private boolean isHybridMode(String searchMode) {
        return KbConstants.SEARCH_MODE_HYBRID.equals(searchMode);
    }

    /**
     * 搜索上下文。
     */
    private static class SearchContext {
        /**
         * 索引映射。
         */
        private final Map<String, KbChunkIndex> indexMap;

        /**
         * chunk映射。
         */
        private final Map<String, KbChunk> chunkMap;

        /**
         * 文档映射。
         */
        private final Map<String, KbDocument> documentMap;

        private SearchContext(Map<String, KbChunkIndex> indexMap, Map<String, KbChunk> chunkMap, Map<String, KbDocument> documentMap) {
            this.indexMap = indexMap;
            this.chunkMap = chunkMap;
            this.documentMap = documentMap;
        }
    }

    /**
     * 权重对。
     */
    private static class WeightPair {
        /**
         * 语义权重。
         */
        private final BigDecimal semanticWeight;

        /**
         * 关键词权重。
         */
        private final BigDecimal keywordWeight;

        private WeightPair(BigDecimal semanticWeight, BigDecimal keywordWeight) {
            this.semanticWeight = semanticWeight;
            this.keywordWeight = keywordWeight;
        }
    }

    /**
     * 关键词命中结果。
     */
    private static class KeywordMatch {
        /**
         * 命中字段。
         */
        private String matchedField;

        /**
         * 命中词。
         */
        private String matchedTerm;

        /**
         * 命中片段。
         */
        private String matchedText;

        /**
         * 命中分数。
         */
        private BigDecimal score;

        /**
         * 位置。
         */
        private int position;
    }

    /**
     * 聚合结果。
     */
    private static class SearchAggregate {
        /**
         * 语义候选。
         */
        private KbSemanticSearchItemVO semanticItem;

        /**
         * 语义分。
         */
        private BigDecimal semanticScore = BigDecimal.ZERO;

        /**
         * 全文候选。
         */
        private KbSemanticSearchItemVO keywordItem;

        /**
         * 全文分。
         */
        private BigDecimal keywordScore = BigDecimal.ZERO;

        /**
         * 增加语义候选。
         *
         * @param item 候选
         */
        private boolean addSemantic(KbSemanticSearchItemVO item) {
            if (item == null) {
                return false;
            }
            if (semanticItem == null || item.getScore().compareTo(semanticScore) > 0 || (item.getScore().compareTo(semanticScore) == 0 && betterOrder(item, semanticItem))) {
                semanticItem = item;
                semanticScore = item.getScore() == null ? BigDecimal.ZERO : item.getScore();
                return true;
            }
            return false;
        }

        /**
         * 增加全文候选。
         *
         * @param item 候选
         */
        private boolean addKeyword(KbSemanticSearchItemVO item) {
            if (item == null) {
                return false;
            }
            if (keywordItem == null || item.getScore().compareTo(keywordScore) > 0 || (item.getScore().compareTo(keywordScore) == 0 && betterOrder(item, keywordItem))) {
                keywordItem = item;
                keywordScore = item.getScore() == null ? BigDecimal.ZERO : item.getScore();
                return true;
            }
            return false;
        }

        /**
         * 构建最终结果。
         *
         * @param searchMode 搜索模式
         * @param weights 权重
         * @return 结果
         */
        private KbSemanticSearchItemVO build(String searchMode, WeightPair weights) {
            KbSemanticSearchItemVO base = chooseBaseItem();
            if (base == null) {
                return null;
            }
            BigDecimal semantic = semanticItem == null ? BigDecimal.ZERO : semanticScore;
            BigDecimal keyword = keywordItem == null ? BigDecimal.ZERO : keywordScore;
            BigDecimal finalScore;
            String hitType;
            if (KbConstants.SEARCH_MODE_SEMANTIC.equals(searchMode)) {
                finalScore = semantic;
                hitType = KbConstants.HIT_TYPE_SEMANTIC;
            } else if (KbConstants.SEARCH_MODE_FULLTEXT.equals(searchMode)) {
                finalScore = keyword;
                hitType = KbConstants.HIT_TYPE_FULLTEXT;
            } else {
                finalScore = semantic.multiply(weights.semanticWeight).add(keyword.multiply(weights.keywordWeight)).setScale(6, java.math.RoundingMode.HALF_UP);
                if (semantic.compareTo(BigDecimal.ZERO) > 0 && keyword.compareTo(BigDecimal.ZERO) > 0) {
                    hitType = KbConstants.HIT_TYPE_HYBRID;
                } else if (semantic.compareTo(BigDecimal.ZERO) > 0) {
                    hitType = KbConstants.HIT_TYPE_SEMANTIC;
                } else {
                    hitType = KbConstants.HIT_TYPE_FULLTEXT;
                }
            }
            KbSemanticSearchItemVO item = new KbSemanticSearchItemVO();
            BeanUtils.copyProperties(base, item);
            item.setSemanticScore(semantic);
            item.setKeywordScore(keyword);
            item.setFinalScore(finalScore);
            item.setScore(finalScore);
            item.setHitType(hitType);
            return item;
        }

        /**
         * 选择展示结果。
         *
         * @return 候选
         */
        private KbSemanticSearchItemVO chooseBaseItem() {
            if (semanticItem == null) {
                return keywordItem;
            }
            if (keywordItem == null) {
                return semanticItem;
            }
            int compare = semanticScore.compareTo(keywordScore);
            if (compare > 0) {
                return semanticItem;
            }
            if (compare < 0) {
                return keywordItem;
            }
            return betterOrder(semanticItem, keywordItem) ? semanticItem : keywordItem;
        }

        /**
         * 判断顺序优先级。
         *
         * @param left 左
         * @param right 右
         * @return 是否更优先
         */
        private boolean betterOrder(KbSemanticSearchItemVO left, KbSemanticSearchItemVO right) {
            if (left == null) {
                return false;
            }
            if (right == null) {
                return true;
            }
            int leftSort = left.getSortNo() == null ? Integer.MAX_VALUE : left.getSortNo();
            int rightSort = right.getSortNo() == null ? Integer.MAX_VALUE : right.getSortNo();
            if (leftSort != rightSort) {
                return leftSort < rightSort;
            }
            return compareText(left.getChunkIndexId(), right.getChunkIndexId()) < 0;
        }
    }

    /**
     * 规整相似度阈值。
     *
     * @param requestThreshold 请求阈值
     * @param configThreshold 配置阈值
     * @return 阈值
     */
    private BigDecimal resolveThreshold(BigDecimal requestThreshold, BigDecimal configThreshold) {
        BigDecimal threshold = requestThreshold != null ? requestThreshold : configThreshold;
        if (threshold == null) {
            threshold = KbConstants.DEFAULT_SIMILARITY_THRESHOLD;
        }
        if (threshold.compareTo(BigDecimal.ZERO) < 0 || threshold.compareTo(BigDecimal.ONE) > 0) {
            throw new JeecgBootException("similarity_threshold不在0~1范围内");
        }
        return threshold;
    }

    /**
     * 规整topK。
     *
     * @param requestTopK 请求topK
     * @param configTopK 配置topK
     * @return topK
     */
    private int resolveTopK(Integer requestTopK, Integer configTopK) {
        int topK = requestTopK != null ? requestTopK : (configTopK == null || configTopK < 1 ? KbConstants.DEFAULT_TOP_K : configTopK);
        if (topK <= 0) {
            throw new JeecgBootException("top_k必须大于0");
        }
        return topK;
    }

    /**
     * 规整最终返回数量。
     *
     * @param requestFinalTopK 请求最终返回数量
     * @param requestTopK 请求topK
     * @param configFinalTopK 配置最终返回数量
     * @param configTopK 配置topK
     * @return 最终返回数量
     */
    private int resolveFinalTopK(Integer requestFinalTopK, Integer requestTopK, Integer configFinalTopK, Integer configTopK) {
        Integer finalTopK = requestFinalTopK;
        if (finalTopK != null && finalTopK <= 0) {
            throw new JeecgBootException("final_top_k必须大于0");
        }
        if (finalTopK == null) {
            finalTopK = requestTopK;
        }
        if (finalTopK == null && configFinalTopK != null && configFinalTopK > 0) {
            finalTopK = configFinalTopK;
        }
        if (finalTopK == null) {
            finalTopK = configTopK;
        }
        if (finalTopK == null) {
            finalTopK = KbConstants.DEFAULT_TOP_K;
        }
        if (finalTopK <= 0) {
            throw new JeecgBootException("final_top_k必须大于0");
        }
        return finalTopK;
    }

    /**
     * 规整是否启用Rerank。
     *
     * @param requestUseRerank 请求值
     * @param configUseRerank 配置值
     * @return 是否启用
     */
    private boolean resolveUseRerank(Boolean requestUseRerank, Boolean configUseRerank) {
        if (requestUseRerank != null) {
            return requestUseRerank;
        }
        return configUseRerank != null ? configUseRerank : KbConstants.DEFAULT_USE_RERANK;
    }

    /**
     * 规整是否启用Query Optimization。
     *
     * @param requestUseQueryOptimization 请求值
     * @param configUseQueryOptimization 配置值
     * @return 是否启用
     */
    private boolean resolveUseQueryOptimization(Boolean requestUseQueryOptimization, Boolean configUseQueryOptimization) {
        if (requestUseQueryOptimization != null) {
            return requestUseQueryOptimization;
        }
        return configUseQueryOptimization != null ? configUseQueryOptimization : KbConstants.DEFAULT_USE_QUERY_OPTIMIZATION;
    }

    /**
     * 规整查询优化模式。
     *
     * @param requestMode 请求值
     * @param configMode 配置值
     * @return 优化模式
     */
    private String resolveQueryOptimizationMode(String requestMode, String configMode) {
        String mode = normalizeQueryOptimizationMode(requestMode);
        if (mode == null) {
            mode = normalizeQueryOptimizationMode(configMode);
        }
        return mode == null ? KbConstants.DEFAULT_QUERY_OPTIMIZATION_MODE : mode;
    }

    /**
     * 标准化查询优化模式。
     *
     * @param mode 模式
     * @return 标准化结果
     */
    private String normalizeQueryOptimizationMode(String mode) {
        if (oConvertUtils.isEmpty(mode)) {
            return null;
        }
        String value = mode.trim().toLowerCase(Locale.ROOT);
        if (KbConstants.QUERY_OPTIMIZATION_MODE_OFF.equals(value)
                || KbConstants.DEFAULT_QUERY_OPTIMIZATION_MODE.equals(value)
                || "keywords".equals(value)
                || "expand".equals(value)
                || KbConstants.SEARCH_MODE_HYBRID.equals(value)) {
            return value;
        }
        return null;
    }

    /**
     * 规整最大生成query数量。
     *
     * @param requestMax 请求值
     * @param configMax 配置值
     * @return 最大数量
     */
    private int resolveMaxRewriteQueries(Integer requestMax, Integer configMax) {
        int max = requestMax != null ? requestMax : (configMax == null || configMax < 1 ? KbConstants.DEFAULT_MAX_REWRITE_QUERIES : configMax);
        if (max <= 0) {
            throw new JeecgBootException("max_rewrite_queries必须大于0");
        }
        if (max > KbConstants.MAX_REWRITE_QUERIES_LIMIT) {
            throw new JeecgBootException("max_rewrite_queries不能超过" + KbConstants.MAX_REWRITE_QUERIES_LIMIT);
        }
        return max;
    }

    /**
     * 规整是否保留原始query。
     *
     * @param requestKeepOriginalQuery 请求值
     * @param configKeepOriginalQuery 配置值
     * @return 是否保留
     */
    private boolean resolveKeepOriginalQuery(Boolean requestKeepOriginalQuery, Boolean configKeepOriginalQuery) {
        if (requestKeepOriginalQuery != null) {
            return requestKeepOriginalQuery;
        }
        return configKeepOriginalQuery != null ? configKeepOriginalQuery : KbConstants.DEFAULT_KEEP_ORIGINAL_QUERY;
    }

    /**
     * 规整Rerank模型。
     *
     * @param requestRerankModel 请求模型
     * @param searchConfig 搜索配置
     * @return 模型名称
     */
    private String resolveRerankModel(String requestRerankModel, KbSearchConfigVo searchConfig) {
        if (oConvertUtils.isNotEmpty(requestRerankModel)) {
            return requestRerankModel;
        }
        if (searchConfig == null) {
            return null;
        }
        return searchConfig.getRerankModel();
    }

    /**
     * 规整Rerank候选数量。
     *
     * @param requestRerankTopN 请求值
     * @param configRerankTopN 配置值
     * @return Rerank候选数量
     */
    private int resolveRerankTopN(Integer requestRerankTopN, Integer configRerankTopN) {
        int rerankTopN = requestRerankTopN != null ? requestRerankTopN : (configRerankTopN == null || configRerankTopN < 1 ? KbConstants.DEFAULT_RERANK_TOP_N : configRerankTopN);
        if (rerankTopN <= 0) {
            throw new JeecgBootException("rerank_top_n必须大于0");
        }
        return rerankTopN;
    }

    /**
     * 规整Rerank分数阈值。
     *
     * @param requestThreshold 请求阈值
     * @param configThreshold 配置阈值
     * @return Rerank分数阈值
     */
    private BigDecimal resolveRerankScoreThreshold(BigDecimal requestThreshold, BigDecimal configThreshold) {
        BigDecimal threshold = requestThreshold != null ? requestThreshold : configThreshold;
        if (threshold == null) {
            return null;
        }
        if (threshold.compareTo(BigDecimal.ZERO) < 0 || threshold.compareTo(BigDecimal.ONE) > 0) {
            throw new JeecgBootException("rerank_score_threshold不合法");
        }
        return threshold;
    }

    /**
     * 规整参考文本上限。
     *
     * @param requestReferenceLimit 请求值
     * @param configReferenceLimit 配置值
     * @return 参考文本上限
     */
    private int resolveReferenceLimit(Integer requestReferenceLimit, Integer configReferenceLimit) {
        int referenceLimit = requestReferenceLimit != null ? requestReferenceLimit : (configReferenceLimit == null || configReferenceLimit < 1 ? KbConstants.DEFAULT_REFERENCE_LIMIT : configReferenceLimit);
        if (referenceLimit <= 0) {
            throw new JeecgBootException("reference_limit必须大于0");
        }
        return referenceLimit;
    }

    /**
     * 进行Rerank和参考文本限制处理。
     *
     * @param query 查询内容
     * @param candidates 候选结果
     * @param useRerank 是否启用Rerank
     * @param rerankTopN Rerank候选数量
     * @param rerankScoreThreshold Rerank阈值
     * @param finalTopK 最终返回数量
     * @param referenceLimit 参考文本上限
     * @return 最终结果
     */
    private List<KbSemanticSearchItemVO> applyRerankAndReferenceLimit(String query,
                                                                       List<KbSemanticSearchItemVO> candidates,
                                                                       boolean useRerank,
                                                                       String rerankModel,
                                                                       int rerankTopN,
                                                                       BigDecimal rerankScoreThreshold,
                                                                       int finalTopK,
                                                                       int referenceLimit) {
        RetrievalDebugCollector collector = debugCollector();
        if (collector != null) {
            collector.markExecuted("rerank", useRerank);
            collector.markExecuted("reference_limit", true);
        }
        if (candidates == null || candidates.isEmpty()) {
            return Collections.emptyList();
        }
        long rerankStart = System.nanoTime();
        List<KbSemanticSearchItemVO> ranked = useRerank
                ? rerankCandidates(query, candidates, rerankModel, rerankTopN, rerankScoreThreshold)
                : new ArrayList<>(candidates);
        if (collector != null && useRerank) {
            collector.recordDuration("rerank_ms", rerankStart);
            collector.setCandidateCount("rerank_candidate_count", ranked.size());
        }
        if (ranked.isEmpty()) {
            return Collections.emptyList();
        }
        if (finalTopK > 0 && ranked.size() > finalTopK) {
            ranked = new ArrayList<>(ranked.subList(0, finalTopK));
        }
        long referenceStart = System.nanoTime();
        List<KbSemanticSearchItemVO> results = applyReferenceLimit(ranked, referenceLimit);
        if (collector != null) {
            collector.recordDuration("reference_limit_ms", referenceStart);
            collector.setCandidateCount("reference_retained_count", results.size());
        }
        return results;
    }

    /**
     * 执行Rerank。
     *
     * @param query 查询内容
     * @param candidates 候选结果
     * @param rerankTopN Rerank候选数量
     * @param rerankScoreThreshold Rerank分数阈值
     * @return Rerank后的结果
     */
    private List<KbSemanticSearchItemVO> rerankCandidates(String query,
                                                          List<KbSemanticSearchItemVO> candidates,
                                                          String rerankModel,
                                                          int rerankTopN,
                                                          BigDecimal rerankScoreThreshold) {
        if (candidates == null || candidates.isEmpty()) {
            return Collections.emptyList();
        }
        int size = Math.min(rerankTopN, candidates.size());
        List<KbSemanticSearchItemVO> pool = new ArrayList<>(candidates.subList(0, size));
        RetrievalDebugCollector collector = debugCollector();
        if (collector != null) {
            collector.setCandidateCount("rerank_candidate_count", pool.size());
        }
        if (pool.isEmpty()) {
            return Collections.emptyList();
        }
        if (oConvertUtils.isEmpty(rerankModel)) {
            rerankModel = "heuristic";
        }
        Set<String> queryTerms = new LinkedHashSet<>(buildKeywordTerms(query));
        String normalizedQuery = normalizeText(query);
        for (KbSemanticSearchItemVO item : pool) {
            BigDecimal rerankScore = calculateRerankScore(normalizedQuery, queryTerms, item);
            item.setRerankScore(rerankScore);
            item.setScore(rerankScore);
        }
        if (rerankScoreThreshold != null) {
            List<KbSemanticSearchItemVO> filtered = new ArrayList<>();
            for (KbSemanticSearchItemVO item : pool) {
                if (item != null && item.getRerankScore() != null && item.getRerankScore().compareTo(rerankScoreThreshold) >= 0) {
                    filtered.add(item);
                } else if (collector != null) {
                    collector.addFilter("below_rerank_threshold");
                }
            }
            pool = filtered;
        }
        pool.sort((left, right) -> {
            BigDecimal leftScore = left.getRerankScore() == null ? BigDecimal.ZERO : left.getRerankScore();
            BigDecimal rightScore = right.getRerankScore() == null ? BigDecimal.ZERO : right.getRerankScore();
            int scoreCompare = rightScore.compareTo(leftScore);
            if (scoreCompare != 0) {
                return scoreCompare;
            }
            BigDecimal leftFinal = left.getFinalScore() == null ? BigDecimal.ZERO : left.getFinalScore();
            BigDecimal rightFinal = right.getFinalScore() == null ? BigDecimal.ZERO : right.getFinalScore();
            int finalCompare = rightFinal.compareTo(leftFinal);
            if (finalCompare != 0) {
                return finalCompare;
            }
            int leftSort = left.getSortNo() == null ? Integer.MAX_VALUE : left.getSortNo();
            int rightSort = right.getSortNo() == null ? Integer.MAX_VALUE : right.getSortNo();
            if (leftSort != rightSort) {
                return Integer.compare(leftSort, rightSort);
            }
            return compareText(left.getChunkIndexId(), right.getChunkIndexId());
        });
        return pool;
    }

    /**
     * 计算Rerank分数。
     *
     * @param normalizedQuery 归一化后的查询
     * @param queryTerms 查询词
     * @param item 结果项
     * @return Rerank分数
     */
    private BigDecimal calculateRerankScore(String normalizedQuery,
                                            Set<String> queryTerms,
                                            KbSemanticSearchItemVO item) {
        if (item == null) {
            return BigDecimal.ZERO;
        }
        StringBuilder builder = new StringBuilder();
        if (oConvertUtils.isNotEmpty(item.getContent())) {
            builder.append(item.getContent()).append(' ');
        }
        if (oConvertUtils.isNotEmpty(item.getMatchedIndexText())) {
            builder.append(item.getMatchedIndexText()).append(' ');
        } else if (oConvertUtils.isNotEmpty(item.getIndexText())) {
            builder.append(item.getIndexText()).append(' ');
        }
        if (oConvertUtils.isNotEmpty(item.getMatchedText())) {
            builder.append(item.getMatchedText());
        }
        String candidateText = normalizeText(builder.toString());
        if (oConvertUtils.isEmpty(candidateText)) {
            return item.getFinalScore() == null ? BigDecimal.ZERO : item.getFinalScore();
        }
        int hitCount = 0;
        for (String term : queryTerms) {
            if (oConvertUtils.isNotEmpty(term) && candidateText.contains(term)) {
                hitCount++;
            }
        }
        double coverage = queryTerms.isEmpty() ? 0d : (double) hitCount / (double) queryTerms.size();
        double baseScore = item.getFinalScore() == null ? (item.getScore() == null ? 0d : item.getScore().doubleValue()) : item.getFinalScore().doubleValue();
        double rerankScore = baseScore * 0.45d + coverage * 0.45d;
        if (oConvertUtils.isNotEmpty(normalizedQuery) && candidateText.contains(normalizedQuery)) {
            rerankScore += 0.10d;
        }
        if (rerankScore > 1d) {
            rerankScore = 1d;
        }
        if (rerankScore < 0d) {
            rerankScore = 0d;
        }
        return BigDecimal.valueOf(rerankScore).setScale(6, java.math.RoundingMode.HALF_UP);
    }

    /**
     * 按参考文本上限裁剪结果。
     *
     * @param ranked 结果列表
     * @param referenceLimit 参考文本上限
     * @return 裁剪后的结果
     */
    private List<KbSemanticSearchItemVO> applyReferenceLimit(List<KbSemanticSearchItemVO> ranked, int referenceLimit) {
        RetrievalDebugCollector collector = debugCollector();
        if (ranked == null || ranked.isEmpty()) {
            return Collections.emptyList();
        }
        if (referenceLimit <= 0) {
            throw new JeecgBootException("reference_limit必须大于0");
        }
        List<KbSemanticSearchItemVO> results = new ArrayList<>();
        int used = 0;
        for (KbSemanticSearchItemVO item : ranked) {
            if (item == null) {
                continue;
            }
            String content = item.getContent();
            if (oConvertUtils.isEmpty(content)) {
                if (collector != null) {
                    collector.addFilter("invalid_status");
                }
                continue;
            }
            int length = content.length();
            if (used >= referenceLimit) {
                if (collector != null) {
                    collector.addFilter("over_reference_limit");
                }
                break;
            }
            KbSemanticSearchItemVO copy = new KbSemanticSearchItemVO();
            BeanUtils.copyProperties(item, copy);
            if (used + length <= referenceLimit) {
                copy.setReferenceLength(length);
                results.add(copy);
                used += length;
                continue;
            }
            int remaining = referenceLimit - used;
            if (remaining <= 0) {
                if (collector != null) {
                    collector.addFilter("over_reference_limit");
                }
                break;
            }
            copy.setContent(content.substring(0, Math.min(remaining, content.length())));
            copy.setReferenceLength(copy.getContent() == null ? 0 : copy.getContent().length());
            results.add(copy);
            used += copy.getReferenceLength();
            if (collector != null) {
                collector.addFilter("over_reference_limit");
            }
            break;
        }
        return results;
    }

    /**
     * 生成空结果。
     *
     * @param query query
     * @param topK topK
     * @param threshold 阈值
     * @return 结果
     */
    private KbSemanticSearchResultVO emptyResult(String query, int topK, BigDecimal threshold) {
        KbSemanticSearchResultVO vo = new KbSemanticSearchResultVO();
        vo.setQuery(query);
        vo.setOriginalQuery(query);
        vo.setOptimizedQueries(Collections.emptyList());
        vo.setUsedQueries(Collections.singletonList(query));
        vo.setTopK(topK);
        vo.setFinalTopK(topK);
        vo.setSimilarityThreshold(threshold);
        vo.setUseRerank(Boolean.FALSE);
        vo.setRerankTopN(KbConstants.DEFAULT_RERANK_TOP_N);
        vo.setReferenceLimit(KbConstants.DEFAULT_REFERENCE_LIMIT);
        vo.setUsedReferenceLength(0);
        vo.setUseQueryOptimization(Boolean.FALSE);
        vo.setQueryOptimizationMode(KbConstants.DEFAULT_QUERY_OPTIMIZATION_MODE);
        vo.setResultCount(0);
        vo.setResults(Collections.emptyList());
        return vo;
    }

    /**
     * 确保知识库可用。
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
}
