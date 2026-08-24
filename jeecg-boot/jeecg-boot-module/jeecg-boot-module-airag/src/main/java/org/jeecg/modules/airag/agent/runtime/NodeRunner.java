package org.jeecg.modules.airag.agent.runtime;

import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.modules.airag.agent.error.AgentErrorCode;
import org.jeecg.modules.airag.agent.error.AgentErrorException;
import org.jeecg.modules.airag.agent.graph.AgentNode;
import org.jeecg.modules.airag.agent.graph.LlmNodeDefinition;
import org.jeecg.modules.airag.agent.graph.NodeKind;
import org.jeecg.modules.airag.agent.graph.NodeResult;
import org.jeecg.modules.airag.agent.node.ConfirmationNode;
import org.jeecg.modules.airag.agent.node.LlmNode;
import org.jeecg.modules.airag.agent.node.OptionsNode;
import org.jeecg.modules.airag.agent.node.ToolNode;
import org.jeecg.modules.airag.agent.safety.GlobalSafetySkillPromptProvider;
import org.jeecg.modules.airag.agent.skill.registry.SkillRegistry;
import org.jeecg.modules.airag.safety.moderation.ModerationGuard;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 节点执行包装器。
 *
 * @author codex
 * @date 2026/6/16
 */
@Component
@Slf4j
public class NodeRunner {
    /**
     * 所有 Agent LLM 节点统一注入的回复语言 Skill。
     */
    private static final String AGENT_RESPONSE_LANGUAGE_SKILL = "agent_response_language";
    /**
     * 所有 Agent LLM 节点统一注入的候选项 Skill。
     */
    private static final String AGENT_CANDIDATE_OPTIONS_SKILL = "agent_candidate_options";
    /**
     * 所有子 Agent LLM 节点统一注入的回复风格 Skill。
     */
    private static final String SUB_AGENT_RESPONSE_STYLE_SKILL = "subagent_response_style";
    /**
     * 事件发布器。
     */
    private final AgentEventPublisher eventPublisher;
    /**
     * Skill 注册中心，用于执行前准备节点上下文。
     */
    private final SkillRegistry skillRegistry;
    /**
     * 全局安全 Skill Prompt 提供器。
     */
    private final GlobalSafetySkillPromptProvider globalSafetySkillPromptProvider;
    /**
     * 统一文本审核门禁。
     */
    private final ModerationGuard moderationGuard;

    /**
     * 构造函数。
     *
     * @param eventPublisher 事件发布器
     */
    public NodeRunner(AgentEventPublisher eventPublisher,
                      SkillRegistry skillRegistry,
                      GlobalSafetySkillPromptProvider globalSafetySkillPromptProvider,
                      ModerationGuard moderationGuard) {
        this.eventPublisher = eventPublisher;
        this.skillRegistry = skillRegistry;
        this.globalSafetySkillPromptProvider = globalSafetySkillPromptProvider;
        this.moderationGuard = moderationGuard;
    }

    /**
     * 执行一个节点并统一发送首尾事件。
     *
     * @param context 运行上下文
     * @param node 节点对象
     * @return 节点结果
     */
    public NodeResult run(AgentContext context, AgentNode node) {
        AgentRunControlService.throwIfStopRequested(context);
        if (node.kind() == NodeKind.LLM) {
            return runLlmNode(context, (LlmNode) node);
        }
        if (node.kind() == NodeKind.CONFIRM) {
            return runConfirmNode(context, (ConfirmationNode) node);
        }
        if (node.kind() == NodeKind.OPTIONS) {
            return runOptionsNode(context, (OptionsNode) node);
        }
        if (node.kind() == NodeKind.TOOL) {
            return runToolNode(context, (ToolNode) node);
        }
        throw new AgentErrorException(
                AgentErrorCode.RUNTIME_NODE_TYPE_UNSUPPORTED,
                Map.of("nodeKind", String.valueOf(node.kind()))
        );
    }

