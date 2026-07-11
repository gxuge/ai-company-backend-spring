package org.jeecg.modules.airag.kb.service.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.common.util.oConvertUtils;
import org.jeecg.modules.airag.kb.consts.KbConstants;
import org.jeecg.modules.airag.kb.dto.KbFederatedSearchQueryDTO;
import org.jeecg.modules.airag.kb.dto.KbSemanticSearchQueryDTO;
import org.jeecg.modules.airag.kb.entity.KbBase;
import org.jeecg.modules.airag.kb.entity.KbExternalKb;
import org.jeecg.modules.airag.kb.entity.KbFederatedRetrievalLog;
import org.jeecg.modules.airag.kb.mapper.KbBaseMapper;
import org.jeecg.modules.airag.kb.service.IKbExternalKbService;
import org.jeecg.modules.airag.kb.service.IKbFederatedRetrievalLogService;
import org.jeecg.modules.airag.kb.service.IKbFederatedSearchService;
import org.jeecg.modules.airag.kb.service.IKbSemanticSearchService;
import org.jeecg.modules.airag.kb.vo.KbSemanticSearchItemVO;
import org.jeecg.modules.airag.kb.vo.KbSemanticSearchResultVO;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 多知识库联邦检索服务实现。
 */
@Service
public class KbFederatedSearchServiceImpl implements IKbFederatedSearchService {
    /**
     * 内部知识库检索服务。
     */
    private final IKbSemanticSearchService kbSemanticSearchService;

    /**
     * 外部知识库服务。
     */
    private final IKbExternalKbService kbExternalKbService;

    /**
     * 联邦检索日志服务。
     */
    private final IKbFederatedRetrievalLogService kbFederatedRetrievalLogService;

    /**
     * 知识库主表Mapper。
     */
    private final KbBaseMapper kbBaseMapper;

    /**
     * HTTP客户端。
     */
    private final HttpClient httpClient = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    /**
     * 构造方法。
     *
     * @param kbSemanticSearchService 内部知识库检索服务
     * @param kbExternalKbService 外部知识库服务
     * @param kbFederatedRetrievalLogService 联邦检索日志服务
     * @param kbBaseMapper 知识库主表Mapper
     */
    public KbFederatedSearchServiceImpl(IKbSemanticSearchService kbSemanticSearchService,
                                        IKbExternalKbService kbExternalKbService,
                                        IKbFederatedRetrievalLogService kbFederatedRetrievalLogService,
                                        KbBaseMapper kbBaseMapper) {
        this.kbSemanticSearchService = kbSemanticSearchService;
        this.kbExternalKbService = kbExternalKbService;
        this.kbFederatedRetrievalLogService = kbFederatedRetrievalLogService;
        this.kbBaseMapper = kbBaseMapper;
    }

