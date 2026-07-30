package org.jeecg.modules.system.service.impl;

import com.alibaba.fastjson.JSONObject;
import jakarta.annotation.Resource;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.common.system.vo.LoginUser;
import org.jeecg.common.util.UUIDGenerator;
import org.jeecg.common.util.ShiroThreadPoolExecutor;
import org.jeecg.common.util.oConvertUtils;
import org.jeecg.modules.airag.agent.error.AgentErrorCode;
import org.jeecg.modules.airag.agent.error.AgentErrorException;
import org.jeecg.modules.airag.agent.error.AgentErrorSupport;
import org.jeecg.modules.airag.agent.runtime.AgentConversationMessage;
import org.jeecg.modules.airag.agent.runtime.AgentContext;
import org.jeecg.modules.airag.agent.runtime.AgentFlowStateSupport;
import org.jeecg.modules.airag.agent.runtime.AgentRegistry;
import org.jeecg.modules.airag.agent.runtime.AgentResponseLanguageSupport;
import org.jeecg.modules.airag.agent.runtime.AgentResult;
import org.jeecg.modules.airag.agent.runtime.AgentRunLoopService;
import org.jeecg.modules.airag.agent.runtime.AgentRunOutcome;
import org.jeecg.modules.airag.agent.runtime.AgentRunStep;
import org.jeecg.modules.airag.agent.sse.SseConnectionManager;
import org.jeecg.modules.airag.agent.sse.SsePayload;
import org.jeecg.modules.system.dto.tsagentchatsession.TsAgentChatReplyDto;
import org.jeecg.modules.system.entity.TsAgentChatMessage;
import org.jeecg.modules.system.entity.TsAgentChatSession;
import org.jeecg.modules.system.service.ITsAgentChatMessageService;
import org.jeecg.modules.system.service.ITsAgentChatReplyService;
import org.jeecg.modules.system.service.ITsAgentChatSessionService;
import org.jeecg.modules.system.monitor.TsAiLogTraceContext;
import org.jeecg.modules.system.vo.tsagentchatsession.TsAgentChatReplyVo;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Agent 回复编排实现。
 *
 * @author codex
 * @date 2026/6/25
 */
@Service
@Slf4j
public class TsAgentChatReplyServiceImpl implements ITsAgentChatReplyService {

    private static final String ROLE_USER = "user";
    private static final String ROLE_ASSISTANT = "assistant";
    private static final String DEFAULT_AGENT_CODE = "main";
    private static final String SENDER_MAIN_AGENT = "main_agent";
    private static final String SENDER_SUB_AGENT = "sub_agent";

    /**
     * SSE 流式回复执行线程池。
     */
    private static final ExecutorService STREAM_EXECUTOR =
            new ShiroThreadPoolExecutor(8, 8, 60L, TimeUnit.SECONDS, new LinkedBlockingQueue<>());

    @Resource
    private ITsAgentChatSessionService tsAgentChatSessionService;

    @Resource
    private ITsAgentChatMessageService tsAgentChatMessageService;

    @Resource
    private AgentRunLoopService agentRunLoopService;

    @Resource
    private SseConnectionManager sseConnectionManager;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<TsAgentChatReplyVo> createAiReply(LoginUser user, Long sessionId, TsAgentChatReplyDto request) {
        AgentErrorSupport.ResolvedError validationError = validateRequest(user, sessionId, request);
        if (validationError != null) {
            return Result.error(validationError.code().defaultMessage(), buildErrorReply(validationError));
        }
        ReplyRuntime runtime = prepareRuntime(user, sessionId, request);
        AgentRunOutcome runOutcome = executeAgentRun(runtime);
        AgentResult agentResult = runOutcome.getResult();
        TsAgentChatReplyVo vo = saveAssistantReply(runtime, agentResult, resolveAssistantContent(agentResult, runtime.context));
        return Result.OK(vo);
    }

