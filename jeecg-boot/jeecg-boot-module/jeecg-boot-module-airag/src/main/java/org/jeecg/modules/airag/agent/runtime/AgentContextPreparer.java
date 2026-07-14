package org.jeecg.modules.airag.agent.runtime;

import org.jeecg.common.util.oConvertUtils;
import org.jeecg.modules.airag.agent.common.SubAgentHistorySupport;
import org.jeecg.modules.airag.agent.graph.DeepAgentDefinition;
import org.jeecg.modules.airag.agent.graph.DeepAgentDefinitionRegistry;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 根据当前 active Agent 准备可复用的运行上下文。
 *
 * @author codex
 * @date 2026/7/14
 */
@Component
public class AgentContextPreparer {
    private static final String SENDER_MAIN_AGENT = "main_agent";
    private static final String SENDER_SUB_AGENT = "sub_agent";
    private static final String ATTR_SESSION_SUB_AGENT_HISTORY_JSON = "sessionSubAgentHistoryJson";
    private static final String ATTR_SUB_AGENT_HISTORY_JSON = "subAgentHistoryJson";

    private final DeepAgentDefinitionRegistry deepAgentDefinitionRegistry;

    public AgentContextPreparer(DeepAgentDefinitionRegistry deepAgentDefinitionRegistry) {
        this.deepAgentDefinitionRegistry = deepAgentDefinitionRegistry;
    }

    /**
     * 准备当前 Agent 执行上下文。
     *
     * @param context 运行上下文
     * @param agentCode 当前 Agent 编码
     */
    public void prepare(AgentContext context, String agentCode) {
        if (context == null) {
            return;
        }
        clearStepControlState(context);
        if (AgentRegistry.MAIN_AGENT_CODE.equalsIgnoreCase(agentCode)) {
            prepareMainAgent(context);
            return;
        }
        prepareSubAgent(context, agentCode);
    }

    /**
     * 将 Handoff 数据应用到同一个顶层运行上下文。
     *
     * @param context 运行上下文
     * @param result Handoff 结果
     */
    public void applyHandoff(AgentContext context, AgentResult result) {
        if (context == null || result == null) {
            return;
        }
        AgentFlowStateSupport.clear(context);
        if (!AgentRegistry.MAIN_AGENT_CODE.equalsIgnoreCase(result.getHandoffTargetAgentCode())) {
            context.removeAttribute("handoffReport");
        }
        if (StringUtils.hasText(result.getHandoffInput())) {
            context.setUserInput(result.getHandoffInput());
        }
        if (result.getHandoffContext() != null) {
            result.getHandoffContext().forEach(context::putAttribute);
        }
    }

    private void prepareMainAgent(AgentContext context) {
        clearSubAgentConfiguration(context);
        context.setAgentCode(AgentRegistry.MAIN_AGENT_CODE);
        context.setSenderType(SENDER_MAIN_AGENT);
        context.putAttribute("deepAgentsPromptMode", Boolean.TRUE);
        context.putAttribute("deepAgentsMainMode", Boolean.TRUE);
        updatePromptVariables(context);
    }

    private void prepareSubAgent(AgentContext context, String agentCode) {
        clearSubAgentConfiguration(context);
        context.setAgentCode(agentCode);
        context.setSenderType(SENDER_SUB_AGENT);
        context.putAttribute("deepAgentsPromptMode", Boolean.TRUE);
        context.putAttribute("deepAgentsMainMode", Boolean.FALSE);
        context.putAttribute("taskSubAgentName", agentCode);
        context.putAttribute("taskDescription", context.getUserInput());
        injectSubAgentHistoryJson(context, agentCode);

        DeepAgentDefinition definition = this.deepAgentDefinitionRegistry.find(agentCode).orElse(null);
        if (definition != null) {
            context.putAttribute("subAgentDefinition", definition.toMap());
            context.putAttribute("subAgentSkills", definition.getSkills());
            context.putAttribute("subAgentTools", definition.getTools());
            context.putAttribute("subAgentPermissions", definition.getPermissions());
            context.putAttribute("subAgentResponseFormat", definition.getResponseFormat());
            context.putAttribute("skillDomain", definition.getSkillDomain());
            context.putAttribute("skillTopK", definition.getSkillTopK());
        }
        updatePromptVariables(context);
    }

    private void clearSubAgentConfiguration(AgentContext context) {
        context.removeAttribute("taskSubAgentName");
        context.removeAttribute("taskDescription");
        context.removeAttribute("subAgentDefinition");
        context.removeAttribute("subAgentSkills");
        context.removeAttribute("subAgentTools");
        context.removeAttribute("subAgentPermissions");
        context.removeAttribute("subAgentResponseFormat");
        context.removeAttribute("skillDomain");
        context.removeAttribute("skillTopK");
        context.removeAttribute("nodeSkillPrompt");
        context.removeAttribute("loadedNodeSkillCodes");
    }

    private void injectSubAgentHistoryJson(AgentContext context, String agentCode) {
        Object fullHistory = context.getAttribute(ATTR_SESSION_SUB_AGENT_HISTORY_JSON);
        if (fullHistory == null) {
            fullHistory = context.getAttribute(ATTR_SUB_AGENT_HISTORY_JSON);
        }
        String sessionHistoryJson = fullHistory == null ? null : String.valueOf(fullHistory);
        String selectedHistoryJson = SubAgentHistorySupport.selectHistoryJson(sessionHistoryJson, agentCode);
        context.putAttribute(ATTR_SESSION_SUB_AGENT_HISTORY_JSON, sessionHistoryJson);
        context.putAttribute(ATTR_SUB_AGENT_HISTORY_JSON, selectedHistoryJson);
    }

    private void updatePromptVariables(AgentContext context) {
        Map<String, Object> variables = new LinkedHashMap<>();
        Object rawVariables = context.getAttribute("promptVariables");
        if (rawVariables instanceof Map<?, ?> rawMap) {
            for (Map.Entry<?, ?> entry : rawMap.entrySet()) {
                if (entry.getKey() != null) {
                    variables.put(String.valueOf(entry.getKey()), entry.getValue());
                }
            }
        }
        variables.put("user_input", oConvertUtils.getString(context.getUserInput()));
        context.putAttribute("promptVariables", variables);
    }

    private void clearStepControlState(AgentContext context) {
        context.resetNodeSource();
        context.removeAttribute(AgentHandoffSupport.ATTR_HANDOFF_TO_MAIN);
        context.removeAttribute("deepAgentsTaskAlreadyCalled");
        context.removeAttribute("deepAgentsPendingTaskArgs");
    }
}
