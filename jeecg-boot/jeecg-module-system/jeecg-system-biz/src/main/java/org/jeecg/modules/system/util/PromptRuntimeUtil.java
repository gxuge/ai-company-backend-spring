package org.jeecg.modules.system.util;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.JSONArray;
import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.modules.airag.prompts.service.IAiragPromptTemplateService;
import org.jeecg.modules.airag.prompts.vo.AiragPromptTemplateVo;
import org.jeecg.modules.openapi.service.IPromptChatService;
import org.jeecg.modules.openapi.vo.PromptRenderedSectionsVo;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Runtime helpers for prompt rendering and model JSON parsing.
 */
public class PromptRuntimeUtil {

    private PromptRuntimeUtil() {
    }

    public static String buildPrompt(IAiragPromptTemplateService templateService,
                                     String code,
                                     String version,
                                     Map<String, String> variables) {
        AiragPromptTemplateVo template = templateService.getTemplate(code, version);
        String developerPrompt = trimToEmpty(template.getSections().get("developer_prompt"));
        String userPrompt = trimToEmpty(templateService.renderSection(code, version, "user_prompt_template", variables));
        String outputSchemaHint = trimToEmpty(template.getSections().get("output_schema_hint"));
        return developerPrompt + "\n\n" + userPrompt + "\n\n" + outputSchemaHint;
    }

    public static JSONObject callPromptChat(IPromptChatService promptChatService, String prompt) {
        String rawContent = callChatContent(promptChatService, prompt);
        return parseWithOneRepair(rawContent, promptChatService, false);
    }

    public static JSONObject callPromptChat(IPromptChatService promptChatService, PromptRenderedSectionsVo sections) {
        String rawContent = callChatContent(promptChatService, sections);
        return parseWithOneRepair(rawContent, promptChatService, true);
    }

    private static JSONObject parseWithOneRepair(String rawContent,
                                                 IPromptChatService promptChatService,
                                                 boolean toolCallLogMode) {
        try {
            JSONObject parsed = parseJsonObject(rawContent);
            return parsed;
        } catch (JeecgBootException firstEx) {
            String repairPrompt = buildJsonRepairPrompt(rawContent);
            String repairedContent = callChatContent(promptChatService, repairPrompt);
            try {
                JSONObject repairedParsed = parseJsonObject(repairedContent);
                return repairedParsed;
            } catch (JeecgBootException ignored) {
                throw new JeecgBootException("AI回复解析失败，非有效JSON");
            }
        }
    }

    public static JSONObject sanitizeToolCallLogJson(JSONObject source) {
        return sanitizeToolCallLogJson(source, true);
    }

    private static JSONObject sanitizeToolCallLogJson(JSONObject source, boolean enabled) {
        if (source == null) {
            return new JSONObject();
        }
        if (!enabled) {
            return source;
        }
        Object sanitized = removeThinkRecursively(source);
        return sanitized instanceof JSONObject ? (JSONObject) sanitized : source;
    }

    private static Object removeThinkRecursively(Object node) {
        if (node instanceof JSONObject jsonObject) {
            JSONObject sanitized = new JSONObject();
            for (Map.Entry<String, Object> entry : jsonObject.entrySet()) {
                String key = entry.getKey();
                if ("think".equalsIgnoreCase(key)) {
                    continue;
                }
                sanitized.put(key, removeThinkRecursively(entry.getValue()));
            }
            return sanitized;
        }
        if (node instanceof JSONArray jsonArray) {
            JSONArray sanitized = new JSONArray();
            for (Object item : jsonArray) {
                sanitized.add(removeThinkRecursively(item));
            }
            return sanitized;
        }
        return node;
    }

    public static JSONObject parseJsonObject(String rawContent) {
        String content = normalizeRawContent(rawContent);
        int start = content.indexOf('{');
        int end = content.lastIndexOf('}');
        if (start >= 0 && end > start) {
            content = content.substring(start, end + 1);
        }
        try {
            return JSONObject.parseObject(content);
        } catch (Exception ex) {
            throw new JeecgBootException("AI回复解析失败，非有效JSON");
        }
    }