    @Override
    public SseEmitter createAiReplyStream(LoginUser user, Long sessionId, TsAgentChatReplyDto request) {
        SseEmitter emitter = new SseEmitter(0L);
        String connectionKey = UUIDGenerator.generate();
        this.sseConnectionManager.register(connectionKey, emitter);
        try {
            AgentErrorSupport.ResolvedError validationError = validateRequest(user, sessionId, request);
            if (validationError != null) {
                sendStreamEnd(connectionKey, validationError);
                completeEmitter(emitter);
                return emitter;
            }
            ReplyRuntime runtime = prepareRuntime(user, sessionId, request);
            runtime.context.setSseConnectionKey(connectionKey);
            STREAM_EXECUTOR.submit(() -> runStreamReply(connectionKey, emitter, runtime));
        } catch (JeecgBootException ex) {
            log.warn("Agent流式回复预处理失败，sessionId={}", sessionId, ex);
            sendStreamEnd(connectionKey, AgentErrorSupport.resolve(ex, AgentErrorCode.CHAT_EXECUTION_FAILED));
            completeEmitter(emitter);
        } catch (Exception ex) {
            log.error("Agent流式回复初始化失败，sessionId={}", sessionId, ex);
            sendStreamEnd(connectionKey, AgentErrorSupport.resolve(ex, AgentErrorCode.CHAT_EXECUTION_FAILED));
            completeEmitter(emitter);
        }
        return emitter;
    }

    /**
     * 校验一次回复请求的基础条件。
     *
     * @param user 当前用户
     * @param sessionId 会话ID
     * @param request 请求参数
     * @return 校验失败错误，校验通过返回 null
     */
    private AgentErrorSupport.ResolvedError validateRequest(LoginUser user,
                                                            Long sessionId,
                                                            TsAgentChatReplyDto request) {
        if (user == null) {
            return resolvedError(AgentErrorCode.CHAT_AUTH_REQUIRED, null);
        }
        if (request == null) {
            return resolvedError(AgentErrorCode.CHAT_REQUEST_INVALID, null);
        }
        request.applyDefaults();
        String userInput = normalizeText(request.getUserInput());
        if (!StringUtils.hasText(userInput)) {
            return resolvedError(
                    AgentErrorCode.CHAT_USER_INPUT_EMPTY,
                    Map.of("field", "userInput")
            );
        }
        TsAgentChatSession session = tsAgentChatSessionService.getOwnedSession(user.getId(), sessionId);
        if (session == null) {
            return resolvedError(
                    AgentErrorCode.CHAT_SESSION_NOT_FOUND,
                    Map.of("sessionId", sessionId == null ? "" : sessionId)
            );
        }
        return null;
    }

    /**
     * 执行流式回复。
     *
     * @param connectionKey SSE 连接键
     * @param emitter SSE 发射器
     * @param runtime 运行时上下文
     */
    private void runStreamReply(String connectionKey, SseEmitter emitter, ReplyRuntime runtime) {
        try {
            AgentRunOutcome runOutcome = executeAgentRun(runtime);
            AgentResult agentResult = runOutcome.getResult();
            String assistantContent = resolveAssistantContent(agentResult, runtime.context);
            if (!StringUtils.hasText(assistantContent)) {
                throw new AgentErrorException(AgentErrorCode.CHAT_EMPTY_RESPONSE);
            }
            saveAssistantReply(runtime, agentResult, assistantContent);
        } catch (Exception ex) {
            log.error("Agent流式回复执行失败，sessionId={}, userMessageId={}", runtime.session.getId(), runtime.userMessage.getId(), ex);
            sendStreamEnd(connectionKey, AgentErrorSupport.resolve(ex, AgentErrorCode.CHAT_EXECUTION_FAILED));
        } finally {
            this.sseConnectionManager.finishRun(connectionKey);
        }
    }

