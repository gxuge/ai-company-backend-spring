package org.jeecg.modules.airag.agent.runtime;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.util.oConvertUtils;
import org.jeecg.modules.airag.agent.common.SubAgentRegistry;
import org.jeecg.modules.airag.agent.graph.Agent;
import org.jeecg.modules.airag.agent.main.TsAgentChatAgent;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * 顶层运行循环使用的 Agent 注册表。
 *
 * @author codex
 * @date 2026/7/14
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AgentRegistry {
    /**
     * 会话持久化使用的主 Agent 编码。
     */
    public static final String MAIN_AGENT_CODE = "main";

    /**
     * 主 Agent。
     */
    private final TsAgentChatAgent mainAgent;
    /**
     * 子 Agent 注册表。
     */
    private final SubAgentRegistry subAgentRegistry;

    /**
     * 查找目标 Agent。
     *
     * @param agentCode Agent 编码
     * @return Agent
     */
    public Optional<Agent> find(String agentCode) {
        String normalized = normalizeCode(agentCode);
        if (isMainAgentCode(normalized)) {
            return Optional.of(this.mainAgent);
        }
        return this.subAgentRegistry.find(normalized).map(subAgent -> (Agent) subAgent);
    }

    /**
     * 判断 Agent 是否存在。
     *
     * @param agentCode Agent 编码
     * @return 是否存在
     */
    public boolean exists(String agentCode) {
        return find(agentCode).isPresent();
    }

    /**
     * 规范化一次 Run 的起始 Agent。
     *
     * <p>历史会话为空或 Agent 已下线时回退主 Agent。</p>
     *
     * @param agentCode 持久化 Agent 编码
     * @return 可用 Agent 编码
     */
    public String normalizeStartingAgentCode(String agentCode) {
        String normalized = normalizeCode(agentCode);
        if (isMainAgentCode(normalized)) {
            return MAIN_AGENT_CODE;
        }
        if (this.subAgentRegistry.exists(normalized)) {
            return normalized;
        }
        if (oConvertUtils.isNotEmpty(normalized)) {
            log.warn("会话active Agent不存在，回退主Agent，agentCode={}", normalized);
        }
        return MAIN_AGENT_CODE;
    }

    /**
     * 判断是否为主 Agent 编码或兼容名称。
     *
     * @param agentCode Agent 编码
     * @return 是否主 Agent
     */
    public boolean isMainAgentCode(String agentCode) {
        String normalized = normalizeCode(agentCode);
        return oConvertUtils.isEmpty(normalized)
                || MAIN_AGENT_CODE.equalsIgnoreCase(normalized)
                || this.mainAgent.agentName().equalsIgnoreCase(normalized);
    }

    /**
     * 规范化 Agent 编码。
     *
     * @param agentCode Agent 编码
     * @return 规范化结果
     */
    public String normalizeCode(String agentCode) {
        if (agentCode == null) {
            return "";
        }
        return agentCode.trim();
    }
}