    @Override
    public KbSemanticSearchResultVO search(KbFederatedSearchQueryDTO dto) {
        FederatedRuntimeSettings settings = resolveSettings(dto);
        FederatedTrace trace = new FederatedTrace();
        trace.setQuery(dto.getQuery());
        trace.setKbIds(settings.kbIds);
        trace.setExternalKbIds(settings.externalKbIds);
        trace.setActualParams(settings.toActualParams());
        trace.setSearchMode(settings.searchMode);
        trace.setUseQueryOptimization(settings.useQueryOptimization);
        trace.setUseRerank(settings.useRerank);
        trace.setReferenceLimit(settings.referenceLimit);
        trace.setFinalTopK(settings.finalTopK);

        long totalStart = System.nanoTime();
        List<KbSemanticSearchItemVO> candidates = new ArrayList<>();
        List<Map<String, Object>> internalSources = new ArrayList<>();
        List<Map<String, Object>> externalSources = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        Map<String, Integer> filterReasons = new LinkedHashMap<>();
        List<String> optimizedQueries = new ArrayList<>();
        List<String> usedQueries = new ArrayList<>();

        boolean anySuccess = false;
        for (String kbId : settings.kbIds) {
            long start = System.nanoTime();
            Map<String, Object> sourceInfo = new LinkedHashMap<>();
            sourceInfo.put("kb_id", kbId);
            try {
                KbBase kb = ensureKbEnabled(kbId);
                KbSemanticSearchQueryDTO internalDto = buildInternalSearchDto(dto, settings);
                KbSemanticSearchResultVO internalResult = kbSemanticSearchService.search(kbId, internalDto);
                Map<String, Object> internalTrace = kbSemanticSearchService.consumeLastSearchTrace();
                List<KbSemanticSearchItemVO> items = internalResult == null || internalResult.getResults() == null ? Collections.emptyList() : internalResult.getResults();
                if (optimizedQueries.isEmpty() && internalTrace != null) {
                    optimizedQueries = toStringList(internalTrace.get("optimized_queries"));
                }
                if (usedQueries.isEmpty() && internalTrace != null) {
                    usedQueries = toStringList(internalTrace.get("used_queries"));
                }
                for (KbSemanticSearchItemVO item : items) {
                    KbSemanticSearchItemVO copy = copyInternalItem(item, kb, settings.getKbWeight(kbId));
                    candidates.add(copy);
                }
                anySuccess = anySuccess || !items.isEmpty();
                sourceInfo.put("kb_name", kb.getName());
                sourceInfo.put("participated", Boolean.TRUE);
                sourceInfo.put("candidate_count", items.size());
                sourceInfo.put("duration_ms", elapsedMs(start));
                sourceInfo.put("status", "success");
                sourceInfo.put("debug_info", internalTrace == null ? Collections.emptyMap() : internalTrace.get("debug_info"));
            } catch (Exception ex) {
                sourceInfo.put("participated", Boolean.TRUE);
                sourceInfo.put("status", "failed");
                sourceInfo.put("error_message", ex.getMessage());
                sourceInfo.put("duration_ms", elapsedMs(start));
                errors.add("kb_id=" + kbId + ":" + ex.getMessage());
                if (settings.strictExternalFailure) {
                    internalSources.add(sourceInfo);
                    trace.setInternalSources(internalSources);
                    trace.setExternalSources(externalSources);
                    saveLogSafely(dto, null, trace, errors);
                    throw ex instanceof JeecgBootException ? (JeecgBootException) ex : new JeecgBootException(ex.getMessage());
                }
            }
            internalSources.add(sourceInfo);
        }

        for (String externalKbId : settings.externalKbIds) {
            long start = System.nanoTime();
            Map<String, Object> sourceInfo = new LinkedHashMap<>();
            sourceInfo.put("external_kb_id", externalKbId);
            try {
                KbExternalKb externalKb = kbExternalKbService.getEntityByExternalKbId(externalKbId);
                if (externalKb == null || Boolean.FALSE.equals(externalKb.getEnabled())) {
                    throw new JeecgBootException("外部知识库未找到或已禁用");
                }
                List<KbSemanticSearchItemVO> items = searchExternalKb(externalKb, settings);
                for (KbSemanticSearchItemVO item : items) {
                    candidates.add(copyExternalItem(item, externalKb, settings.getKbWeight(externalKbId)));
                }
                sourceInfo.put("external_kb_name", externalKb.getName());
                sourceInfo.put("participated", Boolean.TRUE);
                sourceInfo.put("candidate_count", items.size());
                sourceInfo.put("duration_ms", elapsedMs(start));
                sourceInfo.put("status", "success");
                anySuccess = anySuccess || !items.isEmpty();
            } catch (Exception ex) {
                sourceInfo.put("participated", Boolean.TRUE);
                sourceInfo.put("status", "failed");
                sourceInfo.put("error_message", ex.getMessage());
                sourceInfo.put("duration_ms", elapsedMs(start));
                errors.add("external_kb_id=" + externalKbId + ":" + ex.getMessage());
                if (settings.strictExternalFailure) {
                    externalSources.add(sourceInfo);
                    trace.setInternalSources(internalSources);
                    trace.setExternalSources(externalSources);
                    saveLogSafely(dto, null, trace, errors);
                    throw ex instanceof JeecgBootException ? (JeecgBootException) ex : new JeecgBootException(ex.getMessage());
                }
            }
            externalSources.add(sourceInfo);
        }

        trace.setInternalSources(internalSources);
        trace.setExternalSources(externalSources);

        if (!anySuccess && candidates.isEmpty() && !errors.isEmpty()) {
            saveLogSafely(dto, null, trace, errors);
            throw new JeecgBootException(errors.get(errors.size() - 1));
        }

        long mergeStart = System.nanoTime();
        MergeResult mergeResult = mergeCandidates(candidates, filterReasons);
        trace.setMergeBeforeCount(candidates.size());
        trace.setMergeAfterCount(mergeResult.merged.size());
        trace.setFilterReasons(filterReasons);
        trace.setMergeDurationMs(elapsedMs(mergeStart));

        List<KbSemanticSearchItemVO> results = new ArrayList<>(mergeResult.merged);
        if (settings.finalTopK > 0 && results.size() > settings.finalTopK) {
            results = new ArrayList<>(results.subList(0, settings.finalTopK));
        }
        trace.setRerankBeforeCount(results.size());

        results = applyReferenceLimit(results, settings.referenceLimit, filterReasons);
        trace.setRerankAfterCount(results.size());
        trace.setReferenceFilterCount(filterReasons.getOrDefault("over_reference_limit", 0));
        trace.setUsedReferenceLength(results.stream().mapToInt(item -> item.getReferenceLength() == null ? 0 : item.getReferenceLength()).sum());
        trace.setResultCount(results.size());

        KbSemanticSearchResultVO vo = new KbSemanticSearchResultVO();
        vo.setQuery(dto.getQuery());
        vo.setOriginalQuery(dto.getQuery());
        vo.setOptimizedQueries(optimizedQueries);
        vo.setUsedQueries(usedQueries.isEmpty() ? Collections.singletonList(dto.getQuery()) : usedQueries);
        vo.setKbIds(settings.kbIds);
        vo.setExternalKbIds(settings.externalKbIds);
        vo.setSearchMode(settings.searchMode);
        vo.setActualParams(settings.toActualParams());
        vo.setResultCount(results.size());
        vo.setUsedReferenceLength(trace.getUsedReferenceLength());
        vo.setResults(results);
        vo.setDebugInfo(trace.toMap());
        vo.setTopK(settings.topK);
        vo.setFinalTopK(settings.finalTopK);
        vo.setSimilarityThreshold(settings.similarityThreshold);
        vo.setKeywordThreshold(settings.keywordThreshold);
        vo.setSemanticWeight(settings.semanticWeight);
        vo.setKeywordWeight(settings.keywordWeight);
        vo.setUseQueryOptimization(settings.useQueryOptimization);
        vo.setQueryOptimizationMode(settings.queryOptimizationMode);
        vo.setMaxRewriteQueries(settings.maxRewriteQueries);
        vo.setKeepOriginalQuery(settings.keepOriginalQuery);
        vo.setUseRerank(settings.useRerank);
        vo.setRerankTopN(settings.rerankTopN);
        vo.setRerankScoreThreshold(settings.rerankScoreThreshold);
        vo.setReferenceLimit(settings.referenceLimit);

        saveLogSafely(dto, vo, trace, errors);
        return vo;
    }

    /**
     * 构造内部检索请求。
     *
     * @param dto 原始请求
     * @param settings 运行参数
     * @return 请求
     */
    private KbSemanticSearchQueryDTO buildInternalSearchDto(KbFederatedSearchQueryDTO dto, FederatedRuntimeSettings settings) {
        KbSemanticSearchQueryDTO internalDto = new KbSemanticSearchQueryDTO();
        BeanUtils.copyProperties(dto, internalDto);
        internalDto.setTopK(settings.topK);
        internalDto.setFinalTopK(settings.topK);
        internalDto.setReferenceLimit(settings.internalReferenceLimit);
        return internalDto;
    }

