package org.jeecg.modules.airag.agent.subagent.story.tool;

import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.model.chat.request.json.JsonArraySchema;
import dev.langchain4j.model.chat.request.json.JsonObjectSchema;
import org.jeecg.common.util.oConvertUtils;
import org.jeecg.modules.airag.agent.runtime.AgentContext;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 完整故事异步生成工具契约。
 */
public final class StoryGenerateCompleteToolContract {
    public static final String TRANSFER_DATA = "transferData";
    public static final String TRANSFER_DATA_JSON = "transferDataJson";
    public static final String ROLES = "roles";
    public static final String ATTR_GENERATION_ACCEPTED = "storyGenerateCompleteAccepted";
    public static final String ATTR_GENERATION_TASK_ID = "storyGenerateCompleteTaskId";
    public static final String ATTR_GENERATION_EVENT_ID = "storyGenerateCompleteEventId";

    private static final List<String> STORY_FIELDS = List.of(
            "title",
            "storySetting",
            "siteSetting",
            "plotOutline"
    );
    private static final List<String> ROLE_FIELDS = List.of(
            "roleName",
            "gender",
            "occupation",
            "backgroundStory"
    );
    private static final String TOOL_DESCRIPTION =
            "只有故事核心内容和新角色列表全部完整，并且用户明确表示满意时才调用。"
                    + "该工具会异步创建全部角色、生成故事后续内容并保存故事及角色关联。";
    private static final String INPUT_SCHEMA = """
            {
              "type": "object",
              "properties": {
                "transferData": {
                  "type": "object",
                  "description": "用户最终确认的故事核心数据和新角色列表。",
                  "properties": {
                    "title": {"type": "string", "description": "最终故事标题。"},
                    "storySetting": {"type": "string", "description": "最终故事世界观或整体设定。"},
                    "siteSetting": {"type": "string", "description": "最终主要地点或场景设定。"},
                    "plotOutline": {"type": "string", "description": "最终剧情大纲。"},
                    "roles": {
                      "type": "array",
                      "description": "本次故事需要新建的角色列表。",
                      "minItems": 1,
                      "items": {
                        "type": "object",
                        "properties": {
                          "roleName": {"type": "string", "description": "最终角色名称。"},
                          "gender": {"type": "string", "description": "最终角色性别。"},
                          "occupation": {"type": "string", "description": "最终角色职业或身份。"},
                          "backgroundStory": {"type": "string", "description": "最终角色背景故事。"}
                        },
                        "required": ["roleName", "gender", "occupation", "backgroundStory"],
                        "additionalProperties": false
                      }
                    }
                  },
                  "required": ["title", "storySetting", "siteSetting", "plotOutline", "roles"],
                  "additionalProperties": false
                }
              },
              "required": ["transferData"],
              "additionalProperties": false
            }
            """;

    private StoryGenerateCompleteToolContract() {
    }

    public static ToolSpecification buildSpecification() {
        JsonObjectSchema roleSchema = JsonObjectSchema.builder()
                .description("用户最终确认的新角色核心数据")
                .addStringProperty("roleName", "最终角色名称")
                .addStringProperty("gender", "最终角色性别")
                .addStringProperty("occupation", "最终角色职业或身份")
                .addStringProperty("backgroundStory", "最终角色背景故事")
                .required(ROLE_FIELDS)
                .additionalProperties(false)
                .build();
        JsonArraySchema rolesSchema = JsonArraySchema.builder()
                .description("本次故事需要新建的角色列表")
                .items(roleSchema)
                .build();
        JsonObjectSchema transferDataSchema = JsonObjectSchema.builder()
                .description("用户最终确认的故事核心数据和新角色列表")
                .addStringProperty("title", "最终故事标题")
                .addStringProperty("storySetting", "最终故事世界观或整体设定")
                .addStringProperty("siteSetting", "最终主要地点或场景设定")
                .addStringProperty("plotOutline", "最终剧情大纲")
                .addProperty(ROLES, rolesSchema)
                .required(List.of(
                        "title",
                        "storySetting",
                        "siteSetting",
                        "plotOutline",
                        ROLES
                ))
                .additionalProperties(false)
                .build();
        JsonObjectSchema schema = JsonObjectSchema.builder()
                .addProperty(TRANSFER_DATA, transferDataSchema)
                .required(TRANSFER_DATA)
                .additionalProperties(false)
                .build();
        return ToolSpecification.builder()
                .name(StoryTaskToolSpec.STORY_GENERATE_COMPLETE)
                .description(TOOL_DESCRIPTION)
                .parameters(schema)
                .build();
    }

    public static String inputSchema() {
        return INPUT_SCHEMA;
    }

    public static Map<String, Object> requireTransferData(Map<String, Object> arguments) {
        Object rawTransferData = arguments == null ? null : arguments.get(TRANSFER_DATA);
        if (!(rawTransferData instanceof Map<?, ?> rawMap)) {
            throw new IllegalArgumentException("transferData 必须是包含故事核心字段和角色列表的对象");
        }
        Map<String, Object> transferData = new LinkedHashMap<>();
        for (String field : STORY_FIELDS) {
            String value = normalize(rawMap.get(field));
            if (!oConvertUtils.isNotEmpty(value)) {
                throw new IllegalArgumentException("transferData." + field + " 不能为空");
            }
            transferData.put(field, value);
        }
        transferData.put(ROLES, requireRoles(rawMap.get(ROLES)));
        return transferData;
    }

    public static void markAccepted(AgentContext context,
                                    String taskId,
                                    String eventId,
                                    String transferDataJson) {
        if (context == null) {
            return;
        }
        context.putAttribute(ATTR_GENERATION_ACCEPTED, Boolean.TRUE);
        context.putAttribute(ATTR_GENERATION_TASK_ID, taskId);
        context.putAttribute(ATTR_GENERATION_EVENT_ID, eventId);
        context.putAttribute(TRANSFER_DATA_JSON, transferDataJson);
    }

    public static boolean consumeAccepted(AgentContext context) {
        if (context == null || !Boolean.TRUE.equals(context.getAttribute(ATTR_GENERATION_ACCEPTED))) {
            return false;
        }
        context.removeAttribute(ATTR_GENERATION_ACCEPTED);
        return true;
    }

    private static List<Map<String, Object>> requireRoles(Object rawRoles) {
        if (!(rawRoles instanceof List<?> roleList) || roleList.isEmpty()) {
            throw new IllegalArgumentException("transferData.roles 不能为空");
        }
        List<Map<String, Object>> roles = new ArrayList<>(roleList.size());
        for (int index = 0; index < roleList.size(); index++) {
            Object rawRole = roleList.get(index);
            if (!(rawRole instanceof Map<?, ?> rawRoleMap)) {
                throw new IllegalArgumentException("transferData.roles[" + index + "] 必须是角色对象");
            }
            Map<String, Object> role = new LinkedHashMap<>();
            for (String field : ROLE_FIELDS) {
                String value = normalize(rawRoleMap.get(field));
                if (!oConvertUtils.isNotEmpty(value)) {
                    throw new IllegalArgumentException(
                            "transferData.roles[" + index + "]." + field + " 不能为空"
                    );
                }
                role.put(field, value);
            }
            roles.add(role);
        }
        return roles;
    }

    private static String normalize(Object value) {
        String text = oConvertUtils.getString(value);
        return text == null || text.isBlank() ? null : text.trim();
    }
}
