package org.jeecg.modules.airag.kb.service.impl;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.output.FinishReason;
import dev.langchain4j.service.TokenStream;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.exception.JeecgBootBizTipException;
import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.common.util.AssertUtils;
import org.jeecg.common.util.UUIDGenerator;
import org.jeecg.common.util.oConvertUtils;
import org.jeecg.modules.airag.agent.runtime.AgentContext;
import org.jeecg.modules.airag.agent.runtime.AgentModelResolver;
import org.jeecg.modules.airag.common.handler.IAIChatHandler;
import org.jeecg.modules.airag.kb.consts.KbConstants;
import org.jeecg.modules.airag.kb.dto.KbFederatedSearchQueryDTO;
import org.jeecg.modules.airag.kb.dto.KbQueryOptimizationHistoryDTO;
import org.jeecg.modules.airag.kb.dto.KbRagChatLogQueryDTO;
import org.jeecg.modules.airag.kb.dto.KbRagQuestionDTO;
import org.jeecg.modules.airag.kb.entity.KbRagChatLog;
import org.jeecg.modules.airag.kb.service.IKbFederatedSearchService;
import org.jeecg.modules.airag.kb.service.IKbRagChatLogService;
import org.jeecg.modules.airag.kb.service.IKbRagQaService;
import org.jeecg.modules.airag.kb.vo.KbRagAnswerVO;
import org.jeecg.modules.airag.kb.vo.KbRagChatLogVo;
import org.jeecg.modules.airag.kb.vo.KbRagCitationVO;
import org.jeecg.modules.airag.kb.vo.KbRagContextVO;
import org.jeecg.modules.airag.kb.vo.KbSemanticSearchItemVO;
import org.jeecg.modules.airag.kb.vo.KbSemanticSearchResultVO;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import jakarta.validation.Valid;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/**
 * RAG 问答服务实现。
 */
@Slf4j
@Service
public class KbRagQaServiceImpl implements IKbRagQaService {
    /**
     * 多知识库检索服务。
     */
    private final IKbFederatedSearchService kbFederatedSearchService;

    /**
     * 大模型处理器。
     */
    private final IAIChatHandler aiChatHandler;

    /**
     * Agent模型解析器。
     */
    private final AgentModelResolver agentModelResolver;

    /**
     * RAG日志服务。
     */
    private final IKbRagChatLogService kbRagChatLogService;

    /**
     * 构造函数。
     *
     * @param kbFederatedSearchService 多知识库检索服务
     * @param aiChatHandler 大模型处理器
     * @param agentModelResolver Agent模型解析器
     * @param kbRagChatLogService RAG日志服务
     */
    public KbRagQaServiceImpl(IKbFederatedSearchService kbFederatedSearchService,
                              IAIChatHandler aiChatHandler,
                              AgentModelResolver agentModelResolver,
                              IKbRagChatLogService kbRagChatLogService) {
        this.kbFederatedSearchService = kbFederatedSearchService;
        this.aiChatHandler = aiChatHandler;
        this.agentModelResolver = agentModelResolver;
        this.kbRagChatLogService = kbRagChatLogService;
    }

    @Override
    public KbRagAnswerVO ask(KbRagQuestionDTO dto) {
        return ask(null, dto);
    }

    @Override
    public KbRagAnswerVO ask(AgentContext context, KbRagQuestionDTO dto) {
        try {
            RagRuntime runtime = buildRuntime(context, dto);
            String answer = runtime.usedContext.isEmpty()
                    ? buildFallbackAnswer(runtime.answerMode)
                    : generateAnswer(runtime, false).answer;
            KbRagAnswerVO vo = buildAnswerVo(runtime, answer, false, null);
            saveLogSafely(runtime, vo, KbConstants.LOG_STATUS_SUCCESS, null);
            return vo;
        } catch (Exception ex) {
            RagRuntime runtime = buildRuntimeSkeleton(context, dto);
            KbRagAnswerVO vo = buildErrorVo(runtime, ex);
            saveLogSafely(runtime, vo, KbConstants.LOG_STATUS_FAILED, ex.getMessage());
            if (ex instanceof JeecgBootException) {
                throw ex;
            }
            throw new JeecgBootException(ex.getMessage());
        }
    }

    @Override
    public SseEmitter askStream(KbRagQuestionDTO dto) {
        return askStream(null, dto);
    }