    /**
     * 复制内部结果项。
     *
     * @param item 原始项
     * @param kb 知识库
     * @param weight 权重
     * @return 复制项
     */
    private KbSemanticSearchItemVO copyInternalItem(KbSemanticSearchItemVO item, KbBase kb, BigDecimal weight) {
        KbSemanticSearchItemVO copy = new KbSemanticSearchItemVO();
        BeanUtils.copyProperties(item, copy);
        copy.setSourceScope(KbConstants.SOURCE_SCOPE_INTERNAL);
        copy.setKbId(kb.getId());
        copy.setKbName(kb.getName());
        copy.setExternalKbId(null);
        copy.setExternalKbName(null);
        copy.setExternalResultId(null);
        copy.setMergedScore(resolveMergedScore(copy, weight));
        copy.setScore(copy.getScore() == null ? copy.getMergedScore() : copy.getScore());
        if (oConvertUtils.isEmpty(copy.getDocumentName())) {
            copy.setDocumentName(copy.getTitle());
        }
        return copy;
    }

    /**
     * 复制外部结果项。
     *
     * @param item 原始项
     * @param externalKb 外部知识库配置
     * @param weight 权重
     * @return 复制项
     */
    private KbSemanticSearchItemVO copyExternalItem(KbSemanticSearchItemVO item, KbExternalKb externalKb, BigDecimal weight) {
        KbSemanticSearchItemVO copy = new KbSemanticSearchItemVO();
        BeanUtils.copyProperties(item, copy);
        copy.setSourceScope(KbConstants.SOURCE_SCOPE_EXTERNAL);
        copy.setKbId(null);
        copy.setKbName(null);
        copy.setExternalKbId(externalKb.getExternalKbId());
        copy.setExternalKbName(externalKb.getName());
        copy.setSourceType(KbConstants.SOURCE_TYPE_EXTERNAL);
        copy.setExternalResultId(copy.getExternalResultId() == null ? copy.getChunkIndexId() : copy.getExternalResultId());
        copy.setMergedScore(resolveMergedScore(copy, weight));
        copy.setScore(copy.getScore() == null ? copy.getMergedScore() : copy.getScore());
        if (oConvertUtils.isEmpty(copy.getDocumentName())) {
            copy.setDocumentName(copy.getTitle());
        }
        if (oConvertUtils.isEmpty(copy.getHitType())) {
            copy.setHitType(KbConstants.HIT_TYPE_EXTERNAL);
        }
        return copy;
    }

    /**
     * 计算合并分。
     *
     * @param item 结果项
     * @param weight 权重
     * @return 合并分
     */
    private BigDecimal resolveMergedScore(KbSemanticSearchItemVO item, BigDecimal weight) {
        BigDecimal baseScore = item.getRerankScore() != null ? item.getRerankScore()
                : (item.getFinalScore() != null ? item.getFinalScore() : (item.getScore() == null ? BigDecimal.ZERO : item.getScore()));
        BigDecimal usedWeight = weight == null ? BigDecimal.ONE : weight;
        return baseScore.multiply(usedWeight).setScale(6, java.math.RoundingMode.HALF_UP);
    }

    /**
     * 合并去重。
     *
     * @param candidates 候选结果
     * @param filterReasons 过滤原因
     * @return 合并结果
     */
    private MergeResult mergeCandidates(List<KbSemanticSearchItemVO> candidates, Map<String, Integer> filterReasons) {
        if (candidates == null || candidates.isEmpty()) {
            return new MergeResult(Collections.emptyList());
        }
        Map<String, KbSemanticSearchItemVO> bestByContent = new LinkedHashMap<>();
        for (KbSemanticSearchItemVO item : candidates) {
            if (item == null) {
                continue;
            }
            String contentKey = normalizeContentKey(item.getContent(), item.getTitle(), item.getMatchedText());
            if (oConvertUtils.isEmpty(contentKey)) {
                contentKey = buildUniqueKey(item);
            }
            KbSemanticSearchItemVO existing = bestByContent.get(contentKey);
            if (existing == null || compareMerged(existing, item) < 0) {
                bestByContent.put(contentKey, item);
            } else {
                increment(filterReasons, "duplicated_chunk_filtered");
            }
        }
        List<KbSemanticSearchItemVO> results = new ArrayList<>(bestByContent.values());
        results.sort((left, right) -> {
            int compare = right.getMergedScore().compareTo(left.getMergedScore());
            if (compare != 0) {
                return compare;
            }
            BigDecimal leftScore = left.getScore() == null ? BigDecimal.ZERO : left.getScore();
            BigDecimal rightScore = right.getScore() == null ? BigDecimal.ZERO : right.getScore();
            compare = rightScore.compareTo(leftScore);
            if (compare != 0) {
                return compare;
            }
            int leftSort = left.getSortNo() == null ? Integer.MAX_VALUE : left.getSortNo();
            int rightSort = right.getSortNo() == null ? Integer.MAX_VALUE : right.getSortNo();
            if (leftSort != rightSort) {
                return Integer.compare(leftSort, rightSort);
            }
            return compareTextSafe(left.getChunkIndexId(), right.getChunkIndexId());
        });
        return new MergeResult(results);
    }

    /**
     * 比较合并分。
     *
     * @param left 左侧
     * @param right 右侧
     * @return 比较结果
     */
    private int compareMerged(KbSemanticSearchItemVO left, KbSemanticSearchItemVO right) {
        BigDecimal leftScore = left == null || left.getMergedScore() == null ? BigDecimal.ZERO : left.getMergedScore();
        BigDecimal rightScore = right == null || right.getMergedScore() == null ? BigDecimal.ZERO : right.getMergedScore();
        return leftScore.compareTo(rightScore);
    }