    /**
     * 准备一次回复所需的上下文与首条用户消息。
     *
     * @param user 当前用户
     * @param sessionId 会话ID
     * @param request 请求参数
     * @return 运行时上下文
     */
    private ReplyRuntime prepareRuntime(LoginUser user, Long sessionId, TsAgentChatReplyDto request) {
        if (user == null) {
            throw new AgentErrorException(AgentErrorCode.CHAT_AUTH_REQUIRED);
        }
        if (request == null) {
            throw new AgentErrorException(AgentErrorCode.CHAT_REQUEST_INVALID);
        }
        request.applyDefaults();

        String userInput = normalizeText(request.getUserInput());
        if (!StringUtils.hasText(userInput)) {
            throw new AgentErrorException(
                    AgentErrorCode.CHAT_USER_INPUT_EMPTY,
                    Map.of("field", "userInput")
            );
        }

        TsAgentChatSession session = tsAgentChatSessionService.getOwnedSession(user.getId(), sessionId);
        if (session == null) {
            throw new AgentErrorException(
                    AgentErrorCode.CHAT_SESSION_NOT_FOUND,
                    Map.of("sessionId", sessionId == null ? "" : sessionId)
            );
        }

        TsAgentChatMessage userMessage = tsAgentChatMessageService.saveUserMessage(
                user.getId(),
                sessionId,
                userInput,
                "text",
                null,
                null,
                null
        );

        List<TsAgentChatMessage> recentMessages = tsAgentChatMessageService.listRecentMessages(
                user.getId(),
                sessionId,
                request.getHistoryCount()
        );
        Map<String, String> variables = buildPromptVariables(session, userInput, recentMessages);
        AgentContext context = buildAgentContext(user, session, userInput, userMessage, recentMessages, variables);
        context.putAttribute("optionValue", normalizeText(request.getOptionValue()));
        context.putAttribute("interactionId", normalizeText(request.getInteractionId()));
        ReplyRuntime runtime = new ReplyRuntime();
        runtime.session = session;
        runtime.userMessage = userMessage;
        runtime.recentMessages = recentMessages;
        runtime.variables = variables;
        runtime.context = context;
        runtime.userInput = userInput;
        runtime.startingAgentCode = normalizeActiveAgentCode(session.getActiveAgentCode());
        runtime.context.putAttribute("startingAgentCode", runtime.startingAgentCode);
        return runtime;
    }

    /**
     * 执行顶层 Agent Run 并持久化最后实际运行的 Agent。
     *
     * @param runtime 回复运行时
     * @return Run 结果
     */
    private AgentRunOutcome executeAgentRun(ReplyRuntime runtime) {
        AgentRunOutcome outcome = this.agentRunLoopService.run(runtime.startingAgentCode, runtime.context);
        runtime.runOutcome = outcome;
        runtime.context.putAttribute("agentRunSteps", buildRunStepSummary(outcome));
        if (outcome != null && StringUtils.hasText(outcome.getLastAgentCode())) {
            String lastAgentCode = normalizeActiveAgentCode(outcome.getLastAgentCode());
            String previousRawAgentCode = runtime.session.getActiveAgentCode();
            String previousAgentCode = normalizeActiveAgentCode(previousRawAgentCode);
            runtime.session.setActiveAgentCode(lastAgentCode);
            updateFlowResumeState(runtime.session, runtime.context, lastAgentCode, outcome.getResult());
            if (!lastAgentCode.equalsIgnoreCase(previousAgentCode)
                    || !StringUtils.hasText(previousRawAgentCode)) {
                runtime.session.setActiveAgentUpdatedAt(new Date());
            }
            runtime.session.setUpdatedAt(new Date());
            this.tsAgentChatSessionService.updateById(runtime.session);
        }
        return outcome;
    }