    @Override
    public SseEmitter askStream(AgentContext context, KbRagQuestionDTO dto) {
        SseEmitter emitter = new SseEmitter(-0L);
        try {
            RagRuntime runtime = buildRuntime(context, dto);
            if (runtime.usedContext.isEmpty()) {
                KbRagAnswerVO vo = buildAnswerVo(runtime, buildFallbackAnswer(runtime.answerMode), true, null);
                saveLogSafely(runtime, vo, KbConstants.LOG_STATUS_SUCCESS, null);
                sendSse(emitter, "rag.complete", buildCompletePayload(vo));
                emitter.complete();
                return emitter;
            }
            sendSse(emitter, "rag.start", runtime.toStartPayload());
            ChatExecution execution = prepareChatExecution(runtime);
            AtomicReference<StringBuilder> deltaBuffer = new AtomicReference<>(new StringBuilder());
            AtomicReference<String> answerRef = new AtomicReference<>("");
            AtomicReference<Throwable> errorRef = new AtomicReference<>();
            AtomicBoolean terminal = new AtomicBoolean(false);
            TokenStream tokenStream = execution.tokenStream;
            tokenStream.onPartialResponse(delta -> {
                if (terminal.get()) {
                    return;
                }
                String safeDelta = delta == null ? "" : delta;
                deltaBuffer.get().append(safeDelta);
                sendSse(emitter, "rag.delta", Map.of("delta", safeDelta));
            }).onCompleteResponse(response -> {
                if (!terminal.compareAndSet(false, true)) {
                    return;
                }
                try {
                    String finalAnswer = null;
                    try {
                        if (response != null && response.aiMessage() != null && StringUtils.hasText(response.aiMessage().text())) {
                            finalAnswer = response.aiMessage().text();
                        }
                    } catch (Exception ignore) {
                        // ignore
                    }
                    if (!StringUtils.hasText(finalAnswer)) {
                        finalAnswer = deltaBuffer.get().toString();
                    }
                    if (!StringUtils.hasText(finalAnswer)) {
                        finalAnswer = buildFallbackAnswer(runtime.answerMode);
                    }
                    answerRef.set(finalAnswer);
                    KbRagAnswerVO vo = buildAnswerVo(runtime, finalAnswer, true, execution.llmModel);
                    sendSse(emitter, "rag.complete", buildCompletePayload(vo));
                    saveLogSafely(runtime, vo, KbConstants.LOG_STATUS_SUCCESS, null);
                } catch (Exception ex) {
                    errorRef.set(ex);
                    KbRagAnswerVO vo = buildErrorVo(runtime, ex);
                    saveLogSafely(runtime, vo, KbConstants.LOG_STATUS_FAILED, ex.getMessage());
                    sendSse(emitter, "rag.error", Map.of("message", ex.getMessage()));
                } finally {
                    emitter.complete();
                }
            }).onError(error -> {
                if (!terminal.compareAndSet(false, true)) {
                    return;
                }
                errorRef.set(error);
                KbRagAnswerVO vo = buildErrorVo(runtime, error);
                saveLogSafely(runtime, vo, KbConstants.LOG_STATUS_FAILED, error.getMessage());
                sendSse(emitter, "rag.error", Map.of("message", error.getMessage()));
                emitter.complete();
            }).start();
            return emitter;
        } catch (Exception ex) {
            RagRuntime runtime = buildRuntimeSkeleton(context, dto);
            KbRagAnswerVO vo = buildErrorVo(runtime, ex);
            saveLogSafely(runtime, vo, KbConstants.LOG_STATUS_FAILED, ex.getMessage());
            try {
                sendSse(emitter, "rag.error", Map.of("message", ex.getMessage()));
            } catch (Exception ignore) {
                // ignore
            }
            emitter.completeWithError(ex);
            return emitter;
        }
    }

    @Override
    public IPage<KbRagChatLogVo> pageLogs(KbRagChatLogQueryDTO dto) {
        long pageNo = dto == null || dto.getPageNo() == null || dto.getPageNo() < 1 ? 1L : dto.getPageNo();
        long pageSize = dto == null || dto.getPageSize() == null || dto.getPageSize() < 1 ? 10L : dto.getPageSize();
        Page<KbRagChatLog> page = new Page<>(pageNo, pageSize);
        LambdaQueryWrapper<KbRagChatLog> wrapper = new LambdaQueryWrapper<>();
        if (dto != null) {
            if (oConvertUtils.isNotEmpty(dto.getKbId())) {
                wrapper.and(q -> q.like(KbRagChatLog::getKbIdsJson, dto.getKbId())
                        .or()
                        .like(KbRagChatLog::getExternalKbIdsJson, dto.getKbId()));
            }
            if (oConvertUtils.isNotEmpty(dto.getQuery())) {
                wrapper.like(KbRagChatLog::getQuery, dto.getQuery());
            }
            if (oConvertUtils.isNotEmpty(dto.getStatus())) {
                wrapper.eq(KbRagChatLog::getStatus, dto.getStatus());
            }
            if (dto.getStartTime() != null) {
                wrapper.ge(KbRagChatLog::getCreatedAt, dto.getStartTime());
            }
            if (dto.getEndTime() != null) {
                wrapper.le(KbRagChatLog::getCreatedAt, dto.getEndTime());
            }
        }
        wrapper.orderByDesc(KbRagChatLog::getCreatedAt);
        return kbRagChatLogService.page(page, wrapper).convert(KbRagChatLogVo::from);
    }

    @Override
    public KbRagChatLogVo getLogById(String id) {
        AssertUtils.assertNotEmpty("日志ID不能为空", id);
        KbRagChatLog entity = kbRagChatLogService.getById(id);
        if (entity == null) {
            throw new JeecgBootBizTipException("未找到对应日志");
        }
        return KbRagChatLogVo.from(entity);
    }

