package org.jeecg.modules.airag.agent.main;

import lombok.RequiredArgsConstructor;
import org.jeecg.modules.airag.agent.common.SubAgentRegistry;
import org.jeecg.modules.airag.agent.error.AgentErrorCode;
import org.jeecg.modules.airag.agent.error.AgentErrorSupport;
import org.jeecg.modules.airag.agent.graph.Agent;
import org.jeecg.modules.airag.agent.graph.AgentNode;
import org.jeecg.modules.airag.agent.graph.NodeResult;
import org.jeecg.modules.airag.agent.graph.DeepAgentDefinitionRegistry;
import org.jeecg.modules.airag.agent.interaction.AgentOptionsInteractionSupport;
import org.jeecg.modules.airag.agent.interaction.UserInteractionSupport;
import org.jeecg.modules.airag.agent.runtime.AgentContext;
import org.jeecg.modules.airag.agent.runtime.AgentResult;
import org.jeecg.modules.airag.agent.runtime.NodeRunner;
import org.jeecg.modules.airag.agent.tool.DeepAgentTaskToolService;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Agent 会话总入口。
 *
 * <p>按照 DeepAgents 风格运行：主 Agent 只负责启动主节点，
 * 由模型通过 task 工具自行委托子 Agent，不再做旧式固定分发。</p>
 *
 * @author codex
 * @date 2026/6/25
 */
@Component
@RequiredArgsConstructor
public class TsAgentChatAgent implements Agent {
    /**
     * 节点执行器。
     */
    private final NodeRunner nodeRunner;
    /**
     * DeepAgents 主节点。
     */
    private final TsAgentDeepAgentsMainNode mainNode;
    /**
     * 子 Agent 注册中心。
     */
    private final SubAgentRegistry subAgentRegistry;
    /**
     * Deep Agent 定义注册中心。
     */
    private final DeepAgentDefinitionRegistry deepAgentDefinitionRegistry;
    /**
     * DeepAgents task 工具服务。
     */
    private final DeepAgentTaskToolService deepAgentTaskToolService;

    @Override
    public String agentName() {
        return "ts_agent_chat";
    }

    @Override
    public AgentResult execute(AgentContext context) {
        if (context != null) {
            context.putAttribute("deepAgentsPromptMode", Boolean.TRUE);
            context.putAttribute("deepAgentsMainMode", Boolean.TRUE);
            context.putAttribute("availableSubAgentsPrompt", this.deepAgentDefinitionRegistry.describeAvailableDeepAgents());
            context.putAttribute("subAgentListPrompt", this.subAgentRegistry.describeAvailableSubAgents());
        }
        Map<String, Object> pendingInteraction = UserInteractionSupport.getPending(context);
        if (AgentOptionsInteractionSupport.isCandidateOptions(pendingInteraction)
                && !AgentOptionsInteractionSupport.resumeConversation(context, pendingInteraction)) {
            return AgentOptionsInteractionSupport.waitingResult(
                    context,
                    pendingInteraction,
                    this.mainNode.nodeName(),
                    "dialog"
            );
        }
        NodeResult nodeResult = this.nodeRunner.run(context, (AgentNode) this.mainNode);
        if (nodeResult == null) {
            return AgentResult.failed(AgentErrorCode.RUNTIME_AGENT_EMPTY_RESULT, null);
        }
        AgentResult pendingTaskResult = this.deepAgentTaskToolService.consumePendingHandoff(context);
        if (pendingTaskResult != null) {
            enrichResult(pendingTaskResult, nodeResult);
            return pendingTaskResult;
        }
        pendingInteraction = UserInteractionSupport.getPending(context);
        if (AgentOptionsInteractionSupport.isCandidateOptions(pendingInteraction)) {
            AgentResult waitingResult = AgentOptionsInteractionSupport.waitingResult(
                    context,
                    pendingInteraction,
                    this.mainNode.nodeName(),
                    "dialog"
            );
            enrichResult(waitingResult, nodeResult);
            return waitingResult;
        }
        AgentResult result;
        if (nodeResult.isSuccess()) {
            result = AgentResult.success(nodeResult.getContent());
        } else {
            result = AgentErrorSupport.failed(AgentErrorCode.RUNTIME_AGENT_EXECUTION_FAILED, null);
            String detail = nodeResult.getErrorMessage() == null
                    ? nodeResult.getContent()
                    : nodeResult.getErrorMessage();
            if (detail != null && !detail.isBlank()) {
                result.getData().put("details", java.util.Map.of("originalMessage", detail));
            }
        }
        enrichResult(result, nodeResult);
        return result;
    }

    private void enrichResult(AgentResult result, NodeResult nodeResult) {
        if (result == null) {
            return;
        }
        if (result.getStructuredResult() == null) {
            result.setStructuredResult(nodeResult.getData());
        }
        if (result.getData() == null) {
            result.setData(new LinkedHashMap<>());
        }
        result.getData().put("mainNodeResult", nodeResult.getData());
        result.getData().put("dispatchMode", "deep-agents");
        result.getData().put("mainNode", mainNode == null ? null : mainNode.nodeName());
        result.getData().put("deepAgentsPromptMode", Boolean.TRUE);
    }
}
