package org.jeecg.modules.airag.agent.subagent.story.node;

import org.jeecg.common.util.oConvertUtils;
import org.jeecg.modules.airag.agent.node.ToolNode;
import org.jeecg.modules.airag.agent.runtime.AgentContext;
import org.jeecg.modules.airag.agent.subagent.story.StoryTaskPromptSupport;
import org.jeecg.modules.airag.agent.subagent.story.tool.StoryTaskToolSpec;
import org.jeecg.modules.airag.agent.tool.ToolCallRequest;
import org.jeecg.modules.airag.agent.tool.ToolRegistry;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 故事流程门禁节点。
 *
 * <p>用于判断故事核心设定是否可以进入背景 / 场景阶段。</p>
 *
 * @author codex
 * @date 2026/7/11
 */
@Component
public class StoryFlowGateNode extends ToolNode {

    public StoryFlowGateNode(ToolRegistry toolRegistry) {
        super("story_flow_gate", "故事流程门禁", StoryTaskToolSpec.STORY_FLOW_GATE, toolRegistry);
    }

    @Override
    protected ToolCallRequest buildRequest(AgentContext context) {
        ToolCallRequest request = new ToolCallRequest();
        Map<String, Object> arguments = new LinkedHashMap<>();
        String stage = oConvertUtils.getString(context == null ? null : context.getAttribute("storyFlowStage"));
        if (!oConvertUtils.isNotEmpty(stage)) {
            stage = StoryTaskPromptSupport.isConfirmation(oConvertUtils.getString(context == null ? null : context.getUserInput()))
                    ? "story_confirm"
                    : "preset";
        }
        arguments.put("stage", stage);
        request.setArguments(arguments);
        if (context != null) {
            context.putAttribute("storyFlowStage", stage);
        }
        return request;
    }
}