    /**
     * 将 Agent 结果落库为助手消息并组装返回体。
     *
     * @param runtime 运行时上下文
     * @param agentResult Agent 结果
     * @param assistantContent 助手回复文本
     * @return 回复 VO
     */
    private TsAgentChatReplyVo saveAssistantReply(ReplyRuntime runtime, AgentResult agentResult, String assistantContent) {
        if (runtime == null) {
            throw new AgentErrorException(AgentErrorCode.CHAT_EXECUTION_FAILED);
        }
        if (!StringUtils.hasText(assistantContent)) {
            throw new AgentErrorException(AgentErrorCode.CHAT_EMPTY_RESPONSE);
        }
        String promptCode = extractString(agentResult == null ? null : agentResult.getData(), "promptCode");
        String promptVersion = extractString(agentResult == null ? null : agentResult.getData(), "promptVersion");
        String messageStatus = toMessageStatus(agentResult == null ? null : agentResult.getStatus());
        String lastAgentCode = runtime.runOutcome == null
                ? runtime.startingAgentCode
                : normalizeActiveAgentCode(runtime.runOutcome.getLastAgentCode());
        String senderType = AgentRegistry.MAIN_AGENT_CODE.equalsIgnoreCase(lastAgentCode)
                ? SENDER_MAIN_AGENT
                : SENDER_SUB_AGENT;
        String sourceNodeName = resolveSourceNodeName(runtime.context, agentResult);
        String sourceEventId = SENDER_SUB_AGENT.equals(senderType)
                ? runtime.context.getLastCompletedSubAgentEventId()
                : null;

        TsAgentChatMessage assistantMessage = tsAgentChatMessageService.saveAssistantMessage(
                runtime.context.getUserId(),
                runtime.session.getId(),
                senderType,
                lastAgentCode,
                sourceNodeName,
                sourceEventId,
                assistantContent,
                "text",
                messageStatus,
                runtime.userMessage.getId(),
                runtime.context.getRunId(),
                promptCode,
                runtime.session.getAppId(),
                null,
                buildExtJson(runtime.session, runtime.userInput, runtime.context, agentResult, promptCode, promptVersion, runtime.variables)
        );

        TsAgentChatReplyVo vo = new TsAgentChatReplyVo();
        vo.setSessionId(runtime.session.getId());
        vo.setUserMessageId(runtime.userMessage.getId());
        vo.setAssistantMessageId(assistantMessage.getId());
        vo.setContentText(assistantContent);
        vo.setPromptCode(promptCode);
        vo.setPromptVersion(promptVersion);
        vo.setRenderedPrompt(null);
        vo.setCreatedAt(assistantMessage.getCreatedAt() == null ? new Date() : assistantMessage.getCreatedAt());
        return vo;
    }

    /**
     * 获取生成正式助手消息的节点名称。
     *
     * @param context 运行上下文
     * @param agentResult Agent 执行结果
     * @return 最终结果节点，失败场景回退当前节点
     */
    private String resolveSourceNodeName(AgentContext context, AgentResult agentResult) {
        if (context == null) {
            return null;
        }
        if (agentResult == null
                || agentResult.getStatus() == null
                || agentResult.getStatus() == AgentResult.Status.FAILED) {
            String currentNodeName = normalizeText(context.getCurrentNodeName());
            return StringUtils.hasText(currentNodeName)
                    ? currentNodeName
                    : normalizeText(context.getResultNodeName());
        }
        String resultNodeName = normalizeText(context.getResultNodeName());
        return StringUtils.hasText(resultNodeName)
                ? resultNodeName
                : normalizeText(context.getCurrentNodeName());
    }

    /**
     * 发送流式终止事件。
     *
     * @param connectionKey SSE 连接键
     * @param error 错误信息
     */
    private void sendStreamEnd(String connectionKey, AgentErrorSupport.ResolvedError error) {
        if (!StringUtils.hasText(connectionKey)) {
            return;
        }
        AgentErrorSupport.ResolvedError resolved = error == null
                ? resolvedError(AgentErrorCode.CHAT_EXECUTION_FAILED, null)
                : error;
        Map<String, Object> errorPayload = AgentErrorSupport.toPayload(resolved);
        SsePayload payload = new SsePayload();
        payload.setEvent("agent.end");
        payload.setContent(resolved.code().defaultMessage());
        payload.setStatus(0);
        Map<String, Object> data = new LinkedHashMap<>();
        data.putAll(errorPayload);
        data.put("error", new LinkedHashMap<>(errorPayload));
        data.put("status", "FAILED");
        payload.setData(data);
        this.sseConnectionManager.send(connectionKey, "agent.end", payload);
    }

    private AgentErrorSupport.ResolvedError resolvedError(AgentErrorCode errorCode,
                                                          Map<String, Object> errorArgs) {
        return new AgentErrorSupport.ResolvedError(errorCode, errorArgs, null);
    }

    private TsAgentChatReplyVo buildErrorReply(AgentErrorSupport.ResolvedError error) {
        AgentErrorSupport.ResolvedError resolved = error == null
                ? resolvedError(AgentErrorCode.CHAT_EXECUTION_FAILED, null)
                : error;
        TsAgentChatReplyVo vo = new TsAgentChatReplyVo();
        vo.setErrorCode(resolved.code().code());
        vo.setErrorCategory(resolved.code().category());
        vo.setRetryable(resolved.code().retryable());
        vo.setErrorArgs(new LinkedHashMap<>(resolved.args()));
        return vo;
    }

