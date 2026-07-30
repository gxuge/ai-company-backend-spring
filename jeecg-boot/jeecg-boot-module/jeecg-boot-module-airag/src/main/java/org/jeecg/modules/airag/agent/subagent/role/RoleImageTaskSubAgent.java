package org.jeecg.modules.airag.agent.subagent.role;

import org.jeecg.common.util.oConvertUtils;
import org.jeecg.modules.airag.agent.graph.NodeResult;
import org.jeecg.modules.airag.agent.graph.SubAgent;
import org.jeecg.modules.airag.agent.error.AgentErrorCode;
import org.jeecg.modules.airag.agent.error.AgentErrorSupport;
import org.jeecg.modules.airag.agent.runtime.AgentContext;
import org.jeecg.modules.airag.agent.runtime.AgentHandoffSupport;
import org.jeecg.modules.airag.agent.runtime.AgentResult;
import org.jeecg.modules.airag.agent.runtime.NodeRunner;
import org.jeecg.modules.airag.agent.subagent.role.node.RoleCreateImageNode;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 独立角色形象生成子 Agent。
 */
@Component
public class RoleImageTaskSubAgent implements SubAgent {
    public static final String SUB_AGENT_NAME = "role_image_task_agent";

    private final NodeRunner nodeRunner;
    private final RoleCreateImageNode roleCreateImageNode;

    public RoleImageTaskSubAgent(NodeRunner nodeRunner,
                                 RoleCreateImageNode roleCreateImageNode) {
        this.nodeRunner = nodeRunner;
        this.roleCreateImageNode = roleCreateImageNode;
    }

    @Override
    public String subAgentName() {
        return SUB_AGENT_NAME;
    }

    @Override
    public AgentResult execute(AgentContext context) {
        try {
            NodeResult nodeResult = this.nodeRunner.run(context, this.roleCreateImageNode);
            if (isHandoff(context, nodeResult)) {
                return AgentHandoffSupport.buildHandoffResult(context, subAgentName(), "image");
            }
            if (nodeResult == null || !nodeResult.isSuccess()) {
                return AgentResult.failed(nodeResult == null ? "Role image generation returned no result" : nodeResult.getErrorMessage());
            }

            String content = nodeResult.getContent();
            if (!oConvertUtils.isNotEmpty(content) && context != null) {
                content = context.getLatestContent();
            }
            if (!oConvertUtils.isNotEmpty(content)) {
                content = "Role image generated";
            }

            Map<String, Object> structuredResult = new LinkedHashMap<>();
            structuredResult.put("roleImageResultJson", context == null ? null : context.getAttribute("roleImageResultJson"));
            structuredResult.put("nodeResult", nodeResult.getData());
            structuredResult.put("nodeContent", nodeResult.getContent());
            return AgentHandoffSupport.buildTerminalCompletedHandoffResult(
                    context,
                    subAgentName(),
                    content,
                    structuredResult
            );
        } catch (Exception ex) {
            AgentResult result = AgentErrorSupport.failed(
                    AgentErrorCode.GENERATION_ROLE_IMAGE_EXECUTION_FAILED,
                    Map.of("subAgentName", subAgentName())
            );
            AgentErrorSupport.attach(result, ex, AgentErrorCode.GENERATION_ROLE_IMAGE_EXECUTION_FAILED);
            return result;
        }
    }

    private boolean isHandoff(AgentContext context, NodeResult nodeResult) {
        return AgentHandoffSupport.isHandoff(nodeResult)
                || !AgentHandoffSupport.getHandoffPayload(context).isEmpty();
    }
}