    /**
     * 构造运行上下文。
     *
     * @param context Agent上下文
     * @param dto 请求
     * @return 运行上下文
     */
    private RagRuntime buildRuntime(AgentContext context, KbRagQuestionDTO dto) {
        if (dto == null) {
            throw new JeecgBootException("请求不能为空");
        }
        if ((dto.getKbIds() == null || dto.getKbIds().isEmpty())
                && (dto.getExternalKbIds() == null || dto.getExternalKbIds().isEmpty())) {
            throw new JeecgBootException("kb_ids和external_kb_ids不能同时为空");
        }
        if (oConvertUtils.isEmpty(dto.getQuery())) {
            throw new JeecgBootException("query不能为空");
        }
        if (dto.getReferenceLimit() != null && dto.getReferenceLimit() <= 0) {
            throw new JeecgBootException("reference_limit必须大于0");
        }
        if (dto.getTopK() != null && dto.getTopK() <= 0) {
            throw new JeecgBootException("top_k必须大于0");
        }
        if (dto.getFinalTopK() != null && dto.getFinalTopK() <= 0) {
            throw new JeecgBootException("final_top_k必须大于0");
        }
        validateChatHistory(dto.getChatHistory());
        if (dto.getStream() != null && !(dto.getStream() instanceof Boolean)) {
            throw new JeecgBootException("stream必须为布尔值");
        }
        if (dto.getCiteSources() != null && !(dto.getCiteSources() instanceof Boolean)) {
            throw new JeecgBootException("cite_sources必须为布尔值");
        }

        if (context != null) {
            context.normalize();
        }

        KbFederatedSearchQueryDTO federatedDto = new KbFederatedSearchQueryDTO();
        BeanUtils.copyProperties(dto, federatedDto);
        federatedDto.setKbIds(dto.getKbIds());
        federatedDto.setExternalKbIds(dto.getExternalKbIds());

        long searchStart = System.nanoTime();
        KbSemanticSearchResultVO searchResult = kbFederatedSearchService.search(federatedDto);
        long searchDurationMs = elapsedMs(searchStart);

        List<KbRagContextVO> usedContext = new ArrayList<>();
        List<KbRagCitationVO> citations = new ArrayList<>();
        List<KbSemanticSearchItemVO> resultItems = searchResult == null || searchResult.getResults() == null
                ? Collections.emptyList()
                : searchResult.getResults();
        for (int i = 0; i < resultItems.size(); i++) {
            KbSemanticSearchItemVO item = resultItems.get(i);
            KbRagContextVO ctx = toContextVO(item, i + 1);
            usedContext.add(ctx);
            if (Boolean.TRUE.equals(dto.getCiteSources())) {
                citations.add(toCitationVO(item, i + 1));
            }
        }

        Map<String, Object> actualParams = new LinkedHashMap<>();
        if (searchResult != null && searchResult.getActualParams() != null) {
            actualParams.putAll(searchResult.getActualParams());
        }
        actualParams.put("answer_mode", normalizeAnswerMode(dto.getAnswerMode()));
        actualParams.put("cite_sources", dto.getCiteSources() == null ? Boolean.TRUE : dto.getCiteSources());
        actualParams.put("stream", dto.getStream() == null ? Boolean.FALSE : dto.getStream());
        if (context != null) {
            actualParams.put("agent_app_id", context.getAppId());
            actualParams.put("agent_session_id", context.getAgentSessionId());
        }

        Map<String, Object> debugInfo = new LinkedHashMap<>();
        if (searchResult != null && searchResult.getDebugInfo() != null) {
            debugInfo.putAll(searchResult.getDebugInfo());
        }
        debugInfo.put("rag_stage", "rag_answer");
        debugInfo.put("rag_search_duration_ms", searchDurationMs);
        debugInfo.put("rag_context_count", usedContext.size());
        debugInfo.put("rag_citation_count", citations.size());
        debugInfo.put("rag_answer_mode", normalizeAnswerMode(dto.getAnswerMode()));
        debugInfo.put("rag_cite_sources", dto.getCiteSources() == null ? Boolean.TRUE : dto.getCiteSources());
        debugInfo.put("rag_stream", dto.getStream() == null ? Boolean.FALSE : dto.getStream());
        debugInfo.put("rag_has_context", !usedContext.isEmpty());
        debugInfo.put("rag_no_context_fallback", usedContext.isEmpty());
        debugInfo.put("rag_context_preview", buildContextPreview(usedContext));

        RagRuntime runtime = new RagRuntime();
        runtime.context = context;
        runtime.request = dto;
        runtime.searchResult = searchResult == null ? new KbSemanticSearchResultVO() : searchResult;
        runtime.answerMode = normalizeAnswerMode(dto.getAnswerMode());
        runtime.citeSources = dto.getCiteSources() == null ? Boolean.TRUE : dto.getCiteSources();
        runtime.stream = dto.getStream() == null ? Boolean.FALSE : dto.getStream();
        runtime.usedContext = usedContext;
        runtime.citations = citations;
        runtime.actualParams = actualParams;
        runtime.debugInfo = debugInfo;
        runtime.llmModel = resolveLlmModel(context);
        runtime.originalQuery = dto.getQuery();
        runtime.usedQueries = runtime.searchResult.getUsedQueries() == null ? Collections.singletonList(dto.getQuery()) : runtime.searchResult.getUsedQueries();
        runtime.optimizedQueries = runtime.searchResult.getOptimizedQueries() == null ? Collections.emptyList() : runtime.searchResult.getOptimizedQueries();
        runtime.kbIds = dto.getKbIds() == null ? Collections.emptyList() : dto.getKbIds();
        runtime.externalKbIds = dto.getExternalKbIds() == null ? Collections.emptyList() : dto.getExternalKbIds();
        runtime.resultCount = usedContext.size();
        runtime.usedReferenceLength = runtime.searchResult.getUsedReferenceLength() == null ? 0 : runtime.searchResult.getUsedReferenceLength();
        runtime.searchMode = runtime.searchResult.getSearchMode();
        runtime.referenceLimit = runtime.searchResult.getReferenceLimit();
        runtime.topK = runtime.searchResult.getTopK();
        runtime.finalTopK = runtime.searchResult.getFinalTopK();
        runtime.useQueryOptimization = runtime.searchResult.getUseQueryOptimization();
        runtime.useRerank = runtime.searchResult.getUseRerank();
        runtime.llmDurationMs = 0L;
        return runtime;
    }

