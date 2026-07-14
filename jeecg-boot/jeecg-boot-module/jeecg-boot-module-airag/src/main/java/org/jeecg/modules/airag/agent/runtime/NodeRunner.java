package org.jeecg.modules.airag.agent.runtime;

import lombok.extern.slf4j.Slf4j;
import org.jeecg.modules.airag.agent.graph.AgentNode;
import org.jeecg.modules.airag.agent.graph.LlmNodeDefinition;
import org.jeecg.modules.airag.agent.graph.NodeKind;
import org.jeecg.modules.airag.agent.graph.NodeResult;
import org.jeecg.modules.airag.agent.node.LlmNode;
import org.jeecg.modules.airag.agent.node.ToolNode;
import org.jeecg.modules.airag.agent.skill.registry.SkillRegistry;
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
     * 事件发布器。
     */
    private final AgentEventPublisher eventPublisher;
    /**
     * Skill 注册中心，用于执行前准备节点上下文。
     */
    private final SkillRegistry skillRegistry;

    /**
     * 构造函数。
     *
     * @param eventPublisher 事件发布器
     */
    public NodeRunner(AgentEventPublisher eventPublisher,
                      SkillRegistry skillRegistry) {
        this.eventPublisher = eventPublisher;
        this.skillRegistry = skillRegistry;
    }

    /**
     * 执行一个节点并统一发送首尾事件。
     *
     * @param context 运行上下文
     * @param node 节点对象
     * @return 节点结果
     */
    public NodeResult run(AgentContext context, AgentNode node) {
        if (node.kind() == NodeKind.LLM) {
            return runLlmNode(context, (LlmNode) node);
        }
        return runToolNode(context, (ToolNode) node);
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
        NodeResult result = null;
        markCurrentNode(context, node);
        prepareNodeSkillContext(context, node);
        this.eventPublisher.publishLlmStart(context, node.nodeName(), node.getPromptCode());
        try {
            result = node.execute(context);
            success = result != null && result.isSuccess();
            markResultNode(context, node, result, success);
            return result;
        } catch (Exception ex) {
            this.eventPublisher.publishLlmError(context, node.nodeName(), node.getPromptCode(), ex);
            throw new RuntimeException(ex);
        } finally {
            Map<String, Object> payload = result == null ? new LinkedHashMap<>() : result.getData();
            this.eventPublisher.publishLlmEnd(context, node.nodeName(), node.getPromptCode(), success, payload);
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
            return;
        }
        context.putAttribute("loadedNodeSkillCodes", new ArrayList<>());
        context.putAttribute("nodeSkillPrompt", "");
        if (this.skillRegistry == null || node == null || node.getDefinition() == null) {
            return;
        }
        LlmNodeDefinition definition = node.getDefinition();
        List<String> skillCodes = definition.getSkills();
        if (skillCodes == null || skillCodes.isEmpty()) {
            return;
        }
        List<String> loadedSkillCodes = new ArrayList<>();
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
        context.putAttribute("loadedNodeSkillCodes", loadedSkillCodes);
        context.putAttribute("nodeSkillPrompt", prompt.toString().trim());
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
            Map<String, Object> errorPayload = new LinkedHashMap<>();
            errorPayload.put("toolName", node.getToolName());
            this.eventPublisher.publishToolError(context, node.nodeName(), node.getToolName(), ex, errorPayload);
            throw new RuntimeException(ex);
        } finally {
            Map<String, Object> payload = result == null ? new LinkedHashMap<>() : result.getData();
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