    /**
     * 安全关闭 SSE 连接。
     *
     * @param emitter SSE 发射器
     */
    private void completeEmitter(SseEmitter emitter) {
        if (emitter == null) {
            return;
        }
        try {
            emitter.complete();
        } catch (Exception ignore) {
            // ignore
        }
    }

    /**
     * 解析 Agent 的最终回复文本，兼容上下文回填。
     *
     * @param agentResult Agent 结果
     * @param context 运行上下文
     * @return 回复文本
     */
    private String resolveAssistantContent(AgentResult agentResult, AgentContext context) {
        if (isConfirmationInteraction(agentResult)) {
            String question = normalizeText(extractString(agentResult.getData(), "question"));
            String generatedContent = normalizeText(context == null ? null : context.getLatestContent());
            if (StringUtils.hasText(generatedContent) && !generatedContent.equals(question)) {
                return generatedContent;
            }
            for (String attributeName : List.of("roleDialogNodeResultContent", "storyDialogNodeResultContent")) {
                generatedContent = normalizeText(oConvertUtils.getString(
                        context == null ? null : context.getAttribute(attributeName)
                ));
                if (StringUtils.hasText(generatedContent) && !generatedContent.equals(question)) {
                    return generatedContent;
                }
            }
            generatedContent = normalizeText(extractString(agentResult.getData(), "formattedResult"));
            return StringUtils.hasText(generatedContent) && !generatedContent.equals(question)
                    ? generatedContent
                    : null;
        }
        String assistantContent = normalizeText(agentResult == null ? null : agentResult.getContent());
        if (!StringUtils.hasText(assistantContent) && agentResult != null && agentResult.getData() != null) {
            assistantContent = normalizeText(extractString(agentResult.getData(), "formattedResult"));
        }
        if (!StringUtils.hasText(assistantContent) && context != null) {
            assistantContent = normalizeText(context.getLatestContent());
        }
        return assistantContent;
    }

    private boolean isConfirmationInteraction(AgentResult agentResult) {
        if (agentResult == null
                || agentResult.getStatus() != AgentResult.Status.WAITING_USER
                || agentResult.getData() == null) {
            return false;
        }
        String interactionType = normalizeText(extractString(agentResult.getData(), "interactionType"));
        return "confirm".equalsIgnoreCase(interactionType)
                && StringUtils.hasText(extractString(agentResult.getData(), "interactionId"));
    }

    /**
     * 构造 Agent 运行上下文。
     *
     * @param user 当前用户
     * @param session 会话
     * @param userInput 用户输入
     * @param userMessage 用户消息
     * @param recentMessages 最近消息
     * @param variables 提示词变量
     * @return Agent 上下文
     */
    private AgentContext buildAgentContext(LoginUser user,
                                           TsAgentChatSession session,
                                           String userInput,
                                           TsAgentChatMessage userMessage,
                                           List<TsAgentChatMessage> recentMessages,
                                           Map<String, String> variables) {
        AgentContext context = new AgentContext();
        context.setAppId(session.getAppId());
        context.setAgentSessionId(session.getId());
        context.setSessionId(session.getId());
        context.setMessageId(userMessage == null || userMessage.getId() == null ? null : String.valueOf(userMessage.getId()));
        context.setTurnId(userMessage == null || userMessage.getId() == null ? null : String.valueOf(userMessage.getId()));
        context.setAgentCode(normalizeActiveAgentCode(session.getActiveAgentCode()));
        context.setResumeNodeName(session.getActiveNodeName());
        context.setActiveStage(session.getActiveStage());
        context.setUserId(user == null || user.getId() == null ? null : String.valueOf(user.getId()));
        context.setUserInput(userInput);
        AgentResponseLanguageSupport.apply(context, LocaleContextHolder.getLocale().toLanguageTag());
        context.setConversationMessages(buildConversationMessages(recentMessages));
        context.putAttribute("sessionMemoryJson", session.getMemoryJson());
        context.putAttribute("sessionStateJson", session.getStateJson());
        context.putAttribute("sessionTitle", session.getSessionTitle());
        context.putAttribute("sessionSummary", session.getSessionSummary());
        context.putAttribute("lastAssistantMessage", findLastAssistantMessage(recentMessages));
        context.putAttribute("promptVariables", variables);
        AgentFlowStateSupport.restore(context, context.getAgentCode(), session.getAgentFlowStateJson());
        bindTsAiLogContext(context);
        return context;
    }

