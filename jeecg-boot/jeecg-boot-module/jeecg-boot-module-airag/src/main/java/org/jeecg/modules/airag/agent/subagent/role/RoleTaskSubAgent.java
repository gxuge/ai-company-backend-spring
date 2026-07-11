package org.jeecg.modules.airag.agent.subagent.role;

import org.jeecg.modules.airag.agent.graph.SubAgent;
import org.jeecg.modules.airag.agent.runtime.AgentContext;
import org.jeecg.modules.airag.agent.runtime.AgentResult;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 角色子 Agent 骨架。
 *
 * <p>当前只占位注册，不接入具体角色生成实现，后续会在这里补齐
 * role_create_dialog / role_flow_gate / role_create_image / role_create_voice 的实际执行链路。</p>
 *
 * @author codex
 * @date 2026/7/10
 */
@Component
public class RoleTaskSubAgent implements SubAgent {

    @Override
    public String subAgentName() {
        return RoleTaskChainSpec.SUB_AGENT_NAME;
    }

    @Override
    public AgentResult execute(AgentContext context) {
        AgentResult result = AgentResult.waitingUser("角色子 Agent 骨架已接入，后续在此补齐创建角色、形象和声音的完整链路。");
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("subAgentName", subAgentName());
        data.put("skills", RoleTaskChainSpec.SKILLS);
        data.put("tools", RoleTaskChainSpec.TOOLS);
        data.put("chain", RoleTaskChainSpec.CHAIN);
        data.put("stage", "skeleton");
        result.getData().putAll(data);
        if (context != null) {
            context.putAttribute("roleTaskSkeleton", Boolean.TRUE);
            context.putAttribute("roleTaskChainSpec", data);
        }
        return result;
    }
}
