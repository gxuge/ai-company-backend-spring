package org.jeecg.modules.airag.agent.subagent.role.node;

import org.jeecg.modules.airag.agent.node.ConfirmationNode;
import org.jeecg.modules.airag.agent.subagent.role.tool.RoleTaskToolSpec;
import org.jeecg.modules.airag.agent.tool.ToolRegistry;
import org.springframework.stereotype.Component;

/**
 * 角色确认节点。
 *
 * <p>负责展示角色确认选项，并根据前端回传的 optionValue 输出确定性流程动作。</p>
 *
 * @author codex
 * @date 2026/7/14
 */
@Component
public class RoleConfirmationNode extends ConfirmationNode {

    public RoleConfirmationNode(ToolRegistry toolRegistry) {
        super("role_confirmation", "角色确认", RoleTaskToolSpec.ROLE_CONFIRMATION, toolRegistry);
    }
}