    /**
     * 按本轮最终 Agent 和状态更新下一轮流程恢复位置。
     *
     * @param session 会话
     * @param context Agent 上下文
     * @param lastAgentCode 最后实际运行的 Agent
     * @param result 最终结果
     */
    private void updateFlowResumeState(TsAgentChatSession session,
                                       AgentContext context,
                                       String lastAgentCode,
                                       AgentResult result) {
        if (session == null) {
            return;
        }
        if (!AgentFlowStateSupport.supports(lastAgentCode)
                || result == null
                || (result.getStatus() != AgentResult.Status.WAITING_USER
                && result.getStatus() != AgentResult.Status.FAILED)) {
            clearFlowResumeState(session);
            return;
        }
        String resumeNodeName = extractString(result.getData(), AgentFlowStateSupport.DATA_RESUME_NODE_NAME);
        if (!StringUtils.hasText(resumeNodeName) && context != null) {
            resumeNodeName = context.getResumeNodeName();
        }
        if (!StringUtils.hasText(resumeNodeName) && context != null) {
            resumeNodeName = context.getCurrentNodeName();
        }
        String activeStage = extractString(result.getData(), AgentFlowStateSupport.DATA_ACTIVE_STAGE);
        if (!StringUtils.hasText(activeStage) && context != null) {
            activeStage = context.getActiveStage();
        }
        if (!StringUtils.hasText(activeStage)) {
            activeStage = extractString(result.getData(), "stage");
        }
        session.setActiveNodeName(normalizeText(resumeNodeName));
        session.setActiveStage(normalizeText(activeStage));
        session.setAgentFlowStateJson(AgentFlowStateSupport.snapshot(context, lastAgentCode));
    }

    /**
     * 清空会话中的子 Agent 流程恢复状态。
     *
     * @param session 会话
     */
    private void clearFlowResumeState(TsAgentChatSession session) {
        session.setActiveNodeName(null);
        session.setActiveStage(null);
        session.setAgentFlowStateJson(null);
    }

    /**
     * 将当前 ts_ai_log 上下文绑定到 AgentContext，供异步 LLM 节点落库使用。
     */
    private void bindTsAiLogContext(AgentContext context) {
        if (context == null) {
            return;
        }
        TsAiLogTraceContext.State state = TsAiLogTraceContext.get();
        if (state == null) {
            return;
        }
        context.putAttribute("tsAiLogId", state.getLogId());
        context.putAttribute("tsAiLogTraceId", state.getTraceId());
        context.putAttribute("tsAiLogStepCounter", new AtomicInteger(10));
    }

    /**
     * 构建提示词变量。
     */
    private Map<String, String> buildPromptVariables(TsAgentChatSession session,
                                                     String userInput,
                                                     List<TsAgentChatMessage> recentMessages) {
        Map<String, String> variables = new LinkedHashMap<>();
        JSONObject memory = parseMemory(session == null ? null : session.getMemoryJson());
        putIfHas(variables, "role_name", memory.getString("role_name"));
        putIfHas(variables, "gender", memory.getString("gender"));
        putIfHas(variables, "occupation", memory.getString("occupation"));
        putIfHas(variables, "background_story", memory.getString("background_story"));
        putIfHas(variables, "other_roles_block", memory.getString("other_roles_block"));
        putIfHas(variables, "title", fallback(memory.getString("title"), session == null ? null : session.getSessionTitle()));
        putIfHas(variables, "story_intro", memory.getString("story_intro"));
        putIfHas(variables, "story_setting", memory.getString("story_setting"));
        putIfHas(variables, "site_setting", memory.getString("site_setting"));
        putIfHas(variables, "plot_outline", memory.getString("plot_outline"));
        putIfHas(variables, "story_mode", memory.getString("story_mode"));
        putIfHas(variables, "user_input", userInput);
        putIfHas(variables, "state_json", session == null ? null : session.getStateJson());
        putIfHas(variables, "last_assistant_message", findLastAssistantMessage(recentMessages));
        return variables;
    }