    /**
     * 执行 LLM 节点。
     *
     * @param context 运行上下文
     * @param node LLM 节点
     * @return 节点结果
     */
    private NodeResult runLlmNode(AgentContext context, LlmNode node) {
        boolean success = false;
        boolean interrupted = false;
        NodeResult result = null;
        markCurrentNode(context, node);
        prepareNodeSkillContext(context, node);
        this.eventPublisher.publishLlmStart(context, node.nodeName(), node.getPromptCode());
        try {
            result = node.execute(context);
            AgentRunControlService.throwIfStopRequested(context);
            success = result != null && result.isSuccess();
            markResultNode(context, node, result, success);
            return result;
        } catch (Exception ex) {
            if (AgentRunControlService.isInterrupted(ex, context)) {
                interrupted = true;
                throw new AgentRunInterruptedException();
            }
            this.eventPublisher.publishLlmError(context, node.nodeName(), node.getPromptCode(), ex);
            throw new RuntimeException(ex);
        } finally {
            Map<String, Object> payload = result == null ? new LinkedHashMap<>() : result.getData();
            if (interrupted || AgentRunControlService.isStopRequested(context)) {
                payload.put("stopReason", "user_stop");
                this.eventPublisher.publishLlmEnd(context, node.nodeName(), node.getPromptCode(), 3, payload);
            } else {
                this.eventPublisher.publishLlmEnd(context, node.nodeName(), node.getPromptCode(), success, payload);
            }
        }
    }

    /**
     * 在 LLM 节点执行前，将节点显式绑定的 Skill 正文注入上下文。
     *
     * @param context 运行上下文
     * @param node LLM 节点
     */
    private void prepareNodeSkillContext(AgentContext context, LlmNode node) {
        if (context == null) {
            throw new JeecgBootException("Agent运行上下文为空，禁止执行LLM节点");
        }
        context.putAttribute("loadedNodeSkillCodes", new ArrayList<>());
        context.putAttribute("nodeSkillPrompt", "");
        context.putAttribute("safetySkillPrompt", "");
        if (this.moderationGuard == null) {
            throw new JeecgBootException("AI审核服务不可用，禁止执行LLM节点");
        }
        context.putAttribute("moderationGuard", this.moderationGuard);
        if (this.skillRegistry == null
                || this.globalSafetySkillPromptProvider == null
                || node == null
                || node.getDefinition() == null) {
            throw new JeecgBootException("全局安全Skill加载器不可用，禁止执行LLM节点");
        }
        LlmNodeDefinition definition = node.getDefinition();
        String safetySkillPrompt = loadRequiredSafetySkill(node);
        List<String> skillCodes = new ArrayList<>();
        skillCodes.add(AGENT_RESPONSE_LANGUAGE_SKILL);
        skillCodes.add(AGENT_CANDIDATE_OPTIONS_SKILL);
        if (isSubAgentContext(context)) {
            skillCodes.add(SUB_AGENT_RESPONSE_STYLE_SKILL);
        }
        if (definition.getSkills() != null) {
            skillCodes.addAll(definition.getSkills());
        }
        List<String> loadedSkillCodes = new ArrayList<>();
        loadedSkillCodes.add(GlobalSafetySkillPromptProvider.SKILL_CODE);
        StringBuilder prompt = new StringBuilder();
        for (String skillCode : skillCodes) {
            if (!StringUtils.hasText(skillCode) || loadedSkillCodes.contains(skillCode)) {
                continue;
            }
            try {
                String skillBody = normalizeSkillBody(this.skillRegistry.getSkillBody(skillCode));
                if (!StringUtils.hasText(skillBody)) {
                    continue;
                }
                if (prompt.length() > 0) {
                    prompt.append("\n\n");
                }
                prompt.append(skillBody.trim());
                loadedSkillCodes.add(skillCode);
            } catch (Exception ex) {
                log.warn("加载节点 Skill 失败，nodeName={}, skillCode={}", node.nodeName(), skillCode, ex);
            }
        }
        if (loadedSkillCodes.contains(AGENT_RESPONSE_LANGUAGE_SKILL)) {
            appendPrompt(prompt, AgentResponseLanguageSupport.buildInstruction(context));
        }
        context.putAttribute("loadedNodeSkillCodes", loadedSkillCodes);
        context.putAttribute("nodeSkillPrompt", prompt.toString().trim());
        context.putAttribute("safetySkillPrompt", safetySkillPrompt);
    }

