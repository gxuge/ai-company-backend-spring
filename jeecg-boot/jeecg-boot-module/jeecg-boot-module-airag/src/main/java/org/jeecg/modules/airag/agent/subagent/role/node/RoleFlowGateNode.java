package org.jeecg.modules.airag.agent.subagent.role.node;

import org.jeecg.common.util.oConvertUtils;
import org.jeecg.modules.airag.agent.node.ToolNode;
import org.jeecg.modules.airag.agent.runtime.AgentContext;
import org.jeecg.modules.airag.agent.subagent.role.RoleTaskPromptSupport;
import org.jeecg.modules.airag.agent.subagent.role.tool.RoleTaskToolSpec;
import org.jeecg.modules.airag.agent.tool.ToolCallRequest;
import org.jeecg.modules.airag.agent.tool.ToolRegistry;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 角色流程门禁节点。
 *
 * <p>用于判断核心设定是否可以进入形象与声音阶段。</p>
 *
 * @author codex
 * @date 2026/7/11
 */
@Component
public class RoleFlowGateNode extends ToolNode {

    public RoleFlowGateNode(ToolRegistry toolRegistry) {
        super("role_flow_gate", "角色流程门禁", RoleTaskToolSpec.ROLE_FLOW_GATE, toolRegistry);
    }

    @Override
    protected ToolCallRequest buildRequest(AgentContext context) {
        ToolCallRequest request = new ToolCallRequest();
        Map<String, Object> arguments = new LinkedHashMap<>();
        String stage = oConvertUtils.getString(context == null ? null : context.getAttribute("roleFlowStage"));
        if (!oConvertUtils.isNotEmpty(stage)) {
            stage = RoleTaskPromptSupport.isConfirmation(oConvertUtils.getString(context == null ? null : context.getUserInput()))
                    ? "core_confirm"
                    : "preset";
        }
        arguments.put("stage", stage);
        arguments.put("userInput", oConvertUtils.getString(context == null ? null : context.getUserInput()));
        request.setArguments(arguments);
        if (context != null) {
            context.putAttribute("roleFlowStage", stage);
        }
        return request;
    }
}