    /**
     * 构造供 LLM 原生消息列表使用的结构化会话历史。
     *
     * <p>保留当前用户消息，后续由 LLM 节点结合 messageId 和当前输入决定是否去重，
     * 避免子 Agent handoff 后丢失用户原始消息。</p>
     */
    private List<AgentConversationMessage> buildConversationMessages(List<TsAgentChatMessage> recentMessages) {
        List<AgentConversationMessage> messages = new ArrayList<>();
        if (recentMessages == null || recentMessages.isEmpty()) {
            return messages;
        }
        for (TsAgentChatMessage message : recentMessages) {
            if (message == null
                    || !StringUtils.hasText(message.getContent())) {
                continue;
            }
            String role = normalizeText(message.getRoleType());
            if (!ROLE_USER.equalsIgnoreCase(role) && !ROLE_ASSISTANT.equalsIgnoreCase(role)) {
                continue;
            }
            messages.add(new AgentConversationMessage(
                    message.getId() == null ? null : String.valueOf(message.getId()),
                    message.getParentMessageId() == null ? null : String.valueOf(message.getParentMessageId()),
                    role.toLowerCase(Locale.ROOT),
                    message.getContent().trim(),
                    normalizeText(message.getAgentCode()),
                    normalizeText(message.getSourceNodeName())
            ));
        }
        return messages;
    }

    /**
     * 查找上一条助手消息。
     */
    private String findLastAssistantMessage(List<TsAgentChatMessage> recentMessages) {
        if (recentMessages == null || recentMessages.isEmpty()) {
            return "";
        }
        for (int i = recentMessages.size() - 1; i >= 0; i--) {
            TsAgentChatMessage message = recentMessages.get(i);
            if (message == null || !StringUtils.hasText(message.getContent())) {
                continue;
            }
            if (ROLE_ASSISTANT.equalsIgnoreCase(normalizeText(message.getRoleType()))) {
                return message.getContent().trim();
            }
        }
        return "";
    }

    /**
     * 解析会话记忆。
     */
    private JSONObject parseMemory(String memoryJson) {
        if (!StringUtils.hasText(memoryJson)) {
            return new JSONObject();
        }
        try {
            JSONObject parsed = JSONObject.parseObject(memoryJson);
            return parsed == null ? new JSONObject() : parsed;
        } catch (Exception ex) {
            return new JSONObject();
        }
    }

    /**
     * 构造扩展信息。
     */
    private String buildExtJson(TsAgentChatSession session,
                                String userInput,
                                AgentContext context,
                                AgentResult agentResult,
                                String promptCode,
                                String promptVersion,
                                Map<String, String> promptVariables) {
        JSONObject ext = new JSONObject();
        ext.put("agentName", context == null ? null : context.getAgentCode());
        ext.put("startingAgentCode", context == null ? null : context.getAttribute("startingAgentCode"));
        ext.put("lastAgentCode", context == null ? null : context.getAgentCode());
        ext.put("agentRunSteps", buildRunStepSummary(context, agentResult));
        ext.put("sessionId", session == null ? null : session.getId());
        ext.put("userInput", userInput);
        ext.put("runId", context == null ? null : context.getRunId());
        ext.put("promptCode", promptCode);
        ext.put("promptVersion", promptVersion);
        ext.put("agentResultStatus", agentResult == null || agentResult.getStatus() == null ? null : agentResult.getStatus().name());
        ext.put("toolName", agentResult == null ? null : agentResult.getData().get("toolName"));
        ext.put("description", agentResult == null ? null : agentResult.getData().get("description"));
        ext.put("structuredResult", agentResult == null ? null : agentResult.getStructuredResult());
        ext.put("subAgentEvents", context == null ? null : context.snapshotEvents());
        ext.put("promptVariables", promptVariables);
        return ext.toJSONString();
    }