    /**
     * 安全比较字符串。
     *
     * @param left 左值
     * @param right 右值
     * @return 比较结果
     */
    private int compareTextSafe(String left, String right) {
        String leftValue = left == null ? "" : left;
        String rightValue = right == null ? "" : right;
        return leftValue.compareTo(rightValue);
    }

    /**
     * 生成唯一key。
     *
     * @param item 结果项
     * @return key
     */
    private String buildUniqueKey(KbSemanticSearchItemVO item) {
        if (item == null) {
            return null;
        }
        if (oConvertUtils.isNotEmpty(item.getSourceScope()) && KbConstants.SOURCE_SCOPE_EXTERNAL.equals(item.getSourceScope())) {
            return "external:" + java.util.Objects.toString(item.getExternalKbId(), "") + ":" + java.util.Objects.toString(item.getExternalResultId(), "");
        }
        if (oConvertUtils.isNotEmpty(item.getChunkIndexId())) {
            return "internal:" + java.util.Objects.toString(item.getKbId(), "") + ":" + item.getChunkIndexId();
        }
        return "content:" + normalizeContentKey(item.getContent(), item.getTitle(), item.getMatchedText());
    }

    /**
     * 归一化内容key。
     *
     * @param values 内容
     * @return key
     */
    private String normalizeContentKey(String... values) {
        if (values == null || values.length == 0) {
            return null;
        }
        String joined = java.util.Arrays.stream(values)
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(oConvertUtils::isNotEmpty)
                .collect(Collectors.joining(" "));
        if (oConvertUtils.isEmpty(joined)) {
            return null;
        }
        return joined.toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
    }

    /**
     * 应用引用长度限制。
     *
     * @param ranked 结果列表
     * @param referenceLimit 上限
     * @param filterReasons 过滤原因
     * @return 结果
     */
    private List<KbSemanticSearchItemVO> applyReferenceLimit(List<KbSemanticSearchItemVO> ranked,
                                                             int referenceLimit,
                                                             Map<String, Integer> filterReasons) {
        if (ranked == null || ranked.isEmpty()) {
            return Collections.emptyList();
        }
        if (referenceLimit <= 0) {
            throw new JeecgBootException("reference_limit必须大于0");
        }
        List<KbSemanticSearchItemVO> results = new ArrayList<>();
        int used = 0;
        for (KbSemanticSearchItemVO item : ranked) {
            if (item == null || oConvertUtils.isEmpty(item.getContent())) {
                continue;
            }
            if (used >= referenceLimit) {
                increment(filterReasons, "over_reference_limit");
                break;
            }
            KbSemanticSearchItemVO copy = new KbSemanticSearchItemVO();
            BeanUtils.copyProperties(item, copy);
            int contentLength = item.getContent().length();
            if (used + contentLength <= referenceLimit) {
                copy.setReferenceLength(contentLength);
                results.add(copy);
                used += contentLength;
                continue;
            }
            int remaining = referenceLimit - used;
            if (remaining <= 0) {
                increment(filterReasons, "over_reference_limit");
                break;
            }
            copy.setContent(item.getContent().substring(0, remaining));
            copy.setReferenceLength(remaining);
            results.add(copy);
            used += remaining;
            increment(filterReasons, "over_reference_limit");
            break;
        }
        return results;
    }

