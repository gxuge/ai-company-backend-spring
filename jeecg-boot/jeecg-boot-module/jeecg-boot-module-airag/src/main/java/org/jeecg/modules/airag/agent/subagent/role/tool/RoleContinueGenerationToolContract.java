package org.jeecg.modules.airag.agent.subagent.role.tool;

import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.model.chat.request.json.JsonObjectSchema;
import org.jeecg.common.util.oConvertUtils;
import org.jeecg.modules.airag.agent.runtime.AgentContext;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 角色确认后继续生成工具契约。
 *
 * <p>负责接收最终角色四字段，并显式请求进入形象和声音节点。</p>
 *
 * @author codex
 * @date 2026/7/17
 */
public final class RoleContinueGenerationToolContract {
    public static final String TRANSFER_DATA = "transferData";
    public static final String TRANSFER_DATA_JSON = "transferDataJson";
    public static final String ATTR_CONTINUE_REQUESTED = "roleContinueGenerationRequested";

    private static final List<String> TRANSFER_FIELDS = List.of(
            "roleName",
            "gender",
            "occupation",
            "backgroundStory"
    );
    private static final String TOOL_DESCRIPTION =
            "只有角色名称、性别、职业、角色背景四项内容全部完整，"
                    + "并且用户明确表示满意且同意继续生成角色形象和声音时才调用。"
                    + "调用后将强制进入角色形象和声音生成流程。";
    private static final String INPUT_SCHEMA = """
            {
              "type": "object",
              "properties": {
                "transferData": {
                  "type": "object",
                  "description": "传递给后续角色形象和声音节点的最终角色核心数据。",
                  "properties": {
                    "roleName": {"type": "string", "description": "最终角色名称。"},
                    "gender": {"type": "string", "description": "最终角色性别。"},
                    "occupation": {"type": "string", "description": "最终角色职业或身份。"},
                    "backgroundStory": {"type": "string", "description": "最终角色背景故事。"}
                  },
                  "required": ["roleName", "gender", "occupation", "backgroundStory"],
                  "additionalProperties": false
                }
              },
              "required": ["transferData"],
              "additionalProperties": false
            }
            """;

    private RoleContinueGenerationToolContract() {
    }

    /**
     * 构建注入模型的继续生成工具定义。
     */
    public static ToolSpecification buildSpecification() {
        JsonObjectSchema transferDataSchema = JsonObjectSchema.builder()
                .description("传递给后续角色形象和声音节点的最终角色核心数据")
                .addStringProperty("roleName", "最终角色名称")
                .addStringProperty("gender", "最终角色性别")
                .addStringProperty("occupation", "最终角色职业或身份")
                .addStringProperty("backgroundStory", "最终角色背景故事")
                .required(TRANSFER_FIELDS)
                .additionalProperties(false)
                .build();
        JsonObjectSchema schema = JsonObjectSchema.builder()
                .addProperty(TRANSFER_DATA, transferDataSchema)
                .required(TRANSFER_DATA)
                .additionalProperties(false)
                .build();
        return ToolSpecification.builder()
                .name(RoleTaskToolSpec.ROLE_CONTINUE_GENERATION)
                .description(TOOL_DESCRIPTION)
                .parameters(schema)
                .build();
    }

    /**
     * 返回工具注册中心使用的 JSON Schema。
     */
    public static String inputSchema() {
        return INPUT_SCHEMA;
    }

    /**
     * 提取并校验四项角色核心字段。
     */
    public static Map<String, Object> requireTransferData(Map<String, Object> arguments) {
        Object rawTransferData = arguments == null ? null : arguments.get(TRANSFER_DATA);
        if (!(rawTransferData instanceof Map<?, ?> rawMap)) {
            throw new IllegalArgumentException("transferData 必须是包含角色核心字段的对象");
        }
        Map<String, Object> transferData = new LinkedHashMap<>();
        for (String field : TRANSFER_FIELDS) {
            String value = normalize(rawMap.get(field));
            if (!oConvertUtils.isNotEmpty(value)) {
                throw new IllegalArgumentException("transferData." + field + " 不能为空");
            }
            transferData.put(field, value);
        }
        return transferData;
    }

    /**
     * 标记当前对话节点已显式请求继续生成。
     */
    public static void markContinueRequested(AgentContext context) {
        if (context != null) {
            context.putAttribute(ATTR_CONTINUE_REQUESTED, Boolean.TRUE);
        }
    }

    /**
     * 消费继续生成标记。
     */
    public static boolean consumeContinueRequested(AgentContext context) {
        if (context == null || !Boolean.TRUE.equals(context.getAttribute(ATTR_CONTINUE_REQUESTED))) {
            return false;
        }
        context.removeAttribute(ATTR_CONTINUE_REQUESTED);
        return true;
    }

    private static String normalize(Object value) {
        String text = oConvertUtils.getString(value);
        return text == null || text.isBlank() ? null : text.trim();
    }
}
