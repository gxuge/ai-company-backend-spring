package org.jeecg.modules.airag.agent.subagent.story;

import org.jeecg.common.util.oConvertUtils;
import org.jeecg.modules.airag.agent.graph.NodeResult;
import org.jeecg.modules.airag.agent.graph.SubAgent;
import org.jeecg.modules.airag.agent.runtime.AgentContext;
import org.jeecg.modules.airag.agent.runtime.AgentHandoffSupport;
import org.jeecg.modules.airag.agent.runtime.AgentResult;
import org.jeecg.modules.airag.agent.runtime.NodeRunner;
import org.jeecg.modules.airag.agent.subagent.story.node.StoryCreateBackgroundNode;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 独立故事背景生成子 Agent。
 */
@Component
public class StoryBackgroundTaskSubAgent implements SubAgent {
    public static final String SUB_AGENT_NAME = "story_background_task_agent";

    private final NodeRunner nodeRunner;
    private final StoryCreateBackgroundNode storyCreateBackgroundNode;

    public StoryBackgroundTaskSubAgent(NodeRunner nodeRunner,
                                       StoryCreateBackgroundNode storyCreateBackgroundNode) {
        this.nodeRunner = nodeRunner;
        this.storyCreateBackgroundNode = storyCreateBackgroundNode;
    }

    @Override
    public String subAgentName() {
        return SUB_AGENT_NAME;
    }

    @Override
    public AgentResult execute(AgentContext context) {
        try {
            NodeResult nodeResult = this.nodeRunner.run(context, this.storyCreateBackgroundNode);
            if (isHandoff(context, nodeResult)) {
                return AgentHandoffSupport.buildHandoffResult(context, subAgentName(), "background");
            }
            if (nodeResult == null || !nodeResult.isSuccess()) {
                return AgentResult.failed(nodeResult == null ? "故事背景生成节点未返回结果" : nodeResult.getErrorMessage());
            }

            String content = nodeResult.getContent();
            if (!oConvertUtils.isNotEmpty(content) && context != null) {
                content = context.getLatestContent();
            }
            if (!oConvertUtils.isNotEmpty(content)) {
                content = "故事背景已生成";
            }

            Map<String, Object> structuredResult = new LinkedHashMap<>();
            structuredResult.put("storyBackgroundResultJson", context == null ? null : context.getAttribute("storyBackgroundResultJson"));
            structuredResult.put("storySceneResultJson", context == null ? null : context.getAttribute("storySceneResultJson"));
            structuredResult.put("storySceneImageResultJson", context == null ? null : context.getAttribute("storySceneImageResultJson"));
            structuredResult.put("nodeResult", nodeResult.getData());
            structuredResult.put("nodeContent", nodeResult.getContent());
            return AgentHandoffSupport.buildCompletedHandoffResult(
                    context,
                    subAgentName(),
                    content,
                    structuredResult
            );
        } catch (Exception ex) {
            return AgentResult.failed(ex.getMessage());
        }
    }

    private boolean isHandoff(AgentContext context, NodeResult nodeResult) {
        return AgentHandoffSupport.isHandoff(nodeResult)
                || !AgentHandoffSupport.getHandoffPayload(context).isEmpty();
    }
}