    /**
     * 查询外部知识库。
     *
     * @param externalKb 外部知识库配置
     * @param settings 运行参数
     * @return 结果
     */
    private List<KbSemanticSearchItemVO> searchExternalKb(KbExternalKb externalKb, FederatedRuntimeSettings settings) {
        Map<String, Object> requestBody = new LinkedHashMap<>();
        requestBody.put("query", settings.originalQuery);
        requestBody.put("top_k", settings.topK);
        requestBody.put("search_mode", settings.searchMode);
        requestBody.put("metadata_filter", settings.metadataFilter);

        String payload = JSON.toJSONString(requestBody);
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(externalKb.getEndpointUrl()))
                .timeout(Duration.ofMillis(externalKb.getTimeoutMs() == null ? 5000 : externalKb.getTimeoutMs()))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8));
        applyAuth(builder, externalKb.getAuthType(), externalKb.getAuthConfig());

        try {
            HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new JeecgBootException("外部知识库调用失败，HTTP状态=" + response.statusCode());
            }
            return parseExternalResults(response.body(), externalKb, settings);
        } catch (JeecgBootException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new JeecgBootException("外部知识库调用失败：" + ex.getMessage());
        }
    }

    /**
     * 解析外部结果。
     *
     * @param body 响应体
     * @param externalKb 外部知识库
     * @return 结果
     */
    private List<KbSemanticSearchItemVO> parseExternalResults(String body, KbExternalKb externalKb, FederatedRuntimeSettings settings) {
        if (oConvertUtils.isEmpty(body)) {
            return Collections.emptyList();
        }
        Object parsed;
        try {
            parsed = JSON.parse(body);
        } catch (Exception ex) {
            throw new JeecgBootException("外部知识库响应解析失败");
        }
        List<JSONObject> items = new ArrayList<>();
        if (parsed instanceof JSONArray) {
            for (Object element : (JSONArray) parsed) {
                if (element instanceof JSONObject) {
                    items.add((JSONObject) element);
                }
            }
        } else if (parsed instanceof JSONObject) {
            JSONObject object = (JSONObject) parsed;
            Object results = object.get("results");
            if (results instanceof JSONArray) {
                for (Object element : (JSONArray) results) {
                    if (element instanceof JSONObject) {
                        items.add((JSONObject) element);
                    }
                }
            } else if (results instanceof List) {
                for (Object element : (List<?>) results) {
                    if (element instanceof JSONObject) {
                        items.add((JSONObject) element);
                    } else if (element instanceof Map) {
                        items.add(new JSONObject((Map<String, Object>) element));
                    }
                }
            } else {
                items.add(object);
            }
        }
        List<KbSemanticSearchItemVO> results = new ArrayList<>();
        for (JSONObject object : items) {
            KbSemanticSearchItemVO item = new KbSemanticSearchItemVO();
            item.setSourceScope(KbConstants.SOURCE_SCOPE_EXTERNAL);
            item.setExternalKbId(externalKb.getExternalKbId());
            item.setExternalKbName(externalKb.getName());
            item.setSourceType(KbConstants.SOURCE_TYPE_EXTERNAL);
            item.setExternalResultId(object.getString("external_result_id"));
            if (oConvertUtils.isEmpty(item.getExternalResultId())) {
                item.setExternalResultId(object.getString("id"));
            }
            item.setContent(object.getString("content"));
            item.setTitle(object.getString("title"));
            item.setDocumentName(oConvertUtils.isNotEmpty(item.getTitle()) ? item.getTitle() : item.getContent());
            item.setMatchedQuery(settings == null ? null : settings.originalQuery);
            item.setMatchedField(oConvertUtils.isNotEmpty(item.getTitle()) ? "title" : "content");
            item.setMatchedText(oConvertUtils.isNotEmpty(item.getTitle()) ? item.getTitle() : item.getContent());
            item.setSourceUrl(object.getString("source_url"));
            item.setScore(object.getBigDecimal("score") == null ? BigDecimal.ZERO : object.getBigDecimal("score"));
            item.setFinalScore(item.getScore());
            item.setSemanticScore(BigDecimal.ZERO);
            item.setKeywordScore(BigDecimal.ZERO);
            item.setMergedScore(resolveMergedScore(item, externalKb.getWeight()));
            item.setHitType(KbConstants.HIT_TYPE_EXTERNAL);
            Object metadata = object.get("metadata_json");
            if (metadata == null) {
                metadata = object.get("metadata");
            }
            item.setMetadataJson(metadata instanceof String ? (String) metadata : JSON.toJSONString(object));
            results.add(item);
        }
        return results;
    }

    /**
     * 应用鉴权。
     *
     * @param builder 请求构建器
     * @param authType 鉴权类型
     * @param authConfig 鉴权配置
     */
    private void applyAuth(HttpRequest.Builder builder, String authType, String authConfig) {
        String type = authType == null ? "none" : authType.toLowerCase(Locale.ROOT);
        if ("none".equals(type)) {
            return;
        }
        String value = parseAuthValue(authConfig, type);
        if (oConvertUtils.isEmpty(value)) {
            return;
        }
        if ("api_key".equals(type)) {
            String headerName = "X-API-Key";
            JSONObject config = safeParseObject(authConfig);
            if (config != null && oConvertUtils.isNotEmpty(config.getString("header_name"))) {
                headerName = config.getString("header_name");
            }
            builder.header(headerName, value);
            return;
        }
        if ("bearer".equals(type)) {
            builder.header("Authorization", "Bearer " + value);
        }
    }

    /**
     * 解析鉴权值。
     *
     * @param authConfig 鉴权配置
     * @param authType 鉴权类型
     * @return 值
     */
    private String parseAuthValue(String authConfig, String authType) {
        if (oConvertUtils.isEmpty(authConfig)) {
            return null;
        }
        JSONObject config = safeParseObject(authConfig);
        if (config == null) {
            return authConfig;
        }
        if ("api_key".equals(authType)) {
            String key = config.getString("api_key");
            if (oConvertUtils.isNotEmpty(key)) {
                return key;
            }
            key = config.getString("value");
            if (oConvertUtils.isNotEmpty(key)) {
                return key;
            }
        }
        if ("bearer".equals(authType)) {
            String token = config.getString("token");
            if (oConvertUtils.isNotEmpty(token)) {
                return token;
            }
            token = config.getString("access_token");
            if (oConvertUtils.isNotEmpty(token)) {
                return token;
            }
        }
        return authConfig;
    }

    /**
     * 安全解析JSON对象。
     *
     * @param json JSON
     * @return 对象
     */
    private JSONObject safeParseObject(String json) {
        if (oConvertUtils.isEmpty(json)) {
            return null;
        }
        try {
            return JSON.parseObject(json);
        } catch (Exception ex) {
            return null;
        }
    }

    /**
     * 保存日志。
     *
     * @param dto 请求
     * @param result 结果
     * @param trace trace
     * @param errors 错误
     */
    private void saveLogSafely(KbFederatedSearchQueryDTO dto, KbSemanticSearchResultVO result, FederatedTrace trace, List<String> errors) {
        try {
            KbFederatedRetrievalLog log = new KbFederatedRetrievalLog();
            Date now = new Date();
            log.setQuery(dto == null ? null : dto.getQuery());
            log.setKbIdsJson(JSON.toJSONString(trace.getKbIds()));
            log.setExternalKbIdsJson(JSON.toJSONString(trace.getExternalKbIds()));
            log.setActualParamsJson(JSON.toJSONString(trace.getActualParams()));
            log.setResultCount(result == null ? 0 : result.getResultCount());
            log.setResultJson(result == null ? "[]" : JSON.toJSONString(result.getResults()));
            log.setDebugJson(JSON.toJSONString(trace.toMap()));
            boolean failed = errors != null && !errors.isEmpty() && (result == null || result.getResultCount() == null || result.getResultCount() <= 0);
            log.setStatus(failed ? KbConstants.LOG_STATUS_FAILED : KbConstants.LOG_STATUS_SUCCESS);
            log.setErrorMessage(errors == null || errors.isEmpty() ? null : JSON.toJSONString(errors));
            log.setCreatedAt(now);
            log.setUpdatedAt(now);
            kbFederatedRetrievalLogService.save(log);
        } catch (Exception ignored) {
            // 日志失败不影响主流程。
        }
    }

    /**
     * 获取知识库实体并校验。
     *
     * @param kbId 知识库ID
     * @return 实体
     */
    private KbBase ensureKbEnabled(String kbId) {
        KbBase kb = kbBaseMapper.selectById(kbId);
        if (kb == null || KbConstants.STATUS_DISABLE.equals(kb.getStatus())) {
            throw new JeecgBootException("未找到对应知识库");
        }
        return kb;
    }

    /**
     * 解析运行参数。
     *
     * @param dto 请求
     * @return 参数
     */
    private FederatedRuntimeSettings resolveSettings(KbFederatedSearchQueryDTO dto) {
        if (dto == null) {
            throw new JeecgBootException("请求不能为空");
        }
        if ((dto.getKbIds() == null || dto.getKbIds().isEmpty()) && (dto.getExternalKbIds() == null || dto.getExternalKbIds().isEmpty())) {
            throw new JeecgBootException("kb_ids和external_kb_ids不能同时为空");
        }
        FederatedRuntimeSettings settings = new FederatedRuntimeSettings();
        settings.originalQuery = dto.getQuery();
        settings.kbIds = distinctOrEmpty(dto.getKbIds());
        settings.externalKbIds = distinctOrEmpty(dto.getExternalKbIds());
        if (settings.kbIds.isEmpty() && settings.externalKbIds.isEmpty()) {
            throw new JeecgBootException("kb_ids和external_kb_ids不能同时为空");
        }
        settings.searchMode = normalizeSearchMode(dto.getSearchMode());
        if (settings.searchMode == null) {
            settings.searchMode = KbConstants.SEARCH_MODE_SEMANTIC;
        }
        settings.topK = resolvePositive(dto.getTopK(), KbConstants.DEFAULT_TOP_K, "top_k必须大于0");
        settings.finalTopK = resolvePositive(dto.getFinalTopK(), settings.topK, "final_top_k必须大于0");
        settings.referenceLimit = resolvePositive(dto.getReferenceLimit(), KbConstants.DEFAULT_REFERENCE_LIMIT, "reference_limit必须大于0");
        settings.internalReferenceLimit = Math.max(settings.referenceLimit, 1000000);
        settings.similarityThreshold = resolveThreshold(dto.getSimilarityThreshold(), KbConstants.DEFAULT_SIMILARITY_THRESHOLD);
        settings.keywordThreshold = resolveThreshold(dto.getKeywordThreshold(), BigDecimal.ZERO);
        settings.semanticWeight = resolveWeight(dto.getSemanticWeight(), new BigDecimal("0.5"), "semantic_weight不能小于0");
        settings.keywordWeight = resolveWeight(dto.getKeywordWeight(), new BigDecimal("0.5"), "keyword_weight不能小于0");
        if (settings.semanticWeight.compareTo(BigDecimal.ZERO) == 0 && settings.keywordWeight.compareTo(BigDecimal.ZERO) == 0) {
            throw new JeecgBootException("semantic_weight和keyword_weight不能同时为0");
        }
        BigDecimal sum = settings.semanticWeight.add(settings.keywordWeight);
        if (sum.compareTo(BigDecimal.ONE) != 0 && sum.compareTo(BigDecimal.ZERO) > 0) {
            settings.semanticWeight = settings.semanticWeight.divide(sum, 6, java.math.RoundingMode.HALF_UP);
            settings.keywordWeight = settings.keywordWeight.divide(sum, 6, java.math.RoundingMode.HALF_UP);
        }
        settings.useQueryOptimization = dto.getUseQueryOptimization() != null ? dto.getUseQueryOptimization() : KbConstants.DEFAULT_USE_QUERY_OPTIMIZATION;
        settings.queryOptimizationMode = normalizeQueryOptimizationMode(dto.getQueryOptimizationMode());
        if (settings.queryOptimizationMode == null) {
            settings.queryOptimizationMode = KbConstants.DEFAULT_QUERY_OPTIMIZATION_MODE;
        }
        settings.maxRewriteQueries = resolvePositive(dto.getMaxRewriteQueries(), KbConstants.DEFAULT_MAX_REWRITE_QUERIES, "max_rewrite_queries必须大于0");
        settings.keepOriginalQuery = dto.getKeepOriginalQuery() != null ? dto.getKeepOriginalQuery() : KbConstants.DEFAULT_KEEP_ORIGINAL_QUERY;
        settings.useRerank = dto.getUseRerank() != null ? dto.getUseRerank() : KbConstants.DEFAULT_USE_RERANK;
        settings.rerankTopN = resolvePositive(dto.getRerankTopN(), KbConstants.DEFAULT_RERANK_TOP_N, "rerank_top_n必须大于0");
        settings.rerankScoreThreshold = resolveThreshold(dto.getRerankScoreThreshold(), null);
        settings.kbWeights = dto.getKbWeights() == null ? new LinkedHashMap<>() : new LinkedHashMap<>(dto.getKbWeights());
        for (Map.Entry<String, BigDecimal> entry : settings.kbWeights.entrySet()) {
            if (entry.getValue() != null && entry.getValue().compareTo(BigDecimal.ZERO) < 0) {
                throw new JeecgBootException("kb_weights不能包含负数");
            }
        }
        settings.strictExternalFailure = dto.getStrictExternalFailure() != null && dto.getStrictExternalFailure();
        settings.metadataFilter = dto.getMetadataFilter() == null ? new LinkedHashMap<>() : new LinkedHashMap<>(dto.getMetadataFilter());
        settings.chatHistory = dto.getChatHistory();
        return settings;
    }

    /**
     * 分配权重。
     *
     * @param sourceId 来源ID
     * @param configWeight 配置权重
     * @return 权重
     */
    private BigDecimal resolveKbWeight(String sourceId, BigDecimal configWeight) {
        if (oConvertUtils.isEmpty(sourceId)) {
            return configWeight == null ? BigDecimal.ONE : configWeight;
        }
        BigDecimal weight = configWeight == null ? BigDecimal.ONE : configWeight;
        if (weight.compareTo(BigDecimal.ZERO) < 0) {
            throw new JeecgBootException("kb_weights不能包含负数");
        }
        return weight;
    }

    /**
     * 归一化检索模式。
     *
     * @param mode 模式
     * @return 模式
     */
    private String normalizeSearchMode(String mode) {
        if (oConvertUtils.isEmpty(mode)) {
            return null;
        }
        String value = mode.trim().toLowerCase(Locale.ROOT);
        if (KbConstants.SEARCH_MODE_SEMANTIC.equals(value) || KbConstants.SEARCH_MODE_FULLTEXT.equals(value) || KbConstants.SEARCH_MODE_HYBRID.equals(value)) {
            return value;
        }
        throw new JeecgBootException("search_mode非法");
    }

    /**
     * 归一化Query Optimization模式。
     *
     * @param mode 模式
     * @return 模式
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
        throw new JeecgBootException("query_optimization_mode非法");
    }

    /**
     * 解析阈值。
     *
     * @param value 值
     * @param defaultValue 默认值
     * @return 阈值
     */
    private BigDecimal resolveThreshold(BigDecimal value, BigDecimal defaultValue) {
        BigDecimal threshold = value == null ? defaultValue : value;
        if (threshold == null) {
            return null;
        }
        if (threshold.compareTo(BigDecimal.ZERO) < 0 || threshold.compareTo(BigDecimal.ONE) > 0) {
            throw new JeecgBootException("阈值不在0~1范围内");
        }
        return threshold;
    }

    /**
     * 解析权重。
     *
     * @param value 值
     * @param defaultValue 默认值
     * @param errorMessage 错误
     * @return 权重
     */
    private BigDecimal resolveWeight(BigDecimal value, BigDecimal defaultValue, String errorMessage) {
        BigDecimal weight = value == null ? defaultValue : value;
        if (weight.compareTo(BigDecimal.ZERO) < 0) {
            throw new JeecgBootException(errorMessage);
        }
        return weight;
    }

    /**
     * 解析正整数。
     *
     * @param value 值
     * @param defaultValue 默认值
     * @param errorMessage 错误
     * @return 值
     */
    private int resolvePositive(Integer value, int defaultValue, String errorMessage) {
        int result = value == null ? defaultValue : value;
        if (result <= 0) {
            throw new JeecgBootException(errorMessage);
        }
        return result;
    }

    /**
     * 去重。
     *
     * @param list 列表
     * @return 列表
     */
    private List<String> distinctOrEmpty(List<String> list) {
        if (list == null || list.isEmpty()) {
            return new ArrayList<>();
        }
        Set<String> set = new LinkedHashSet<>();
        for (String item : list) {
            if (oConvertUtils.isNotEmpty(item)) {
                set.add(item.trim());
            }
        }
        return new ArrayList<>(set);
    }

    /**
     * 转为字符串列表。
     *
     * @param value 值
     * @return 列表
     */
    @SuppressWarnings("unchecked")
    private List<String> toStringList(Object value) {
        if (value == null) {
            return new ArrayList<>();
        }
        if (value instanceof List) {
            List<String> list = new ArrayList<>();
            for (Object item : (List<Object>) value) {
                if (item != null) {
                    list.add(String.valueOf(item));
                }
            }
            return list;
        }
        return new ArrayList<>(Collections.singletonList(String.valueOf(value)));
    }

    /**
     * 记录过滤计数。
     *
     * @param filterReasons 原因
     * @param key key
     */
    private void increment(Map<String, Integer> filterReasons, String key) {
        filterReasons.put(key, filterReasons.getOrDefault(key, 0) + 1);
    }

    /**
     * 计算耗时。
     *
     * @param startNano 起始时间
     * @return 毫秒
     */
    private long elapsedMs(long startNano) {
        return java.util.concurrent.TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNano);
    }

    /**
     * 联邦运行参数。
     */
    private static class FederatedRuntimeSettings {
        private String originalQuery;
        private List<String> kbIds = new ArrayList<>();
        private List<String> externalKbIds = new ArrayList<>();
        private String searchMode;
        private int topK;
        private int finalTopK;
        private int referenceLimit;
        private int internalReferenceLimit;
        private BigDecimal similarityThreshold;
        private BigDecimal keywordThreshold;
        private BigDecimal semanticWeight;
        private BigDecimal keywordWeight;
        private boolean useQueryOptimization;
        private String queryOptimizationMode;
        private int maxRewriteQueries;
        private boolean keepOriginalQuery;
        private boolean useRerank;
        private int rerankTopN;
        private BigDecimal rerankScoreThreshold;
        private Map<String, BigDecimal> kbWeights = new LinkedHashMap<>();
        private Map<String, Object> metadataFilter = new LinkedHashMap<>();
        private List<?> chatHistory;
        private boolean strictExternalFailure;

        private BigDecimal getKbWeight(String sourceId) {
            if (kbWeights != null && kbWeights.containsKey(sourceId) && kbWeights.get(sourceId) != null) {
                return kbWeights.get(sourceId);
            }
            return BigDecimal.ONE;
        }

        private Map<String, Object> toActualParams() {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("kb_ids", kbIds);
            map.put("external_kb_ids", externalKbIds);
            map.put("search_mode", searchMode);
            map.put("top_k", topK);
            map.put("final_top_k", finalTopK);
            map.put("reference_limit", referenceLimit);
            map.put("similarity_threshold", similarityThreshold);
            map.put("keyword_threshold", keywordThreshold);
            map.put("semantic_weight", semanticWeight);
            map.put("keyword_weight", keywordWeight);
            map.put("use_query_optimization", useQueryOptimization);
            map.put("query_optimization_mode", queryOptimizationMode);
            map.put("max_rewrite_queries", maxRewriteQueries);
            map.put("keep_original_query", keepOriginalQuery);
            map.put("use_rerank", useRerank);
            map.put("rerank_top_n", rerankTopN);
            map.put("rerank_score_threshold", rerankScoreThreshold);
            map.put("kb_weights", kbWeights);
            map.put("metadata_filter", metadataFilter);
            map.put("strict_external_failure", strictExternalFailure);
            return map;
        }
    }

    /**
     * 联邦trace。
     */
    private static class FederatedTrace {
        private String query;
        private List<String> kbIds = new ArrayList<>();
        private List<String> externalKbIds = new ArrayList<>();
        private Map<String, Object> actualParams = new LinkedHashMap<>();
        private String searchMode;
        private boolean useQueryOptimization;
        private boolean useRerank;
        private int referenceLimit;
        private int finalTopK;
        private List<Map<String, Object>> internalSources = new ArrayList<>();
        private List<Map<String, Object>> externalSources = new ArrayList<>();
        private int mergeBeforeCount;
        private int mergeAfterCount;
        private int rerankBeforeCount;
        private int rerankAfterCount;
        private int referenceFilterCount;
        private int usedReferenceLength;
        private int resultCount;
        private Map<String, Integer> filterReasons = new LinkedHashMap<>();
        private long mergeDurationMs;

        private void setQuery(String query) {
            this.query = query;
        }

        private void setKbIds(List<String> kbIds) {
            this.kbIds = kbIds == null ? new ArrayList<>() : new ArrayList<>(kbIds);
        }

        private void setExternalKbIds(List<String> externalKbIds) {
            this.externalKbIds = externalKbIds == null ? new ArrayList<>() : new ArrayList<>(externalKbIds);
        }

        private void setActualParams(Map<String, Object> actualParams) {
            this.actualParams = actualParams == null ? new LinkedHashMap<>() : new LinkedHashMap<>(actualParams);
        }

        private Map<String, Object> getActualParams() {
            return actualParams;
        }

        private List<String> getKbIds() {
            return kbIds;
        }

        private List<String> getExternalKbIds() {
            return externalKbIds;
        }

        private void setSearchMode(String searchMode) {
            this.searchMode = searchMode;
        }

        private void setUseQueryOptimization(boolean useQueryOptimization) {
            this.useQueryOptimization = useQueryOptimization;
        }

        private void setUseRerank(boolean useRerank) {
            this.useRerank = useRerank;
        }

        private void setReferenceLimit(int referenceLimit) {
            this.referenceLimit = referenceLimit;
        }

        private void setFinalTopK(int finalTopK) {
            this.finalTopK = finalTopK;
        }

        private void setInternalSources(List<Map<String, Object>> internalSources) {
            this.internalSources = internalSources == null ? new ArrayList<>() : internalSources;
        }

        private void setExternalSources(List<Map<String, Object>> externalSources) {
            this.externalSources = externalSources == null ? new ArrayList<>() : externalSources;
        }

        private void setMergeBeforeCount(int mergeBeforeCount) {
            this.mergeBeforeCount = mergeBeforeCount;
        }

        private void setMergeAfterCount(int mergeAfterCount) {
            this.mergeAfterCount = mergeAfterCount;
        }

        private void setRerankBeforeCount(int rerankBeforeCount) {
            this.rerankBeforeCount = rerankBeforeCount;
        }

        private void setRerankAfterCount(int rerankAfterCount) {
            this.rerankAfterCount = rerankAfterCount;
        }

        private void setReferenceFilterCount(int referenceFilterCount) {
            this.referenceFilterCount = referenceFilterCount;
        }

        private void setUsedReferenceLength(int usedReferenceLength) {
            this.usedReferenceLength = usedReferenceLength;
        }

        private int getUsedReferenceLength() {
            return usedReferenceLength;
        }

        private void setResultCount(int resultCount) {
            this.resultCount = resultCount;
        }

        private void setFilterReasons(Map<String, Integer> filterReasons) {
            this.filterReasons = filterReasons == null ? new LinkedHashMap<>() : new LinkedHashMap<>(filterReasons);
        }

        private void setMergeDurationMs(long mergeDurationMs) {
            this.mergeDurationMs = mergeDurationMs;
        }

        private Map<String, Object> toMap() {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("query", query);
            map.put("kb_ids", kbIds);
            map.put("external_kb_ids", externalKbIds);
            map.put("actual_params", actualParams);
            map.put("search_mode", searchMode);
            map.put("use_query_optimization", useQueryOptimization);
            map.put("use_rerank", useRerank);
            map.put("reference_limit", referenceLimit);
            map.put("final_top_k", finalTopK);
            map.put("internal_sources", internalSources);
            map.put("external_sources", externalSources);
            map.put("merge_before_count", mergeBeforeCount);
            map.put("merge_after_dedup_count", mergeAfterCount);
            map.put("rerank_before_count", rerankBeforeCount);
            map.put("rerank_after_count", rerankAfterCount);
            map.put("reference_limit_filtered_count", referenceFilterCount);
            map.put("used_reference_length", usedReferenceLength);
            map.put("result_count", resultCount);
            map.put("filter_reasons", filterReasons);
            map.put("merge_duration_ms", mergeDurationMs);
            return map;
        }
    }

    /**
     * 合并结果。
     */
    private static class MergeResult {
        private final List<KbSemanticSearchItemVO> merged;

        private MergeResult(List<KbSemanticSearchItemVO> merged) {
            this.merged = merged == null ? Collections.emptyList() : merged;
        }
    }
}
