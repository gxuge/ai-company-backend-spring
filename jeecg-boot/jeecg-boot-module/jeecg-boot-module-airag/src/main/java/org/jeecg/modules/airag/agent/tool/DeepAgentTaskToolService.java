package org.jeecg.modules.airag.agent.tool;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.model.chat.request.json.JsonObjectSchema;
import dev.langchain4j.service.tool.ToolExecutor;
import org.jeecg.common.util.oConvertUtils;
import org.jeecg.modules.airag.agent.common.SubAgentHistorySupport;
import org.jeecg.modules.airag.agent.common.SubAgentRegistry;
import org.jeecg.modules.airag.agent.graph.Agent;
import org.jeecg.modules.airag.agent.graph.DeepAgentDefinition;
import org.jeecg.modules.airag.agent.graph.SubAgent;
import org.jeecg.modules.airag.agent.runtime.AgentContext;
import org.jeecg.modules.airag.agent.runtime.AgentResult;
import org.jeecg.modules.airag.agent.runtime.AgentRuntimeService;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * DeepAgents 风格 task 工具。
 *
 * <p>用于让主 Agent 在运行时把任务委托给指定子 Agent，
 * 子 Agent 保持独立上下文与独立 skills。</p>
 */
@Component
public class DeepAgentTaskToolService {
    /**
     * 工具名。
     */
    private static final String TOOL_TASK = "task";
    /**
     * 工具是否启用的上下文标记。
     */
    private static final String CTX_DEEP_AGENTS_MAIN_MODE = "deepAgentsMainMode";
    private static final String ATTR_SESSION_SUB_AGENT_HISTORY_JSON = "sessionSubAgentHistoryJson";
    private static final String ATTR_SUB_AGENT_HISTORY_JSON = "subAgentHistoryJson";
    private static final String ATTR_SUB_AGENT_HISTORY_BLOCK = "subAgentHistoryBlock";
    private static final String SENDER_SUB_AGENT = "sub_agent";

    private final SubAgentRegistry subAgentRegistry;
    private final org.jeecg.modules.airag.agent.graph.DeepAgentDefinitionRegistry deepAgentDefinitionRegistry;
    private final AgentRuntimeService agentRuntimeService;

    public DeepAgentTaskToolService(SubAgentRegistry subAgentRegistry,
                                    org.jeecg.modules.airag.agent.graph.DeepAgentDefinitionRegistry deepAgentDefinitionRegistry,
                                    AgentRuntimeService agentRuntimeService) {
        this.subAgentRegistry = subAgentRegistry;
        this.deepAgentDefinitionRegistry = deepAgentDefinitionRegistry;
        this.agentRuntimeService = agentRuntimeService;
    }

    /**
     * 构建 task 工具。
     *
     * @param context 运行上下文
     * @return 工具 Map
     */
    public Map<ToolSpecification, ToolExecutor> buildToolMap(AgentContext context) {
        Map<ToolSpecification, ToolExecutor> tools = new LinkedHashMap<>();
        if (!isEnabled(context)) {
            return tools;
        }
        tools.put(buildTaskSpec(), buildTaskExecutor(context));
        return tools;
    }

    private boolean isEnabled(AgentContext context) {
        if (context == null) {
            return false;
        }
        Object value = context.getAttribute(CTX_DEEP_AGENTS_MAIN_MODE);
        if (value instanceof Boolean booleanValue) {
            return booleanValue;
        }
        return value != null && Boolean.parseBoolean(String.valueOf(value));
    }

    private ToolSpecification buildTaskSpec() {
        JsonObjectSchema schema = JsonObjectSchema.builder()
                .addStringProperty("subAgentName", "要委托的子 Agent 名称")
                .addStringProperty("taskDescription", "本次需要子 Agent 完成的任务说明")
                .required("subAgentName", "taskDescription")
                .build();
        return ToolSpecification.builder()
                .name(TOOL_TASK)
                .description("把当前任务委托给指定子 Agent 执行")
                .parameters(schema)
                .build();
    }

    private ToolExecutor buildTaskExecutor(AgentContext parentContext) {
        return (toolExecutionRequest, memoryId) -> {
            JSONObject args = parseArgs(toolExecutionRequest == null ? null : toolExecutionRequest.arguments());
            String subAgentName = trimToNull(args == null ? null : args.getString("subAgentName"));
            String taskDescription = trimToNull(args == null ? null : args.getString("taskDescription"));
            if (!StringUtils.hasText(subAgentName)) {
                return "task 调用失败：subAgentName不能为空";
            }
            if (!StringUtils.hasText(taskDescription)) {
                return "task 调用失败：taskDescription不能为空";
            }

            Optional<SubAgent> subAgentOptional = this.subAgentRegistry.find(subAgentName);
            if (subAgentOptional.isEmpty()) {
                return "task 调用失败：未找到子Agent " + subAgentName;
            }

            DeepAgentDefinition definition = this.deepAgentDefinitionRegistry.find(subAgentName).orElse(null);
            AgentContext childContext = buildChildContext(parentContext, args, subAgentName, taskDescription, definition);
            Agent subAgentAdapter = new Agent() {
                @Override
                public String agentName() {
                    return subAgentName;
                }

                @Override
                public AgentResult execute(AgentContext context) {
                    return subAgentOptional.get().execute(context);
                }
            };
            AgentResult result = this.agentRuntimeService.execute(subAgentAdapter, childContext);
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("subAgentName", subAgentName);
            payload.put("taskDescription", taskDescription);
            payload.put("definition", definition == null ? null : definition.toMap());
            payload.put("childRunId", childContext.getRunId());
            payload.put("traceId", childContext.getTraceId());
            payload.put("parentRunId", childContext.getParentRunId());
            payload.put("resultStatus", result == null ? null : result.getStatus());
            payload.put("result", result);
            String summary = result == null ? "" : oConvertUtils.getString(result.getContent());
            if (!StringUtils.hasText(summary) && result != null && result.getStructuredResult() != null) {
                summary = JSON.toJSONString(result.getStructuredResult());
            }
            if (!StringUtils.hasText(summary)) {
                summary = "子Agent已执行完成";
            }
            ToolCallResult toolResult = ToolCallResult.success(summary, result);
            toolResult.setPayload(payload);
            return toolResult.getSummary();
        };
    }

