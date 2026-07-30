package org.jeecg.modules.airag.agent.subagent.story.tool;

import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.model.chat.request.json.JsonObjectSchema;
import org.jeecg.common.util.oConvertUtils;
import org.jeecg.modules.airag.agent.error.AgentErrorCode;
import org.jeecg.modules.airag.agent.error.AgentErrorException;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 故事确认展示工具契约。
 *
 * <p>只维护前端确认区域的动态文案，不承载故事数据和流程跳转。</p>
 *
 * @author codex
 * @date 2026/7/28
 */
public final class StoryConfirmationToolContract {
    public static final String QUESTION = "question";
    public static final String CONFIRM_LABEL = "confirmLabel";
    public static final String REVISE_LABEL = "reviseLabel";

    private static final int MAX_COPY_LENGTH = 12;
    private static final String COPY_REQUIREMENT = "文案由你根据当前对话自行决定，语气亲切自然，可以带符号或表情，不超过12个字。";
    private static final String TOOL_DESCRIPTION =
            "故事六项核心设定整理完成后，用于向用户展示确认问题和两个候选文案。"
                    + "本工具只展示选项，不表示用户已经同意继续，也不会进入后续生成节点。";
    private static final String INPUT_SCHEMA = """
            {
              "type": "object",
              "properties": {
                "question": {
                  "type": "string",
                  "maxLength": 12,
                  "description": "询问用户是否喜欢当前故事版本。文案由你自行决定，语气亲切自然，可以带符号或表情，不超过12个字。"
                },
                "confirmLabel": {
                  "type": "string",
                  "maxLength": 12,
                  "description": "表达用户满意当前结果的候选文案。文案由你自行决定，语气亲切自然，可以带符号或表情，不超过12个字。"
                },
                "reviseLabel": {
                  "type": "string",
                  "maxLength": 12,
                  "description": "表达用户希望继续调整的候选文案。文案由你自行决定，语气亲切自然，可以带符号或表情，不超过12个字。"
                }
              },
              "required": ["question", "confirmLabel", "reviseLabel"],
              "additionalProperties": false
            }
            """;

    private StoryConfirmationToolContract() {
    }

    /**
     * 构建注入模型的故事确认展示工具。
     */
    public static ToolSpecification buildSpecification() {
        JsonObjectSchema schema = JsonObjectSchema.builder()
                .addStringProperty(QUESTION, "询问用户是否喜欢当前故事版本。" + COPY_REQUIREMENT)
                .addStringProperty(CONFIRM_LABEL, "表达用户满意当前结果的候选文案。" + COPY_REQUIREMENT)
                .addStringProperty(REVISE_LABEL, "表达用户希望继续调整的候选文案。" + COPY_REQUIREMENT)
                .required(QUESTION, CONFIRM_LABEL, REVISE_LABEL)
                .additionalProperties(false)
                .build();
        return ToolSpecification.builder()
                .name(StoryTaskToolSpec.STORY_REQUEST_CONFIRMATION)
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
     * 提取并校验三个动态展示文案。
     */
    public static Map<String, String> requireDisplayCopy(Map<String, Object> arguments) {
        Map<String, String> copy = new LinkedHashMap<>();
        copy.put(QUESTION, requireCopy(arguments, QUESTION));
        copy.put(CONFIRM_LABEL, requireCopy(arguments, CONFIRM_LABEL));
        copy.put(REVISE_LABEL, requireCopy(arguments, REVISE_LABEL));
        return copy;
    }

    private static String requireCopy(Map<String, Object> arguments, String fieldName) {
        String value = normalize(arguments == null ? null : arguments.get(fieldName));
        if (!oConvertUtils.isNotEmpty(value)) {
            throw new AgentErrorException(
                    AgentErrorCode.TOOL_STORY_CONFIRMATION_REQUIRED_FIELD_MISSING,
                    Map.of("field", fieldName)
            );
        }
        if (value.codePointCount(0, value.length()) > MAX_COPY_LENGTH) {
            throw new AgentErrorException(
                    AgentErrorCode.TOOL_STORY_CONFIRMATION_FIELD_TOO_LONG,
                    Map.of("field", fieldName, "maxLength", MAX_COPY_LENGTH)
            );
        }
        return value;
    }

    private static String normalize(Object value) {
        String text = oConvertUtils.getString(value);
        return text == null || text.isBlank() ? null : text.trim();
    }
}