    /**
     * 校验历史消息。
     *
     * @param chatHistory 历史消息
     */
    private void validateChatHistory(List<KbQueryOptimizationHistoryDTO> chatHistory) {
        if (chatHistory == null || chatHistory.isEmpty()) {
            return;
        }
        for (KbQueryOptimizationHistoryDTO history : chatHistory) {
            if (history == null) {
                throw new JeecgBootException("chat_history格式非法");
            }
            if (oConvertUtils.isEmpty(history.getRole()) || oConvertUtils.isEmpty(history.getContent())) {
                throw new JeecgBootException("chat_history格式非法");
            }
            String role = history.getRole().trim().toLowerCase(Locale.ROOT);
            if (!"user".equals(role) && !"assistant".equals(role) && !"system".equals(role)) {
                throw new JeecgBootException("chat_history格式非法");
            }
        }
    }

    /**
     * 构造仅用于兜底日志的最小运行上下文。
     *
     * @param context Agent上下文
     * @param dto 请求
     * @return 运行上下文
     */
    private RagRuntime buildRuntimeSkeleton(AgentContext context, KbRagQuestionDTO dto) {
        RagRuntime runtime = new RagRuntime();
        runtime.context = context;
        runtime.request = dto;
        runtime.originalQuery = dto == null ? null : dto.getQuery();
        runtime.answerMode = safeAnswerMode(dto == null ? null : dto.getAnswerMode());
        runtime.citeSources = dto == null || dto.getCiteSources() == null ? Boolean.TRUE : dto.getCiteSources();
        runtime.stream = dto != null && Boolean.TRUE.equals(dto.getStream());
        runtime.kbIds = dto == null || dto.getKbIds() == null ? Collections.emptyList() : dto.getKbIds();
        runtime.externalKbIds = dto == null || dto.getExternalKbIds() == null ? Collections.emptyList() : dto.getExternalKbIds();
        runtime.usedQueries = dto == null ? Collections.emptyList() : Collections.singletonList(dto.getQuery());
        runtime.optimizedQueries = Collections.emptyList();
        runtime.usedContext = Collections.emptyList();
        runtime.citations = Collections.emptyList();
        runtime.actualParams = new LinkedHashMap<>();
        runtime.debugInfo = new LinkedHashMap<>();
        try {
            runtime.llmModel = resolveLlmModel(context);
        } catch (Exception ignore) {
            runtime.llmModel = "default";
        }
        runtime.resultCount = 0;
        runtime.usedReferenceLength = 0;
        runtime.searchMode = dto == null ? null : dto.getSearchMode();
        runtime.referenceLimit = dto == null ? null : dto.getReferenceLimit();
        runtime.topK = dto == null ? null : dto.getTopK();
        runtime.finalTopK = dto == null ? null : dto.getFinalTopK();
        runtime.useQueryOptimization = dto != null && Boolean.TRUE.equals(dto.getUseQueryOptimization());
        runtime.useRerank = dto != null && Boolean.TRUE.equals(dto.getUseRerank());
        return runtime;
    }

    /**
     * 安全解析回答模式，失败时回退到balanced。
     *
     * @param answerMode 回答模式
     * @return 安全模式
     */
    private String safeAnswerMode(String answerMode) {
        try {
            return normalizeAnswerMode(answerMode);
        } catch (Exception ignore) {
            return "balanced";
        }
    }

    /**
     * 生成同步答案。
     *
     * @param runtime 运行上下文
     * @param stream 是否流式
     * @return 结果
     */
    private AnswerGenerationResult generateAnswer(RagRuntime runtime, boolean stream) {
        if (runtime.usedContext.isEmpty()) {
            return new AnswerGenerationResult(buildFallbackAnswer(runtime.answerMode), runtime.llmModel);
        }
        List<ChatMessage> messages = buildMessages(runtime);
        long start = System.nanoTime();
        String answer;
        if (oConvertUtils.isNotEmpty(runtime.llmModel) && !"default".equals(runtime.llmModel)) {
            answer = aiChatHandler.completions(runtime.llmModel, messages, null);
        } else {
            answer = aiChatHandler.completionsByDefaultModel(messages, null);
        }
        runtime.llmDurationMs = elapsedMs(start);
        if (oConvertUtils.isEmpty(answer)) {
            throw new JeecgBootException("LLM返回为空");
        }
        return new AnswerGenerationResult(answer, runtime.llmModel);
    }

