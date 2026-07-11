package org.jeecg.modules.system.service.impl;

import com.alibaba.fastjson.JSONObject;
import jakarta.annotation.Resource;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.common.system.vo.LoginUser;
import org.jeecg.common.util.UUIDGenerator;
import org.jeecg.common.util.ShiroThreadPoolExecutor;
import org.jeecg.common.util.oConvertUtils;
import org.jeecg.modules.airag.agent.common.SubAgentHistorySupport;
import org.jeecg.modules.airag.agent.main.TsAgentChatAgent;
import org.jeecg.modules.airag.agent.runtime.AgentContext;
import org.jeecg.modules.airag.agent.runtime.AgentResult;
import org.jeecg.modules.airag.agent.runtime.AgentRuntimeService;
import org.jeecg.modules.airag.agent.sse.SseConnectionManager;
import org.jeecg.modules.airag.agent.sse.SsePayload;
import org.jeecg.modules.system.dto.tsagentchatsession.TsAgentChatReplyDto;
import org.jeecg.modules.system.entity.TsAgentChatMessage;
import org.jeecg.modules.system.entity.TsAgentChatSession;
import org.jeecg.modules.system.service.ITsAgentChatMessageService;
import org.jeecg.modules.system.service.ITsAgentChatReplyService;
import org.jeecg.modules.system.service.ITsAgentChatSessionService;
import org.jeecg.modules.system.vo.tsagentchatsession.TsAgentChatReplyVo;
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
    private static final String ROLE_SYSTEM = "system";
    private static final String ROLE_TOOL = "tool";
    private static final String ATTR_SESSION_SUB_AGENT_HISTORY_JSON = "sessionSubAgentHistoryJson";
    private static final String ATTR_SUB_AGENT_HISTORY_JSON = "subAgentHistoryJson";
    private static final String ATTR_SUB_AGENT_HISTORY_BLOCK = "subAgentHistoryBlock";
    private static final String DEFAULT_AGENT_CODE = "main";
    private static final String SENDER_MAIN_AGENT = "main_agent";

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
    private AgentRuntimeService agentRuntimeService;

    @Resource
    private TsAgentChatAgent tsAgentChatAgent;

    @Resource
    private SseConnectionManager sseConnectionManager;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<TsAgentChatReplyVo> createAiReply(LoginUser user, Long sessionId, TsAgentChatReplyDto request) {
        String validationError = validateRequest(user, sessionId, request);
        if (validationError != null) {
            return Result.error(validationError);
        }
        ReplyRuntime runtime = prepareRuntime(user, sessionId, request);
        AgentResult agentResult = agentRuntimeService.execute(tsAgentChatAgent, runtime.context);
        recordSubAgentHistory(runtime, agentResult);
        TsAgentChatReplyVo vo = saveAssistantReply(runtime, agentResult, resolveAssistantContent(agentResult, runtime.context));
        return Result.OK(vo);
    }

    @Override
    public SseEmitter createAiReplyStream(LoginUser user, Long sessionId, TsAgentChatReplyDto request) {
        SseEmitter emitter = new SseEmitter(0L);
        String connectionKey = UUIDGenerator.generate();
        this.sseConnectionManager.register(connectionKey, emitter);
        try {
            String validationError = validateRequest(user, sessionId, request);
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
            sendStreamEnd(connectionKey, ex.getMessage());
            completeEmitter(emitter);
        } catch (Exception ex) {
            log.error("Agent流式回复初始化失败，sessionId={}", sessionId, ex);
            sendStreamEnd(connectionKey, ex.getMessage());
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
     * @return 校验失败消息，校验通过返回 null
     */
    private String validateRequest(LoginUser user, Long sessionId, TsAgentChatReplyDto request) {
        if (user == null) {
            return "未登录或登录已过期";
        }
        if (request == null) {
            return "请求参数不能为空";
        }
        request.applyDefaults();
        String userInput = normalizeText(request.getUserInput());
        if (!StringUtils.hasText(userInput)) {
            return "userInput不能为空";
        }
        TsAgentChatSession session = tsAgentChatSessionService.getOwnedSession(user.getId(), sessionId);
        if (session == null) {
            return "会话不存在或无权限访问";
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
            AgentResult agentResult = agentRuntimeService.execute(tsAgentChatAgent, runtime.context);
            recordSubAgentHistory(runtime, agentResult);
            String assistantContent = resolveAssistantContent(agentResult, runtime.context);
            if (!StringUtils.hasText(assistantContent)) {
                throw new JeecgBootException("AI回复为空，请稍后重试");
            }
            saveAssistantReply(runtime, agentResult, assistantContent);
        } catch (Exception ex) {
            log.error("Agent流式回复执行失败，sessionId={}, userMessageId={}", runtime.session.getId(), runtime.userMessage.getId(), ex);
        } finally {
            completeEmitter(emitter);
            this.sseConnectionManager.remove(connectionKey);
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
            throw new JeecgBootException("未登录或登录已过期");
        }
        if (request == null) {
            throw new JeecgBootException("请求参数不能为空");
        }
        request.applyDefaults();

        String userInput = normalizeText(request.getUserInput());
        if (!StringUtils.hasText(userInput)) {
            throw new JeecgBootException("userInput不能为空");
        }

        TsAgentChatSession session = tsAgentChatSessionService.getOwnedSession(user.getId(), sessionId);
        if (session == null) {
            throw new JeecgBootException("会话不存在或无权限访问");
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
        ReplyRuntime runtime = new ReplyRuntime();
        runtime.session = session;
        runtime.userMessage = userMessage;
        runtime.recentMessages = recentMessages;
        runtime.variables = variables;
        runtime.context = context;
        runtime.userInput = userInput;
        return runtime;
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
            throw new JeecgBootException("运行上下文不能为空");
        }
        if (!StringUtils.hasText(assistantContent)) {
            throw new JeecgBootException("AI回复为空，请稍后重试");
        }
        String promptCode = extractString(agentResult == null ? null : agentResult.getData(), "promptCode");
        String promptVersion = extractString(agentResult == null ? null : agentResult.getData(), "promptVersion");
        String messageStatus = toMessageStatus(agentResult == null ? null : agentResult.getStatus());

        TsAgentChatMessage assistantMessage = tsAgentChatMessageService.saveAssistantMessage(
                runtime.context.getUserId(),
                runtime.session.getId(),
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
     * 发送流式终止事件。
     *
     * @param connectionKey SSE 连接键
     * @param message 错误消息
     */
    private void sendStreamEnd(String connectionKey, String message) {
        if (!StringUtils.hasText(connectionKey)) {
            return;
        }
        SsePayload payload = new SsePayload();
        payload.setEvent("agent.end");
        payload.setContent(message);
        payload.setStatus(0);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("errorMessage", message);
        data.put("status", "FAILED");
        payload.setData(data);
        this.sseConnectionManager.send(connectionKey, "agent.end", payload);
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
        String assistantContent = normalizeText(agentResult == null ? null : agentResult.getContent());
        if (!StringUtils.hasText(assistantContent) && agentResult != null && agentResult.getData() != null) {
            assistantContent = normalizeText(extractString(agentResult.getData(), "formattedResult"));
        }
        if (!StringUtils.hasText(assistantContent) && context != null) {
            assistantContent = normalizeText(context.getLatestContent());
        }
        return assistantContent;
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
        context.setAgentCode(normalizeAgentCode(session.getAgentCode()));
        context.setSenderType(SENDER_MAIN_AGENT);
        context.setUserId(user == null || user.getId() == null ? null : String.valueOf(user.getId()));
        context.setUserInput(userInput);
        context.putAttribute("sessionMemoryJson", session.getMemoryJson());
        context.putAttribute("sessionStateJson", session.getStateJson());
        context.putAttribute(ATTR_SESSION_SUB_AGENT_HISTORY_JSON, session.getSubAgentHistoryJson());
        context.putAttribute(ATTR_SUB_AGENT_HISTORY_JSON, session.getSubAgentHistoryJson());
        context.putAttribute("sessionTitle", session.getSessionTitle());
        context.putAttribute("sessionSummary", session.getSessionSummary());
        context.putAttribute("recentMessagesBlock", buildRecentMessagesBlock(recentMessages));
        context.putAttribute("lastAssistantMessage", findLastAssistantMessage(recentMessages));
        context.putAttribute("promptVariables", variables);
        return context;
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
        putIfHas(variables, "recent_messages_block", buildRecentMessagesBlock(recentMessages));
        return variables;
    }

    /**
     * 组装最近消息文本块。
     */
    private String buildRecentMessagesBlock(List<TsAgentChatMessage> recentMessages) {
        if (recentMessages == null || recentMessages.isEmpty()) {
            return "无";
        }
        List<String> lines = new ArrayList<>();
        for (TsAgentChatMessage message : recentMessages) {
            if (message == null || !StringUtils.hasText(message.getContent())) {
                continue;
            }
            String roleLabel = normalizeRoleLabel(message.getRoleType());
            lines.add("【" + roleLabel + "】" + message.getContent().trim());
        }
        return lines.isEmpty() ? "无" : String.join("\n", lines);
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
        ext.put("agentName", tsAgentChatAgent.agentName());
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
        ext.put("subAgentHistoryCount", SubAgentHistorySupport.countHistory(oConvertUtils.getString(context == null ? null : context.getAttribute(ATTR_SUB_AGENT_HISTORY_JSON))));
        ext.put("subAgentHistoryBlock", context == null ? null : context.getAttribute(ATTR_SUB_AGENT_HISTORY_BLOCK));
        ext.put("promptVariables", promptVariables);
        return ext.toJSONString();
    }

    /**
     * 将本轮子 Agent 的执行结果写回会话历史。
     *
     * <p>仅保留每个子 Agent 最近两条记录，供后续同名子 Agent 续跑或重试时使用。</p>
     *
     * @param runtime 运行上下文
     * @param agentResult Agent 结果
     */
    private void recordSubAgentHistory(ReplyRuntime runtime, AgentResult agentResult) {
        if (runtime == null || runtime.session == null || agentResult == null) {
            return;
        }
        String subAgentName = resolveSubAgentName(runtime, agentResult);
        if (!StringUtils.hasText(subAgentName)) {
            return;
        }
        Object structuredResult = extractStructuredResult(agentResult);
        if (agentResult.getStatus() == AgentResult.Status.WAITING_USER && structuredResult == null) {
            return;
        }

        JSONObject record = new JSONObject();
        record.put("runId", runtime.context == null ? null : runtime.context.getRunId());
        record.put("status", agentResult.getStatus() == null ? null : agentResult.getStatus().name());
        record.put("summary", normalizeText(agentResult.getContent()));
        record.put("resultJson", structuredResult);
        record.put("error", extractErrorMessage(agentResult));
        record.put("toolName", extractString(agentResult.getData(), "toolName"));
        record.put("description", extractString(agentResult.getData(), "description"));
        record.put("executionMode", extractString(agentResult.getData(), "executionMode"));
        record.put("events", runtime.context == null ? null : runtime.context.snapshotEvents());
        record.put("createdAt", new Date());

        String updatedHistoryJson = SubAgentHistorySupport.appendHistoryJson(
                runtime.session.getSubAgentHistoryJson(),
                subAgentName,
                record,
                SubAgentHistorySupport.DEFAULT_HISTORY_LIMIT
        );
        runtime.session.setSubAgentHistoryJson(updatedHistoryJson);
        runtime.session.setUpdatedAt(new Date());
        tsAgentChatSessionService.updateById(runtime.session);

        if (runtime.context != null) {
            String selectedHistoryJson = SubAgentHistorySupport.selectHistoryJson(updatedHistoryJson, subAgentName);
            runtime.context.putAttribute(ATTR_SESSION_SUB_AGENT_HISTORY_JSON, updatedHistoryJson);
            runtime.context.putAttribute(ATTR_SUB_AGENT_HISTORY_JSON, selectedHistoryJson);
            runtime.context.putAttribute(ATTR_SUB_AGENT_HISTORY_BLOCK, SubAgentHistorySupport.buildHistoryBlock(selectedHistoryJson));
        }
    }

    /**
     * 解析本轮实际命中的子 Agent 名称。
     *
     * @param runtime 运行上下文
     * @param agentResult Agent 结果
     * @return 子 Agent 名称
     */
    private String resolveSubAgentName(ReplyRuntime runtime, AgentResult agentResult) {
        String resolved = extractString(agentResult == null ? null : agentResult.getData(), "targetSubAgent");
        if (StringUtils.hasText(resolved)) {
            return resolved;
        }
        resolved = extractString(agentResult == null ? null : agentResult.getData(), "resolvedAgent");
        if (StringUtils.hasText(resolved)) {
            return resolved;
        }
        resolved = extractString(agentResult == null ? null : agentResult.getData(), "subAgentName");
        if (StringUtils.hasText(resolved)) {
            return resolved;
        }
        return null;
    }

    /**
     * 提取结构化结果 JSON。
     *
     * @param agentResult Agent 结果
     * @return 结构化结果
     */
    private Object extractStructuredResult(AgentResult agentResult) {
        if (agentResult == null) {
            return null;
        }
        Object structuredResult = agentResult.getStructuredResult();
        if (structuredResult != null) {
            return structuredResult;
        }
        if (agentResult.getData() == null) {
            return null;
        }
        Object value = agentResult.getData().get("resultJson");
        if (value == null) {
            value = agentResult.getData().get("result");
        }
        if (value == null) {
            return null;
        }
        if (value instanceof JSONObject || value instanceof com.alibaba.fastjson.JSONArray) {
            return value;
        }
        if (value instanceof String text) {
            String normalized = normalizeText(text);
            if (!StringUtils.hasText(normalized)) {
                return null;
            }
            try {
                return JSONObject.parse(normalized);
            } catch (Exception ignore) {
                return normalized;
            }
        }
        try {
            return JSONObject.parse(JSONObject.toJSONString(value));
        } catch (Exception ignore) {
            return String.valueOf(value);
        }
    }

    /**
     * 提取错误信息。
     *
     * @param agentResult Agent 结果
     * @return 错误信息
     */
    private String extractErrorMessage(AgentResult agentResult) {
        if (agentResult == null || agentResult.getData() == null) {
            return null;
        }
        String errorMessage = extractString(agentResult.getData(), "errorMessage");
        if (StringUtils.hasText(errorMessage)) {
            return errorMessage;
        }
        errorMessage = extractString(agentResult.getData(), "message");
        if (StringUtils.hasText(errorMessage)) {
            return errorMessage;
        }
        return agentResult.getStatus() == AgentResult.Status.FAILED ? normalizeText(agentResult.getContent()) : null;
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
     * 角色标签归一化。
     */
    private String normalizeRoleLabel(String roleType) {
        String normalized = normalizeText(roleType);
        if (!StringUtils.hasText(normalized)) {
            return ROLE_ASSISTANT;
        }
        String lower = normalized.toLowerCase(Locale.ROOT);
        if (ROLE_USER.equals(lower)) {
            return "用户";
        }
        if (ROLE_ASSISTANT.equals(lower)) {
            return "助手";
        }
        if (ROLE_SYSTEM.equals(lower)) {
            return "系统";
        }
        if (ROLE_TOOL.equals(lower)) {
            return "工具";
        }
        return normalized;
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
    }
}