    public static Map<String, String> buildSettingVars(String roleName,
                                                        String gender,
                                                        String occupation,
                                                        String backgroundStory,
                                                        String greeting,
                                                        String styleHint,
                                                        String keywords,
                                                        String extraInfo) {
        Map<String, String> variables = new HashMap<>();
        variables.put("role_name", nullableToken(roleName));
        variables.put("gender", nullableToken(gender));
        variables.put("occupation", nullableToken(occupation));
        variables.put("background_story", nullableToken(backgroundStory));
        variables.put("greeting", nullableToken(greeting));
        variables.put("style_hint", nullableToken(styleHint));
        variables.put("keywords", nullableToken(keywords));
        variables.put("extra_info", nullableToken(extraInfo));
        return variables;
    }

    public static Map<String, String> buildImageVars(String roleName,
                                                     String gender,
                                                     String occupation,
                                                     String backgroundStory,
                                                     String styleName,
                                                     String aspectRatio,
                                                     String referenceImageUrl) {
        Map<String, String> variables = new HashMap<>();
        variables.put("role_name", nullableToken(roleName));
        variables.put("gender", nullableToken(gender));
        variables.put("occupation", nullableToken(occupation));
        variables.put("background_story", nullableToken(backgroundStory));
        variables.put("style_name", nullableToken(styleName));
        variables.put("aspect_ratio", nullableToken(aspectRatio));
        variables.put("reference_image_url", nullableToken(referenceImageUrl));
        return variables;
    }

    public static Map<String, String> buildVoiceVars(String roleName,
                                                     String gender,
                                                     String occupation,
                                                     String backgroundStory,
                                                     String preferredVoiceName,
                                                     String targetTone,
                                                     String previewText) {
        Map<String, String> variables = new HashMap<>();
        variables.put("role_name", nullableToken(roleName));
        variables.put("gender", nullableToken(gender));
        variables.put("occupation", nullableToken(occupation));
        variables.put("background_story", nullableToken(backgroundStory));
        variables.put("preferred_voice_name", nullableToken(preferredVoiceName));
        variables.put("target_tone", nullableToken(targetTone));
        variables.put("preview_text", nullableToken(previewText));
        return variables;
    }

    public static Map<String, String> buildGenerateRoleVars(String storySetting, String storyBackground) {
        Map<String, String> variables = new HashMap<>();
        variables.put("story_setting", nullableToken(storySetting));
        variables.put("story_background", nullableToken(storyBackground));
        return variables;
    }

    public static String nullableToken(String value) {
        String trimmed = trimToNull(value);
        return trimmed == null ? "null" : trimmed;
    }

    public static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    public static String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value.trim();
            }
        }
        return null;
    }

    public static String normalizeGender(String value) {
        String normalized = trimToNull(value);
        if (!StringUtils.hasText(normalized)) {
            return null;
        }
        String lower = normalized.toLowerCase();
        if (Arrays.asList("male", "female", "unknown").contains(lower)) {
            return lower;
        }
        if ("random".equals(lower)) {
            return null;
        }
        return null;
    }

    private static String callChatContent(IPromptChatService promptChatService, String prompt) {
        String rawContent = promptChatService == null ? null : trimToNull(promptChatService.chat(prompt));
        if (!StringUtils.hasText(rawContent)) {
            throw new JeecgBootException("AI回复为空");
        }
        return rawContent;
    }

    private static String callChatContent(IPromptChatService promptChatService, PromptRenderedSectionsVo sections) {
        String rawContent = null;
        if (promptChatService != null && sections != null) {
            rawContent = trimToNull(promptChatService.chatToolCall(
                    sections.getDeveloperPrompt(),
                    sections.getUserPrompt(),
                    sections.getToolSchema()));
        }
        if (!StringUtils.hasText(rawContent)) {
            throw new JeecgBootException("AI回复为空");
        }
        return rawContent;
    }

    private static String buildJsonRepairPrompt(String rawContent) {
        return "你是JSON修复器。请把下面文本修复为一个合法的JSON对象，只输出JSON对象本身，不要解释，不要Markdown代码块。\n"
                + rawContent;
    }

    private static String normalizeRawContent(String rawContent) {
        if (rawContent == null) {
            throw new JeecgBootException("AI回复为空");
        }
        String content = rawContent.trim();
        if (content.startsWith("```")) {
            int firstLineEnd = content.indexOf('\n');
            if (firstLineEnd > -1) {
                content = content.substring(firstLineEnd + 1);
            }
            if (content.endsWith("```")) {
                content = content.substring(0, content.length() - 3);
            }
            content = content.trim();
        }
        return content;
    }

    private static String trimToEmpty(String value) {
        String trimmed = trimToNull(value);
        return trimmed == null ? "" : trimmed;
    }
}