    /**
     * 强制读取全局安全 Skill，缺失或空正文时禁止继续调用模型。
     *
     * @param node 当前 LLM 节点
     * @return 可直接注入 System Prompt 的安全规则正文
     */
    private String loadRequiredSafetySkill(LlmNode node) {
        try {
            return this.globalSafetySkillPromptProvider.requiredSafetyPrompt();
        } catch (Exception ex) {
            log.error("加载全局安全Skill失败，nodeName={}, skillCode={}",
                    node == null ? null : node.nodeName(), GlobalSafetySkillPromptProvider.SKILL_CODE, ex);
            throw new JeecgBootException("全局安全Skill加载失败，禁止执行LLM节点");
        }
    }

    /**
     * 追加一段节点系统提示词。
     *
     * @param prompt 提示词容器
     * @param content 追加内容
     */
    private void appendPrompt(StringBuilder prompt, String content) {
        if (prompt == null || !StringUtils.hasText(content)) {
            return;
        }
        if (prompt.length() > 0) {
            prompt.append("\n\n");
        }
        prompt.append(content.trim());
    }

    /**
     * 判断当前 LLM 节点是否运行在子 Agent 上下文。
     */
    private boolean isSubAgentContext(AgentContext context) {
        if (context == null) {
            return false;
        }
        if (context.getAttribute("subAgentDefinition") != null) {
            return true;
        }
        Object subAgentName = context.getAttribute("taskSubAgentName");
        return subAgentName != null && StringUtils.hasText(String.valueOf(subAgentName));
    }

    /**
     * 节点上下文只注入 Skill 正文，不把 YAML frontmatter 当作提示词内容。
     *
     * @param skillBody Skill 原始内容
     * @return 可直接注入模型上下文的正文
     */
    private String normalizeSkillBody(String skillBody) {
        if (!StringUtils.hasText(skillBody)) {
            return "";
        }
        String normalized = skillBody.replace("\r\n", "\n").replace('\r', '\n').trim();
        if (!normalized.startsWith("---\n")) {
            return normalized;
        }
        int end = normalized.indexOf("\n---", 4);
        if (end < 0) {
            return normalized;
        }
        int bodyStart = end + "\n---".length();
        if (bodyStart < normalized.length() && normalized.charAt(bodyStart) == '\n') {
            bodyStart++;
        }
        return normalized.substring(Math.min(bodyStart, normalized.length())).trim();
    }

    /**
     * 执行 Tool 节点。
     *
     * @param context 运行上下文
     * @param node Tool 节点
     * @return 节点结果
     */
    private NodeResult runToolNode(AgentContext context, ToolNode node) {
        boolean success = false;
        boolean interrupted = false;
        NodeResult result = null;
        markCurrentNode(context, node);
        Map<String, Object> startPayload = new LinkedHashMap<>();
        startPayload.put("toolName", node.getToolName());
        this.eventPublisher.publishToolStart(context, node.nodeName(), node.getToolName(), startPayload);
        try {
            result = node.execute(context);
            success = result != null && result.isSuccess();
            markResultNode(context, node, result, success);
            return result;
        } catch (Exception ex) {
            if (AgentRunControlService.isInterrupted(ex, context)) {
                interrupted = true;
                throw new AgentRunInterruptedException();
            }
            Map<String, Object> errorPayload = new LinkedHashMap<>();
            errorPayload.put("toolName", node.getToolName());
            this.eventPublisher.publishToolError(context, node.nodeName(), node.getToolName(), ex, errorPayload);
            throw new RuntimeException(ex);
        } finally {
            Map<String, Object> payload = result == null ? new LinkedHashMap<>() : result.getData();
            if (interrupted) {
                payload.put("stopReason", "user_stop");
                this.eventPublisher.publishToolEnd(
                        context,
                        node.nodeName(),
                        node.getToolName(),
                        3,
                        result == null ? "" : result.getContent(),
                        payload
                );
            } else {
                this.eventPublisher.publishToolEnd(
                        context,
                        node.nodeName(),
                        node.getToolName(),
                        success,
                        result == null ? "" : result.getContent(),
                        payload
                );
            }
        }
    }

