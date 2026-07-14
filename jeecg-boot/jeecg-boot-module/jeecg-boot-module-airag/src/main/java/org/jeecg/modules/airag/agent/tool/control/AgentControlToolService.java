package org.jeecg.modules.airag.agent.tool.control;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.model.chat.request.json.JsonObjectSchema;
import dev.langchain4j.service.tool.ToolExecutor;
import org.jeecg.common.util.oConvertUtils;
import org.jeecg.modules.airag.agent.runtime.AgentContext;
import org.jeecg.modules.airag.agent.runtime.AgentHandoffSupport;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Agent 公共控制工具。
 *
 * <p>自动注入子 Agent LLM 节点，用于交还主 Agent 等控制动作。</p>
 *
 * @author codex
 * @date 2026/7/13
 */
@Component
public class AgentControlToolService {
    /**
     * 交还主 Agent 工具名。
     */
    public static final String TOOL_HANDOFF_TO_MAIN = "handoff_to_main";
    private static final String SENDER_SUB_AGENT = "sub_agent";

    /**
     * 构建控制工具。
     *
     * @param context 运行上下文
     * @return 工具 Map
     */
    public Map<ToolSpecification, ToolExecutor> buildToolMap(AgentContext context) {
        Map<ToolSpecification, ToolExecutor> tools = new LinkedHashMap<>();
        if (!isEnabled(context)) {
            return tools;
        }
        tools.put(buildHandoffSpec(), buildHandoffExecutor(context));
        return tools;
    }

    /**
     * 是否应为当前上下文启用控制工具。
     *
     * @param context 运行上下文
     * @return 是否启用
     */
    public boolean isEnabled(AgentContext context) {
        if (context == null) {
            return false;
        }
        return SENDER_SUB_AGENT.equalsIgnoreCase(oConvertUtils.getString(context.getSenderType()));
    }

    /**
     * 构建公共控制提示。
     *
     * @param context 运行上下文
     * @return 提示文本
     */
    public String buildControlPrompt(AgentContext context) {
        if (!isEnabled(context)) {
            return "";
        }
        return """
                ## 子 Agent 控制规则
                如果用户当前请求明显超出本子 Agent 的职责范围，不要强行处理。
                此时必须调用 handoff_to_main，给出 reason、userRequest、progressSummary，并可选 suggestedAgent。
                handoff_to_main 表示本子 Agent 正式结束本轮处理，并把任务交还主 Agent 重新派活。
                """;
    }

    private ToolSpecification buildHandoffSpec() {
        JsonObjectSchema schema = JsonObjectSchema.builder()
                .addStringProperty("reason", "为什么当前请求不应由本子Agent继续处理")
                .addStringProperty("userRequest", "需要交还主Agent重新判断的用户原始请求")
                .addStringProperty("progressSummary", "本子Agent已执行进度和已有结果摘要")
                .addStringProperty("stage", "当前子Agent所处阶段")
                .addStringProperty("suggestedAgent", "可选，建议主Agent重新委托的子Agent名称")
                .required("reason", "userRequest", "progressSummary")
                .build();
        return ToolSpecification.builder()
                .name(TOOL_HANDOFF_TO_MAIN)
                .description("当用户请求明显超出当前子Agent职责时，正式交还主Agent重新派活")
                .parameters(schema)
                .build();
    }

    private ToolExecutor buildHandoffExecutor(AgentContext context) {
        return (toolExecutionRequest, memoryId) -> {
            JSONObject args = parseArgs(toolExecutionRequest == null ? null : toolExecutionRequest.arguments());
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("action", AgentHandoffSupport.ACTION_HANDOFF_TO_MAIN);
            payload.put("status", "HANDOFF");
            payload.put("targetAgentCode", AgentHandoffSupport.MAIN_AGENT_CODE);
            payload.put("subAgentName", oConvertUtils.getString(context == null ? null : context.getAgentCode()));
            payload.put("stage", firstText(args.getString("stage"), currentStage(context)));
            payload.put("reason", args.getString("reason"));
            payload.put("userRequest", firstText(args.getString("userRequest"), context == null ? null : context.getUserInput()));
            payload.put("progressSummary", args.getString("progressSummary"));
            payload.put("suggestedAgent", args.getString("suggestedAgent"));
            if (context != null) {
                context.putAttribute(AgentHandoffSupport.ATTR_HANDOFF_TO_MAIN, payload);
            }
            return JSON.toJSONString(payload);
        };
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

    private String currentStage(AgentContext context) {
        if (context == null) {
            return null;
        }
        String roleStage = oConvertUtils.getString(context.getAttribute("roleTaskStage"));
        if (StringUtils.hasText(roleStage)) {
            return roleStage;
        }
        String storyStage = oConvertUtils.getString(context.getAttribute("storyTaskStage"));
        if (StringUtils.hasText(storyStage)) {
            return storyStage;
        }
        return oConvertUtils.getString(context.getAttribute("taskStage"));
    }

    private String firstText(String first, String second) {
        if (StringUtils.hasText(first)) {
            return first;
        }
        return second;
    }
}