    /**
     * 构造流式执行对象。
     *
     * @param runtime 运行上下文
     * @return 流式执行对象
     */
    private ChatExecution prepareChatExecution(RagRuntime runtime) {
        List<ChatMessage> messages = buildMessages(runtime);
        TokenStream tokenStream;
        if (oConvertUtils.isNotEmpty(runtime.llmModel) && !"default".equals(runtime.llmModel)) {
            tokenStream = aiChatHandler.chat(runtime.llmModel, messages, null);
        } else {
            tokenStream = aiChatHandler.chatByDefaultModel(messages, null);
        }
        return new ChatExecution(tokenStream, runtime.llmModel);
    }

    /**
     * 构造提示消息。
     *
     * @param runtime 运行上下文
     * @return 消息
     */
    private List<ChatMessage> buildMessages(RagRuntime runtime) {
        List<ChatMessage> messages = new ArrayList<>();
        messages.add(new SystemMessage(buildSystemPrompt(runtime)));
        if (runtime.request.getChatHistory() != null) {
            for (KbQueryOptimizationHistoryDTO history : runtime.request.getChatHistory()) {
                if (history == null || oConvertUtils.isEmpty(history.getRole()) || oConvertUtils.isEmpty(history.getContent())) {
                    continue;
                }
                switch (history.getRole()) {
                    case "system" -> messages.add(new SystemMessage(history.getContent()));
                    case "assistant" -> messages.add(new AiMessage(history.getContent()));
                    default -> messages.add(new UserMessage(history.getContent()));
                }
            }
        }
        messages.add(new UserMessage(buildUserPrompt(runtime)));
        return messages;
    }

    /**
     * 构造系统提示词。
     *
     * @param runtime 运行上下文
     * @return 提示词
     */
    private String buildSystemPrompt(RagRuntime runtime) {
        StringBuilder builder = new StringBuilder();
        builder.append("你是一个严格、可靠的知识库问答助手。");
        builder.append("请优先依据提供的RAG上下文回答问题，不要编造知识库外的事实。");
        builder.append("回答模式=").append(runtime.answerMode).append("。");
        if ("strict".equals(runtime.answerMode)) {
            builder.append("strict模式下只能基于上下文回答；如果信息不足，请直接回复：知识库中没有足够信息回答该问题。");
        } else if ("balanced".equals(runtime.answerMode)) {
            builder.append("balanced模式下可以少量补充通用解释，但必须清楚区分知识库内容和模型补充。");
        } else {
            builder.append("creative模式下可以在知识库内容基础上进行适度扩展创作，但不得与上下文冲突。");
        }
        if (runtime.citeSources) {
            builder.append("如果回答引用了上下文，请使用[1]、[2]这样的引用编号，且只允许使用给定编号。");
        } else {
            builder.append("不要在答案中显式输出引用编号。");
        }
        builder.append("若上下文为空或不足，请明确说明，不要伪造答案。");
        return builder.toString();
    }

    /**
     * 构造用户提示词。
     *
     * @param runtime 运行上下文
     * @return 提示词
     */
    private String buildUserPrompt(RagRuntime runtime) {
        StringBuilder builder = new StringBuilder();
        builder.append("用户问题：").append(runtime.originalQuery).append("\n\n");
        builder.append("RAG上下文：\n");
        if (runtime.usedContext.isEmpty()) {
            builder.append("(空)\n");
        } else {
            for (KbRagContextVO contextVO : runtime.usedContext) {
                builder.append("[").append(contextVO.getSourceId()).append("] ");
                if (oConvertUtils.isNotEmpty(contextVO.getKbName())) {
                    builder.append("kb=").append(contextVO.getKbName()).append(" ");
                }
                if (oConvertUtils.isNotEmpty(contextVO.getDocumentName())) {
                    builder.append("doc=").append(contextVO.getDocumentName()).append(" ");
                }
                if (oConvertUtils.isNotEmpty(contextVO.getTitle())) {
                    builder.append("title=").append(contextVO.getTitle()).append(" ");
                }
                if (oConvertUtils.isNotEmpty(contextVO.getSourceUrl())) {
                    builder.append("url=").append(contextVO.getSourceUrl()).append(" ");
                }
                builder.append("score=").append(formatScore(contextVO.getFinalScore() == null ? contextVO.getScore() : contextVO.getFinalScore()));
                if (contextVO.getRerankScore() != null) {
                    builder.append(" rerank=").append(formatScore(contextVO.getRerankScore()));
                }
                builder.append("\n");
                builder.append(contextVO.getContent()).append("\n\n");
            }
        }
        if (runtime.citeSources) {
            builder.append("请在答案末尾保留引用编号，或者在句子后标记对应引用。");
        }
        return builder.toString();
    }

