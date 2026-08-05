package org.jeecg.modules.airag.agent.tool.options;

import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.model.chat.request.json.JsonArraySchema;
import dev.langchain4j.model.chat.request.json.JsonObjectSchema;
import dev.langchain4j.model.chat.request.json.JsonStringSchema;
import jakarta.annotation.PostConstruct;
import org.jeecg.common.util.oConvertUtils;
import org.jeecg.modules.airag.agent.error.AgentErrorCode;
import org.jeecg.modules.airag.agent.error.AgentErrorException;
import org.jeecg.modules.airag.agent.interaction.UserInteractionSupport;
import org.jeecg.modules.airag.agent.runtime.AgentContext;
import org.jeecg.modules.airag.agent.tool.ToolCallRequest;
import org.jeecg.modules.airag.agent.tool.ToolCallResult;
import org.jeecg.modules.airag.agent.tool.ToolDefinition;
import org.jeecg.modules.airag.agent.tool.ToolRegistry;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Agent 通用候选项工具。
 */
@Component
public class AgentOptionsToolService {
    public static final String TOOL_NAME = "agent_request_options";
    public static final String INTERACTION_TYPE = "options";

    private static final int MIN_OPTIONS = 2;
    private static final int MAX_OPTIONS = 4;
    private static final int MAX_QUESTION_LENGTH = 40;
    private static final int MAX_OPTION_LENGTH = 24;
    private static final String ROUTE_KEY = "AGENT_REQUEST_OPTIONS";
    private static final String COPY_STYLE =
            "表达应简短、自然、可爱且舒适，可以适当使用贴合语义的符号或表情，避免堆叠或过长。";
    private static final String TOOL_DESCRIPTION =
            "向用户展示一组可点击的候选回复并等待选择。候选文案应简短、自然、可爱且舒适，"
                    + "可以适当使用符合语义的符号或表情。调用本工具时不得同时调用其他工具；"
                    + "调用后停止继续输出，等待用户选择候选项或自由输入。";
    private static final String INPUT_SCHEMA = """
            {
              "type": "object",
              "properties": {
                "question": {
                  "type": "string",
                  "maxLength": 40,
                  "description": "展示给用户的简短问题或提示语。表达应自然、亲切、舒适，可以适当使用符合语义的符号或表情。"
                },
                "options": {
                  "type": "array",
                  "minItems": 2,
                  "maxItems": 4,
                  "description": "提供给用户点击的2至4条候选回复。选项应简短、明确、不重复，像用户自然说出的话，语气可爱、轻松且舒适。",
                  "items": {
                    "type": "string",
                    "maxLength": 24,
                    "description": "用户可以直接发送的一条简短候选回复，可适当使用贴合语义的符号或表情。"
                  }
                }
              },
              "required": ["question", "options"],
              "additionalProperties": false
            }
            """;

    private final ToolRegistry toolRegistry;

    public AgentOptionsToolService(ToolRegistry toolRegistry) {
        this.toolRegistry = toolRegistry;
    }

    @PostConstruct
    void registerTool() {
        ToolDefinition definition = new ToolDefinition();
        definition.setName(TOOL_NAME);
        definition.setDisplayName("候选回复");
        definition.setDescription(TOOL_DESCRIPTION);
        definition.setRouteKey(ROUTE_KEY);
        definition.setCategory("agent_interaction");
        definition.setContentType(INTERACTION_TYPE);
        definition.setInputSchema(INPUT_SCHEMA);
        definition.setRetryable(false);
        definition.setExecutor(this::requestOptions);
        this.toolRegistry.register(definition);
    }

    /**
     * 构建注入模型的工具定义。
     */
    public ToolSpecification buildSpecification() {
        JsonArraySchema optionsSchema = JsonArraySchema.builder()
                .description("提供给用户点击的2至4条候选回复。选项应简短、明确、不重复，像用户自然说出的话。"
                        + COPY_STYLE)
                .items(JsonStringSchema.builder()
                        .description("用户可以直接发送的一条简短候选回复。" + COPY_STYLE)
                        .build())
                .build();
        JsonObjectSchema schema = JsonObjectSchema.builder()
                .addStringProperty("question", "展示给用户的简短问题或提示语。" + COPY_STYLE)
                .addProperty("options", optionsSchema)
                .required("question", "options")
                .additionalProperties(false)
                .build();
        return ToolSpecification.builder()
                .name(TOOL_NAME)
                .description(TOOL_DESCRIPTION)
                .parameters(schema)
                .build();
    }

    private ToolCallResult requestOptions(AgentContext context, ToolCallRequest request) {
        if (UserInteractionSupport.hasPending(context)) {
            throw invalid("pendingInteraction", "A user interaction is already pending");
        }
        Map<String, Object> arguments = request == null || request.getArguments() == null
                ? Map.of()
                : request.getArguments();
        String question = requireText(arguments.get("question"), "question", MAX_QUESTION_LENGTH);
        List<String> labels = requireOptions(arguments.get("options"));
        List<Map<String, String>> options = new ArrayList<>();
        for (int index = 0; index < labels.size(); index++) {
            Map<String, String> option = new LinkedHashMap<>();
            option.put("label", labels.get(index));
            option.put("optionValue", "candidate_" + (index + 1));
            options.add(option);
        }
        String sourceNode = context == null ? null : context.getCurrentNodeName();
        Map<String, Object> interaction = UserInteractionSupport.createPending(
                context,
                INTERACTION_TYPE,
                TOOL_NAME,
                sourceNode,
                sourceNode,
                question,
                null,
                options
        );
        ToolCallResult result = ToolCallResult.success(question, interaction);
        result.setContentType(INTERACTION_TYPE);
        result.setPayload(new LinkedHashMap<>(interaction));
        return result;
    }

    private List<String> requireOptions(Object rawOptions) {
        if (!(rawOptions instanceof Iterable<?> iterable)) {
            throw invalid("options", "Options must be an array");
        }
        List<String> options = new ArrayList<>();
        Set<String> unique = new HashSet<>();
        for (Object item : iterable) {
            String option = requireText(item, "options", MAX_OPTION_LENGTH);
            if (!unique.add(option)) {
                throw invalid("options", "Options must not contain duplicates");
            }
            options.add(option);
        }
        if (options.size() < MIN_OPTIONS || options.size() > MAX_OPTIONS) {
            throw invalid("options", "Options must contain between 2 and 4 items");
        }
        return options;
    }

    private String requireText(Object rawValue, String field, int maxLength) {
        String value = oConvertUtils.getString(rawValue);
        if (value == null || value.isBlank()) {
            throw invalid(field, "Field is required");
        }
        String normalized = value.trim();
        if (normalized.codePointCount(0, normalized.length()) > maxLength) {
            throw invalid(field, "Field is too long");
        }
        return normalized;
    }

    private AgentErrorException invalid(String field, String reason) {
        return new AgentErrorException(
                AgentErrorCode.TOOL_COMMON_REQUEST_INVALID,
                Map.of("field", field, "reason", reason)
        );
    }
}