    private AgentContext buildChildContext(AgentContext parentContext,
                                           JSONObject args,
                                           String subAgentName,
                                           String taskDescription,
                                           DeepAgentDefinition definition) {
        AgentContext childContext = parentContext == null ? new AgentContext() : parentContext.fork(taskDescription);
        childContext.setAgentCode(subAgentName);
        childContext.setSenderType(SENDER_SUB_AGENT);
        if (parentContext != null && StringUtils.hasText(parentContext.getRunId())) {
            childContext.setParentRunId(parentContext.getRunId());
        }
        childContext.putAttribute(CTX_DEEP_AGENTS_MAIN_MODE, Boolean.FALSE);
        childContext.putAttribute("deepAgentsPromptMode", Boolean.TRUE);
        childContext.putAttribute("taskSubAgentName", subAgentName);
        childContext.putAttribute("taskDescription", taskDescription);
        injectSubAgentHistory(parentContext, childContext, subAgentName);
        Map<String, Object> promptVariables = new LinkedHashMap<>();
        Object rawPromptVariables = parentContext == null ? null : parentContext.getAttribute("promptVariables");
        if (rawPromptVariables instanceof Map<?, ?> rawMap) {
            for (Map.Entry<?, ?> entry : rawMap.entrySet()) {
                if (entry.getKey() == null) {
                    continue;
                }
                promptVariables.put(String.valueOf(entry.getKey()), entry.getValue());
            }
        }
        promptVariables.put("user_input", taskDescription);
        promptVariables.put("task_description", taskDescription);
        promptVariables.put("sub_agent_name", subAgentName);
        Object historyBlock = childContext.getAttribute(ATTR_SUB_AGENT_HISTORY_BLOCK);
        if (historyBlock != null) {
            promptVariables.put("sub_agent_history_block", historyBlock);
        }
        childContext.putAttribute("promptVariables", promptVariables);
        if (args != null) {
            childContext.putAttribute("taskArguments", args);
        }
        if (definition != null) {
            childContext.putAttribute("subAgentDefinition", definition.toMap());
            childContext.putAttribute("subAgentSkills", definition.getSkills());
            childContext.putAttribute("subAgentTools", definition.getTools());
            childContext.putAttribute("subAgentPermissions", definition.getPermissions());
            childContext.putAttribute("subAgentResponseFormat", definition.getResponseFormat());
            childContext.putAttribute("skillDomain", definition.getSkillDomain());
            childContext.putAttribute("skillTopK", definition.getSkillTopK());
        }
        return childContext;
    }

    private void injectSubAgentHistory(AgentContext parentContext, AgentContext childContext, String subAgentName) {
        if (childContext == null || !StringUtils.hasText(subAgentName)) {
            return;
        }
        String sessionHistoryJson = null;
        if (parentContext != null) {
            Object fullHistory = parentContext.getAttribute(ATTR_SESSION_SUB_AGENT_HISTORY_JSON);
            if (fullHistory == null) {
                fullHistory = parentContext.getAttribute(ATTR_SUB_AGENT_HISTORY_JSON);
            }
            if (fullHistory != null) {
                sessionHistoryJson = String.valueOf(fullHistory);
            }
        }
        String selectedHistoryJson = SubAgentHistorySupport.selectHistoryJson(sessionHistoryJson, subAgentName);
        childContext.putAttribute(ATTR_SESSION_SUB_AGENT_HISTORY_JSON, sessionHistoryJson);
        childContext.putAttribute(ATTR_SUB_AGENT_HISTORY_JSON, selectedHistoryJson);
        childContext.putAttribute(ATTR_SUB_AGENT_HISTORY_BLOCK, SubAgentHistorySupport.buildHistoryBlock(selectedHistoryJson));
    }

    private JSONObject parseArgs(String arguments) {
        if (!StringUtils.hasText(arguments)) {
            return new JSONObject();
        }
        try {
            return JSON.parseObject(arguments);
        } catch (Exception ex) {
            return new JSONObject();
        }
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String text = value.trim();
        return text.isEmpty() ? null : text;
    }
}