    /**
     * 生成上下文预览。
     *
     * @param contextVOs 上下文
     * @return 预览
     */
    private List<Map<String, Object>> buildContextPreview(List<KbRagContextVO> contextVOs) {
        if (contextVOs == null || contextVOs.isEmpty()) {
            return Collections.emptyList();
        }
        List<Map<String, Object>> preview = new ArrayList<>(contextVOs.size());
        for (KbRagContextVO contextVO : contextVOs) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("source_id", contextVO.getSourceId());
            item.put("kb_name", contextVO.getKbName());
            item.put("document_name", contextVO.getDocumentName());
            item.put("content_preview", buildContentPreview(contextVO.getContent()));
            preview.add(item);
        }
        return preview;
    }

    /**
     * 构造引用对象。
     *
     * @param item 检索项
     * @param index 序号
     * @return 引用对象
     */
    private KbRagCitationVO toCitationVO(KbSemanticSearchItemVO item, int index) {
        KbRagCitationVO vo = new KbRagCitationVO();
        vo.setCitationId("citation-" + index);
        vo.setKbId(item == null ? null : item.getKbId());
        vo.setKbName(item == null ? null : item.getKbName());
        vo.setExternalKbId(item == null ? null : item.getExternalKbId());
        vo.setExternalKbName(item == null ? null : item.getExternalKbName());
        vo.setDocumentId(item == null ? null : item.getDocumentId());
        vo.setDocumentName(item == null ? null : item.getDocumentName());
        vo.setChunkId(item == null ? null : item.getChunkId());
        vo.setChunkIndexId(item == null ? null : item.getChunkIndexId());
        vo.setExternalResultId(item == null ? null : item.getExternalResultId());
        vo.setSourceUrl(item == null ? null : item.getSourceUrl());
        vo.setContentPreview(buildContentPreview(item == null ? null : item.getContent()));
        vo.setScore(item == null ? null : item.getScore());
        vo.setRerankScore(item == null ? null : item.getRerankScore());
        return vo;
    }

    /**
     * 构造上下文对象。
     *
     * @param item 检索项
     * @param index 序号
     * @return 上下文对象
     */
    private KbRagContextVO toContextVO(KbSemanticSearchItemVO item, int index) {
        KbRagContextVO vo = new KbRagContextVO();
        vo.setSourceScope(item == null ? null : item.getSourceScope());
        vo.setSourceId(resolveSourceId(item, index));
        vo.setKbId(item == null ? null : item.getKbId());
        vo.setKbName(item == null ? null : item.getKbName());
        vo.setExternalKbId(item == null ? null : item.getExternalKbId());
        vo.setExternalKbName(item == null ? null : item.getExternalKbName());
        vo.setDocumentId(item == null ? null : item.getDocumentId());
        vo.setDocumentName(item == null ? null : item.getDocumentName());
        vo.setChunkId(item == null ? null : item.getChunkId());
        vo.setChunkIndexId(item == null ? null : item.getChunkIndexId());
        vo.setExternalResultId(item == null ? null : item.getExternalResultId());
        vo.setContent(item == null ? null : item.getContent());
        vo.setMatchedIndexText(item == null ? null : item.getMatchedIndexText());
        vo.setMatchedIndexType(item == null ? null : item.getMatchedIndexType());
        vo.setMatchedQuery(item == null ? null : item.getMatchedQuery());
        vo.setMatchedField(item == null ? null : item.getMatchedField());
        vo.setMatchedText(item == null ? null : item.getMatchedText());
        vo.setSemanticScore(item == null ? null : item.getSemanticScore());
        vo.setKeywordScore(item == null ? null : item.getKeywordScore());
        vo.setFinalScore(item == null ? null : item.getFinalScore());
        vo.setRerankScore(item == null ? null : item.getRerankScore());
        vo.setMergedScore(item == null ? null : item.getMergedScore());
        vo.setHitType(item == null ? null : item.getHitType());
        vo.setReferenceLength(item == null ? null : item.getReferenceLength());
        vo.setScore(item == null ? null : item.getScore());
        vo.setSourceType(item == null ? null : item.getSourceType());
        vo.setTitle(item == null ? null : item.getTitle());
        vo.setSourceUrl(item == null ? null : item.getSourceUrl());
        vo.setFileType(item == null ? null : item.getFileType());
        vo.setSortNo(item == null ? null : item.getSortNo());
        vo.setMetadataJson(item == null ? null : item.getMetadataJson());
        return vo;
    }

    /**
     * 生成答案返回对象。
     *
     * @param runtime 运行上下文
     * @param answer 答案
     * @param stream 是否流式
     * @param llmModel LLM模型
     * @return 返回对象
     */
    private KbRagAnswerVO buildAnswerVo(RagRuntime runtime, String answer, boolean stream, String llmModel) {
        KbRagAnswerVO vo = new KbRagAnswerVO();
        vo.setQuery(runtime.originalQuery);
        vo.setOriginalQuery(runtime.originalQuery);
        vo.setOptimizedQueries(runtime.optimizedQueries);
        vo.setUsedQueries(runtime.usedQueries);
        vo.setKbIds(runtime.kbIds);
        vo.setExternalKbIds(runtime.externalKbIds);
        vo.setAnswer(answer);
        vo.setAnswerMode(runtime.answerMode);
        vo.setActualParams(runtime.actualParams);
        vo.setUsedContext(runtime.usedContext);
        vo.setCitations(Boolean.TRUE.equals(runtime.citeSources) ? runtime.citations : Collections.emptyList());
        vo.setResultCount(runtime.resultCount);
        vo.setUsedReferenceLength(runtime.usedReferenceLength);
        vo.setDebugInfo(runtime.debugInfo);
        vo.setStatus(KbConstants.LOG_STATUS_SUCCESS);
        vo.setErrorMessage(null);
        vo.setLlmModel(llmModel == null ? runtime.llmModel : llmModel);
        vo.setTokenUsage(new LinkedHashMap<>());
        vo.setStream(stream);
        vo.setCiteSources(runtime.citeSources);
        vo.setUseQueryOptimization(runtime.useQueryOptimization);
        vo.setUseRerank(runtime.useRerank);
        vo.setSearchMode(runtime.searchMode);
        vo.setReferenceLimit(runtime.referenceLimit);
        vo.setData(Map.of(
                "answer", answer,
                "citations", vo.getCitations(),
                "used_context", vo.getUsedContext()));
        return vo;
    }

    /**
     * 生成错误返回对象。
     *
     * @param runtime 运行上下文
     * @param ex 异常
     * @return 返回对象
     */
    private KbRagAnswerVO buildErrorVo(RagRuntime runtime, Throwable ex) {
        KbRagAnswerVO vo = buildAnswerVo(runtime, buildFallbackAnswer(runtime.answerMode), false, runtime.llmModel);
        vo.setStatus(KbConstants.LOG_STATUS_FAILED);
        vo.setErrorMessage(ex == null ? null : ex.getMessage());
        return vo;
    }

    /**
     * 保存日志。
     *
     * @param runtime 运行上下文
     * @param result 结果
     * @param status 状态
     * @param errorMessage 错误信息
     */
    private void saveLogSafely(RagRuntime runtime, KbRagAnswerVO result, String status, String errorMessage) {
        try {
            KbRagChatLog log = new KbRagChatLog();
            Date now = Date.from(Instant.now());
            log.setQuery(runtime.originalQuery);
            log.setAnswer(result == null ? null : result.getAnswer());
            log.setKbIdsJson(JSON.toJSONString(runtime.kbIds));
            log.setExternalKbIdsJson(JSON.toJSONString(runtime.externalKbIds));
            log.setAnswerMode(runtime.answerMode);
            log.setActualParamsJson(JSON.toJSONString(runtime.actualParams));
            log.setUsedQueriesJson(JSON.toJSONString(runtime.usedQueries));
            log.setUsedContextJson(JSON.toJSONString(runtime.usedContext));
            log.setCitationsJson(JSON.toJSONString(result == null ? Collections.emptyList() : result.getCitations()));
            log.setResultCount(result == null ? 0 : result.getResultCount());
            log.setUsedReferenceLength(result == null ? 0 : result.getUsedReferenceLength());
            log.setDebugJson(JSON.toJSONString(runtime.debugInfo));
            log.setStatus(status);
            log.setErrorMessage(errorMessage);
            log.setCreatedAt(now);
            log.setUpdatedAt(now);
            kbRagChatLogService.save(log);
        } catch (Exception ignore) {
            // 日志失败不影响主流程
        }
    }

    /**
     * 获取模型ID。
     *
     * @param context Agent上下文
     * @return 模型ID
     */
    private String resolveLlmModel(AgentContext context) {
        if (context == null || oConvertUtils.isEmpty(context.getAppId())) {
            return "default";
        }
        try {
            return agentModelResolver.resolveTextModelId(context.getAppId());
        } catch (Exception ex) {
            throw ex instanceof JeecgBootException ? (JeecgBootException) ex : new JeecgBootException(ex.getMessage());
        }
    }

    /**
     * 生成回答模式。
     *
     * @param answerMode 回答模式
     * @return 归一化结果
     */
    private String normalizeAnswerMode(String answerMode) {
        if (oConvertUtils.isEmpty(answerMode)) {
            return "balanced";
        }
        String value = answerMode.trim().toLowerCase(Locale.ROOT);
        if ("strict".equals(value) || "balanced".equals(value) || "creative".equals(value)) {
            return value;
        }
        throw new JeecgBootException("answer_mode只能是strict、balanced、creative");
    }

    /**
     * 构造无上下文兜底回答。
     *
     * @param answerMode 模式
     * @return 结果
     */
    private String buildFallbackAnswer(String answerMode) {
        String mode = normalizeAnswerMode(answerMode);
        if ("strict".equals(mode)) {
            return "知识库中没有足够信息回答该问题。";
        }
        if ("creative".equals(mode)) {
            return "当前知识库中没有足够信息，我可以继续帮你扩展思路，但不能把它当作知识库事实。";
        }
        return "当前知识库中没有检索到足够信息，建议你补充问题或换个问法。";
    }

    /**
     * 发送SSE。
     *
     * @param emitter SSE
     * @param eventName 事件名
     * @param data 数据
     */
    private void sendSse(SseEmitter emitter, String eventName, Object data) {
        if (emitter == null) {
            return;
        }
        synchronized (emitter) {
            try {
                if (oConvertUtils.isEmpty(eventName)) {
                    emitter.send(SseEmitter.event().data(data));
                } else {
                    emitter.send(SseEmitter.event().name(eventName).data(data));
                }
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        }
    }

    /**
     * 生成结果完整返回。
     *
     * @param vo 返回对象
     * @return payload
     */
    private Map<String, Object> buildCompletePayload(KbRagAnswerVO vo) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("answer", vo.getAnswer());
        payload.put("citations", vo.getCitations());
        payload.put("used_context", vo.getUsedContext());
        payload.put("debug_info", vo.getDebugInfo());
        payload.put("result_count", vo.getResultCount());
        payload.put("used_reference_length", vo.getUsedReferenceLength());
        payload.put("status", vo.getStatus());
        payload.put("answer_mode", vo.getAnswerMode());
        return payload;
    }

    /**
     * 生成内容预览。
     *
     * @param content 内容
     * @return 预览
     */
    private String buildContentPreview(String content) {
        if (oConvertUtils.isEmpty(content)) {
            return "";
        }
        if (content.length() <= 200) {
            return content;
        }
        return content.substring(0, 200);
    }

    /**
     * 格式化分数。
     *
     * @param value 数值
     * @return 文本
     */
    private String formatScore(BigDecimal value) {
        if (value == null) {
            return null;
        }
        return value.setScale(6, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
    }

    /**
     * 生成来源ID。
     *
     * @param item 结果
     * @param index 序号
     * @return 来源ID
     */
    private String resolveSourceId(KbSemanticSearchItemVO item, int index) {
        if (item == null) {
            return String.valueOf(index);
        }
        if (oConvertUtils.isNotEmpty(item.getSourceScope()) && KbConstants.SOURCE_SCOPE_EXTERNAL.equals(item.getSourceScope())) {
            if (oConvertUtils.isNotEmpty(item.getExternalResultId())) {
                return item.getExternalResultId();
            }
        }
        if (oConvertUtils.isNotEmpty(item.getChunkIndexId())) {
            return item.getChunkIndexId();
        }
        return String.valueOf(index);
    }

    /**
     * 计算耗时。
     *
     * @param startNano 开始时间
     * @return 毫秒
     */
    private long elapsedMs(long startNano) {
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNano);
    }

    /**
     * 运行上下文。
     */
    private static class RagRuntime {
        private AgentContext context;
        private KbRagQuestionDTO request;
        private KbSemanticSearchResultVO searchResult;
        private String answerMode;
        private Boolean citeSources;
        private Boolean stream;
        private List<KbRagContextVO> usedContext = new ArrayList<>();
        private List<KbRagCitationVO> citations = new ArrayList<>();
        private Map<String, Object> actualParams = new LinkedHashMap<>();
        private Map<String, Object> debugInfo = new LinkedHashMap<>();
        private String llmModel;
        private String originalQuery;
        private List<String> usedQueries = new ArrayList<>();
        private List<String> optimizedQueries = new ArrayList<>();
        private List<String> kbIds = new ArrayList<>();
        private List<String> externalKbIds = new ArrayList<>();
        private Integer resultCount = 0;
        private Integer usedReferenceLength = 0;
        private String searchMode;
        private Integer referenceLimit;
        private Integer topK;
        private Integer finalTopK;
        private Boolean useQueryOptimization;
        private Boolean useRerank;
        private Long llmDurationMs = 0L;

        private Map<String, Object> toLogMap() {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("answer_mode", answerMode);
            map.put("search_mode", searchMode);
            map.put("top_k", topK);
            map.put("final_top_k", finalTopK);
            map.put("reference_limit", referenceLimit);
            map.put("use_query_optimization", useQueryOptimization);
            map.put("use_rerank", useRerank);
            map.put("used_reference_length", usedReferenceLength);
            map.put("result_count", resultCount);
            map.put("llm_model", llmModel);
            map.put("llm_duration_ms", llmDurationMs);
            return map;
        }

        private Map<String, Object> toStartPayload() {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("query", originalQuery);
            map.put("answer_mode", answerMode);
            map.put("used_queries", usedQueries);
            map.put("used_context", usedContext);
            map.put("debug_info", debugInfo);
            return map;
        }
    }

    /**
     * 答案生成结果。
     */
    private static class AnswerGenerationResult {
        private final String answer;
        private final String llmModel;

        private AnswerGenerationResult(String answer, String llmModel) {
            this.answer = answer;
            this.llmModel = llmModel;
        }
    }

    /**
     * 流式执行结果。
     */
    private static class ChatExecution {
        private final TokenStream tokenStream;
        private final String llmModel;

        private ChatExecution(TokenStream tokenStream, String llmModel) {
            this.tokenStream = tokenStream;
            this.llmModel = llmModel;
        }
    }
}
