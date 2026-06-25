package org.jeecg.modules.system.service.impl;

import com.alibaba.fastjson.JSONObject;
import jakarta.annotation.Resource;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.common.system.vo.LoginUser;
import org.jeecg.modules.airag.agent.chat.TsAgentChatAgent;
import org.jeecg.modules.airag.agent.runtime.AgentContext;
import org.jeecg.modules.airag.agent.runtime.AgentResult;
import org.jeecg.modules.airag.agent.runtime.AgentRuntimeService;
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

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Agent 回复编排实现。
 *
 * @author codex
 * @date 2026/6/25
 */
@Service
public class TsAgentChatReplyServiceImpl implements ITsAgentChatReplyService {

    private static final String ROLE_USER = "user";
    private static final String ROLE_ASSISTANT = "assistant";
    private static final String ROLE_SYSTEM = "system";
    private static final String ROLE_TOOL = "tool";

    @Resource
    private ITsAgentChatSessionService tsAgentChatSessionService;

    @Resource
    private ITsAgentChatMessageService tsAgentChatMessageService;

    @Resource
    private AgentRuntimeService agentRuntimeService;

    @Resource
    private TsAgentChatAgent tsAgentChatAgent;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<TsAgentChatReplyVo> createAiReply(LoginUser user, Long sessionId, TsAgentChatReplyDto request) {
        if (user == null) {
            return Result.error("未登录或登录已过期");
        }
        if (request == null) {
            return Result.error("请求参数不能为空");
        }
        request.applyDefaults();

        String userInput = normalizeText(request.getUserInput());
        if (!StringUtils.hasText(userInput)) {
            return Result.error("userInput不能为空");
        }

        TsAgentChatSession session = tsAgentChatSessionService.getOwnedSession(user.getId(), sessionId);
        if (session == null) {
            return Result.error("会话不存在或无权限访问");
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
        AgentResult agentResult = agentRuntimeService.execute(tsAgentChatAgent, context);
        String assistantContent = normalizeText(agentResult == null ? null : agentResult.getContent());
        if (!StringUtils.hasText(assistantContent)) {
            throw new JeecgBootException("AI回复为空，请稍后重试");
        }
        String promptCode = extractString(agentResult == null ? null : agentResult.getData(), "promptCode");
        String promptVersion = extractString(agentResult == null ? null : agentResult.getData(), "promptVersion");
        String messageStatus = toMessageStatus(agentResult == null ? null : agentResult.getStatus());

        TsAgentChatMessage assistantMessage = tsAgentChatMessageService.saveAssistantMessage(
                user.getId(),
                sessionId,
                assistantContent,
                "text",
                messageStatus,
                userMessage.getId(),
                context.getRunId(),
                promptCode,
                session.getAppId(),
                null,
                buildExtJson(session, userInput, context, agentResult, promptCode, promptVersion, variables)
        );

        TsAgentChatReplyVo vo = new TsAgentChatReplyVo();
        vo.setSessionId(sessionId);
        vo.setUserMessageId(userMessage.getId());
        vo.setAssistantMessageId(assistantMessage.getId());
        vo.setContentText(assistantContent);
        vo.setPromptCode(promptCode);
        vo.setPromptVersion(promptVersion);
        vo.setRenderedPrompt(null);
        vo.setCreatedAt(assistantMessage.getCreatedAt() == null ? new Date() : assistantMessage.getCreatedAt());
        return Result.OK(vo);
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
        context.setAgentCode(session.getAgentCode());
        context.setUserId(user == null || user.getId() == null ? null : String.valueOf(user.getId()));
        context.setUserInput(userInput);
        context.putAttribute("sessionMemoryJson", session.getMemoryJson());
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
        putIfHas(variables, "user_input", userInput);
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
        ext.put("agentCode", session == null ? null : session.getAgentCode());
        ext.put("userInput", userInput);
        ext.put("runId", context == null ? null : context.getRunId());
        ext.put("promptCode", promptCode);
        ext.put("promptVersion", promptVersion);
        ext.put("agentResultStatus", agentResult == null || agentResult.getStatus() == null ? null : agentResult.getStatus().name());
        ext.put("routeDecision", agentResult == null ? null : agentResult.getData().get("routeDecision"));
        ext.put("targetSubAgent", agentResult == null ? null : agentResult.getData().get("targetSubAgent"));
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
}
