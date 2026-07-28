package org.jeecg.modules.airag.agent.subagent.story.tool;

import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.model.chat.request.json.JsonObjectSchema;
import org.jeecg.common.util.oConvertUtils;
import org.jeecg.modules.airag.agent.runtime.AgentContext;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 故事确认后继续生成工具契约。
 *
 * <p>负责接收最终故事六字段，并显式请求进入背景与场景节点。</p>
 *
 * @author codex
 * @date 2026/7/28
 */
public final class StoryContinueGenerationToolContract {
    public static final String TRANSFER_DATA = "transferData";
    public static final String TRANSFER_DATA_JSON = "transferDataJson";
    public static final String ATTR_CONTINUE_REQUESTED = "storyContinueGenerationRequested";

    private static final List<String> TRANSFER_FIELDS = List.of(
            "title",
            "storyMode",
            "storyIntro",
            "storySetting",
            "siteSetting",
            "plotOutline"
    );
    private static final String TOOL_DESCRIPTION =
            "只有故事标题、模式、简介、设定、地点设定和剧情大纲六项内容全部完整，"
                    + "并且用户明确表示满意且同意继续生成故事背景与场景时才调用。"
                    + "调用后将强制进入故事背景与场景生成流程。";
    private static final String INPUT_SCHEMA = """
            {
              "type": "object",
              "properties": {
                "transferData": {
                  "type": "object",
                  "description": "传递给后续故事背景与场景节点的最终故事核心数据。",
                  "properties": {
                    "title": {"type": "string", "description": "最终故事标题。"},
                    "storyMode": {"type": "string", "description": "最终故事模式，使用 normal 或 chapter。"},
                    "storyIntro": {"type": "string", "description": "最终故事简介。"},
                    "storySetting": {"type": "string", "description": "最终故事世界观或整体设定。"},
                    "siteSetting": {"type": "string", "description": "最终主要地点或场景设定。"},
                    "plotOutline": {"type": "string", "description": "最终剧情大纲。"}
                  },
                  "required": ["title", "storyMode", "storyIntro", "storySetting", "siteSetting", "plotOutline"],
                  "additionalProperties": false
                }
              },
              "required": ["transferData"],
              "additionalProperties": false
            }
            """;

    private StoryContinueGenerationToolContract() {
    }

    /**
     * 构建注入模型的继续生成工具定义。
     */
    public static ToolSpecification buildSpecification() {
        JsonObjectSchema transferDataSchema = JsonObjectSchema.builder()
                .description("传递给后续故事背景与场景节点的最终故事核心数据")
                .addStringProperty("title", "最终故事标题")
                .addStringProperty("storyMode", "最终故事模式，使用 normal 或 chapter")
                .addStringProperty("storyIntro", "最终故事简介")
                .addStringProperty("storySetting", "最终故事世界观或整体设定")
                .addStringProperty("siteSetting", "最终主要地点或场景设定")
                .addStringProperty("plotOutline", "最终剧情大纲")
                .required(TRANSFER_FIELDS)
                .additionalProperties(false)
                .build();
        JsonObjectSchema schema = JsonObjectSchema.builder()
                .addProperty(TRANSFER_DATA, transferDataSchema)
                .required(TRANSFER_DATA)
                .additionalProperties(false)
                .build();
        return ToolSpecification.builder()
                .name(StoryTaskToolSpec.STORY_CONTINUE_GENERATION)
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
     * 提取并校验六项故事核心字段。
     */
    public static Map<String, Object> requireTransferData(Map<String, Object> arguments) {
        Object rawTransferData = arguments == null ? null : arguments.get(TRANSFER_DATA);
        if (!(rawTransferData instanceof Map<?, ?> rawMap)) {
            throw new IllegalArgumentException("transferData 必须是包含故事核心字段的对象");
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