    /**
     * 提取字符串值。
     *
     * @param data 数据
     * @param key 键
     * @return 字符串
     */
    private String extractString(Map<String, Object> data, String key) {
        if (data == null || key == null) {
            return null;
        }
        Object value = data.get(key);
        return value == null ? null : normalizeText(String.valueOf(value));
    }

    /**
     * 将 Agent 状态转换为消息状态。
     *
     * @param status Agent 状态
     * @return 消息状态
     */
    private String toMessageStatus(AgentResult.Status status) {
        if (status == null) {
            return "success";
        }
        return switch (status) {
            case FAILED -> "failed";
            case WAITING_USER -> "success";
            case HANDOFF -> "success";
            case SUCCESS -> "success";
        };
    }

    /**
     * 写入变量，空值不进入。
     */
    private void putIfHas(Map<String, String> variables, String key, String value) {
        if (!StringUtils.hasText(key)) {
            return;
        }
        String normalized = normalizeText(value);
        if (StringUtils.hasText(normalized)) {
            variables.put(key, normalized);
        }
    }

    /**
     * 文本归一化。
     */
    private String normalizeText(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String normalizeAgentCode(String value) {
        String normalized = normalizeText(value);
        return StringUtils.hasText(normalized) ? normalized : DEFAULT_AGENT_CODE;
    }

    /**
     * 规范化当前 active Agent 编码。
     *
     * @param value Agent 编码
     * @return active Agent 编码
     */
    private String normalizeActiveAgentCode(String value) {
        return normalizeAgentCode(value);
    }

    /**
     * 构造当前 Run 的轻量 Step 摘要。
     *
     * @param context 运行上下文
     * @param agentResult 最终结果
     * @return Step 摘要
     */
    private List<Map<String, Object>> buildRunStepSummary(AgentContext context, AgentResult agentResult) {
        List<Map<String, Object>> summaries = new ArrayList<>();
        if (context == null) {
            return summaries;
        }
        Object steps = context.getAttribute("agentRunSteps");
        if (steps instanceof List<?> stepList) {
            for (Object step : stepList) {
                if (step instanceof Map<?, ?> rawMap) {
                    Map<String, Object> summary = new LinkedHashMap<>();
                    rawMap.forEach((key, value) -> {
                        if (key != null) {
                            summary.put(String.valueOf(key), value);
                        }
                    });
                    summaries.add(summary);
                }
            }
        }
        if (summaries.isEmpty() && agentResult != null) {
            Map<String, Object> summary = new LinkedHashMap<>();
            summary.put("agentCode", context.getAgentCode());
            summary.put("status", agentResult.getStatus());
            summaries.add(summary);
        }
        return summaries;
    }

    /**
     * 将顶层 Run 转换成可持久化的 Step 摘要。
     *
     * @param runOutcome Run 结果
     * @return Step 摘要
     */
    private List<Map<String, Object>> buildRunStepSummary(AgentRunOutcome runOutcome) {
        List<Map<String, Object>> summaries = new ArrayList<>();
        if (runOutcome == null || runOutcome.getSteps() == null) {
            return summaries;
        }
        for (AgentRunStep step : runOutcome.getSteps()) {
            if (step == null) {
                continue;
            }
            Map<String, Object> summary = new LinkedHashMap<>();
            summary.put("stepIndex", step.getStepIndex());
            summary.put("agentCode", step.getAgentCode());
            summary.put("status", step.getResult() == null ? null : step.getResult().getStatus());
            summary.put("handoffTargetAgentCode", step.getResult() == null ? null : step.getResult().getHandoffTargetAgentCode());
            summaries.add(summary);
        }
        return summaries;
    }

    /**
     * 兼容空值的兜底。
     */
    private String fallback(String value, String defaultValue) {
        if (StringUtils.hasText(value)) {
            return value.trim();
        }
        return normalizeText(defaultValue);
    }

    /**
     * 回复运行时数据。
     */
    private static class ReplyRuntime {
        private TsAgentChatSession session;
        private TsAgentChatMessage userMessage;
        private List<TsAgentChatMessage> recentMessages;
        private Map<String, String> variables;
        private AgentContext context;
        private String userInput;
        private String startingAgentCode;
        private AgentRunOutcome runOutcome;
    }
}
