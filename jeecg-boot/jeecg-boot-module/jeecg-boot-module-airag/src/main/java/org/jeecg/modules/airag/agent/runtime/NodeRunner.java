package org.jeecg.modules.airag.agent.runtime;

import org.jeecg.modules.airag.agent.graph.AgentNode;
import org.jeecg.modules.airag.agent.graph.NodeKind;
import org.jeecg.modules.airag.agent.graph.NodeResult;
import org.jeecg.modules.airag.agent.node.LlmNode;
import org.jeecg.modules.airag.agent.node.ToolNode;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 节点执行包装器。
 *
 * @author codex
 * @date 2026/6/16
 */
@Component
public class NodeRunner {
    /**
     * 事件发布器。
     */
    private final AgentEventPublisher eventPublisher;

    /**
     * 构造函数。
     *
     * @param eventPublisher 事件发布器
     */
    public NodeRunner(AgentEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
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
        this.eventPublisher.publishLlmStart(context, node.nodeName(), node.getPromptCode());
        try {
            result = node.execute(context);
            success = result != null && result.isSuccess();
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
     * 执行 Tool 节点。
     *
     * @param context 运行上下文
     * @param node Tool 节点
     * @return 节点结果
     */
    private NodeResult runToolNode(AgentContext context, ToolNode node) {
        boolean success = false;
        NodeResult result = null;
        Map<String, Object> startPayload = new LinkedHashMap<>();
        startPayload.put("toolName", node.getToolName());
        this.eventPublisher.publishToolStart(context, node.nodeName(), node.getToolName(), startPayload);
        try {
            result = node.execute(context);
            success = result != null && result.isSuccess();
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
}
