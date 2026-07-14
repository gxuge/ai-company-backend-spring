package org.jeecg.modules.airag.agent.tool;

import com.alibaba.fastjson2.JSONObject;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.model.chat.request.json.JsonObjectSchema;
import dev.langchain4j.service.tool.ToolExecutor;
import org.jeecg.modules.airag.agent.common.SubAgentRegistry;
import org.jeecg.modules.airag.agent.runtime.AgentContext;
import org.jeecg.modules.airag.agent.runtime.AgentResult;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * DeepAgents 风格 task 工具。
 *
 * <p>用于让主 Agent 在运行时把任务委托给指定子 Agent，
 * 子 Agent 保持独立职责配置、历史与 skills。</p>
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
    private static final String ATTR_TASK_ALREADY_CALLED = "deepAgentsTaskAlreadyCalled";
    private static final String ATTR_PENDING_TASK_ARGS = "deepAgentsPendingTaskArgs";

    private final SubAgentRegistry subAgentRegistry;

    public DeepAgentTaskToolService(SubAgentRegistry subAgentRegistry) {
        this.subAgentRegistry = subAgentRegistry;
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
            if (isTaskAlreadyCalled(parentContext)) {
                return "本轮已委托过一个子Agent，不再重复委托；请基于已有子Agent结果回复用户。";
            }
            markTaskCalled(parentContext);
            JSONObject args = parseArgs(toolExecutionRequest == null ? null : toolExecutionRequest.arguments());
            String subAgentName = trimToNull(args == null ? null : args.getString("subAgentName"));
            String taskDescription = trimToNull(args == null ? null : args.getString("taskDescription"));
            if (!StringUtils.hasText(subAgentName)) {
                return "task 调用失败：subAgentName不能为空";
            }
            if (!StringUtils.hasText(taskDescription)) {
                return "task 调用失败：taskDescription不能为空";
            }

            if (parentContext != null) {
                parentContext.putAttribute(ATTR_PENDING_TASK_ARGS, args);
            }
            return "task 已登记，将在当前主 Agent LLM 节点结束后执行；不要再输出面向用户的委托说明。";
        };
    }

    /**
     * 消费已登记的 task，并生成切换到子 Agent 的 Handoff 结果。
     *
     * @param parentContext 当前顶层运行上下文
     * @return Handoff 结果；没有待执行任务时返回 null
     */
    public AgentResult consumePendingHandoff(AgentContext parentContext) {
        JSONObject args = readPendingTaskArgs(parentContext);
        if (args == null || args.isEmpty()) {
            return null;
        }
        String subAgentName = trimToNull(args.getString("subAgentName"));
        String taskDescription = trimToNull(args.getString("taskDescription"));
        if (!StringUtils.hasText(subAgentName) || !StringUtils.hasText(taskDescription)) {
            return AgentResult.failed("task 调用失败：subAgentName/taskDescription不能为空");
        }
        if (!this.subAgentRegistry.exists(subAgentName)) {
            return AgentResult.failed("task 调用失败：未找到子Agent " + subAgentName);
        }

        AgentResult result = AgentResult.handoffTo(subAgentName, taskDescription);
        result.setContent("正在转交子Agent处理");
        result.setStructuredResult(args);
        result.getData().put("action", "HANDOFF_TO_AGENT");
        result.getData().put("status", "HANDOFF");
        result.getData().put("sourceAgentCode", "main");
        result.getData().put("targetAgentCode", subAgentName);
        result.getData().put("targetSubAgent", subAgentName);
        result.getData().put("subAgentName", subAgentName);
        result.getData().put("taskDescription", taskDescription);
        return result;
    }

    private JSONObject readPendingTaskArgs(AgentContext context) {
        Object value = context == null ? null : context.getAttribute(ATTR_PENDING_TASK_ARGS);
        if (value instanceof JSONObject jsonObject) {
            return jsonObject;
        }
        if (value instanceof Map<?, ?> map) {
            JSONObject jsonObject = new JSONObject();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (entry.getKey() != null) {
                    jsonObject.put(String.valueOf(entry.getKey()), entry.getValue());
                }
            }
            return jsonObject;
        }
        if (value != null) {
            return parseArgs(String.valueOf(value));
        }
        return null;
    }

    private boolean isTaskAlreadyCalled(AgentContext context) {
        if (context == null) {
            return false;
        }
        Object value = context.getAttribute(ATTR_TASK_ALREADY_CALLED);
        if (value instanceof Boolean booleanValue) {
            return booleanValue;
        }
        return value != null && Boolean.parseBoolean(String.valueOf(value));
    }

    private void markTaskCalled(AgentContext context) {
        if (context != null) {
            context.putAttribute(ATTR_TASK_ALREADY_CALLED, Boolean.TRUE);
        }
    }

    private JSONObject parseArgs(String arguments) {
        if (!StringUtils.hasText(arguments)) {
            return new JSONObject();
        }
        try {
            return JSONObject.parseObject(arguments);
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