    /**
     * 执行用户确认节点。
     *
     * @param context 运行上下文
     * @param node 确认节点
     * @return 节点结果
     */
    private NodeResult runConfirmNode(AgentContext context, ConfirmationNode node) {
        AgentRunControlService.throwIfStopRequested(context);
        markCurrentNode(context, node);
        try {
            NodeResult result = node.execute(context);
            AgentRunControlService.throwIfStopRequested(context);
            boolean success = result != null && result.isSuccess();
            markResultNode(context, node, result, success);
            if (success && node.isWaitingAction(result.getAction())) {
                this.eventPublisher.publishConfirmStart(
                        context,
                        node.getStartSseName(),
                        node.nodeName(),
                        node.getQuestion(),
                        node.getOptions()
                );
            } else if (success) {
                this.eventPublisher.publishConfirmEnd(
                        context,
                        node.getEndSseName(),
                        node.nodeName(),
                        node.getQuestion(),
                        node.getOptions(),
                        result.getData()
                );
            }
            return result;
        } catch (Exception ex) {
            this.eventPublisher.publishConfirmError(
                    context,
                    node.getErrorSseName(),
                    node.nodeName(),
                    node.getQuestion(),
                    node.getOptions(),
                    ex
            );
            throw new RuntimeException(ex);
        }
    }

    /**
     * 执行用户候选项选择节点。
     *
     * @param context 运行上下文
     * @param node 候选项节点
     * @return 节点结果
     */
    private NodeResult runOptionsNode(AgentContext context, OptionsNode node) {
        AgentRunControlService.throwIfStopRequested(context);
        markCurrentNode(context, node);
        try {
            NodeResult result = node.execute(context);
            AgentRunControlService.throwIfStopRequested(context);
            boolean success = result != null && result.isSuccess();
            markResultNode(context, node, result, success);
            if (success && node.isWaitingAction(result.getAction())) {
                this.eventPublisher.publishOptionsStart(
                        context,
                        node.getStartSseName(),
                        node.nodeName(),
                        node.getQuestion(),
                        node.getOptions()
                );
            } else if (success) {
                this.eventPublisher.publishOptionsEnd(
                        context,
                        node.getEndSseName(),
                        node.nodeName(),
                        node.getQuestion(),
                        node.getOptions(),
                        result.getData()
                );
            }
            return result;
        } catch (Exception ex) {
            this.eventPublisher.publishOptionsError(
                    context,
                    node.getErrorSseName(),
                    node.nodeName(),
                    node.getQuestion(),
                    node.getOptions(),
                    ex
            );
            throw new RuntimeException(ex);
        }
    }

    /**
     * 标记当前执行节点。
     *
     * @param context 运行上下文
     * @param node 节点
     */
    private void markCurrentNode(AgentContext context, AgentNode node) {
        if (context == null || node == null) {
            return;
        }
        context.markCurrentNode(node.nodeName(), nodeType(node.kind()));
    }

    /**
     * 成功节点返回正文后，记录为当前最终结果节点。
     *
     * @param context 运行上下文
     * @param node 节点
     * @param result 节点结果
     * @param success 是否成功
     */
    private void markResultNode(AgentContext context,
                                AgentNode node,
                                NodeResult result,
                                boolean success) {
        if (context == null || node == null || result == null) {
            return;
        }
        context.markResultNode(node.nodeName(), nodeType(node.kind()), result.getContent(), success);
    }

    /**
     * 转换节点类型为存储值。
     *
     * @param kind 节点枚举
     * @return 小写节点类型
     */
    private String nodeType(NodeKind kind) {
        return kind == null ? null : kind.name().toLowerCase();
    }
}
