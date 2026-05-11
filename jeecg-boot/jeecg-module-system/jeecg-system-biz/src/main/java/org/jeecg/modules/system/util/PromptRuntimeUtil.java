package org.jeecg.modules.system.util;

import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.modules.airag.prompts.service.IAiragPromptTemplateService;
import org.jeecg.modules.airag.prompts.vo.AiragPromptTemplateVo;
import org.jeecg.modules.openapi.service.IPromptChatService;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Prompt 运行时工具类。
 * 用途：统一封装“模板渲染 + LLM 调用 + JSON 解析”流程，避免业务代码重复。
 */
@Slf4j
public class PromptRuntimeUtil {
    private PromptRuntimeUtil() {
    }

    /**
     * 组装完整 Prompt（developer_prompt + user_prompt_template + output_schema_hint）。
     */
    public static String buildPrompt(IAiragPromptTemplateService templateService, String code, String version, Map<String, String> variables) {
        AiragPromptTemplateVo template = templateService.getTemplate(code, version);
        String developerPrompt = trimToEmpty(template.getSections().get("developer_prompt"));
        String userPrompt = trimToEmpty(templateService.renderSection(code, version, "user_prompt_template", variables));
        String outputSchemaHint = trimToEmpty(template.getSections().get("output_schema_hint"));
        return developerPrompt + "\n\n" + userPrompt + "\n\n" + outputSchemaHint;
    }

    /**
     * 调用文本模型并解析为 JSON。
     * 若首轮输出不是合法 JSON，会自动进行一次“JSON修复”重试。
     */
    public static JSONObject callPromptChat(IPromptChatService promptChatService, String prompt) {
        String rawContent = callChatContent(promptChatService, prompt);
        try {
            JSONObject parsed = parseJsonObject(rawContent);
            log.info("[PROMPT_CHAT_JSON] stage=first-pass content={}", parsed.toJSONString());
            return parsed;
        } catch (JeecgBootException firstEx) {
            log.warn("[PROMPT_CHAT_RAW] stage=first-pass-parse-fail raw={}", rawContent);
            String repairPrompt = buildJsonRepairPrompt(rawContent);
            String repairedContent = callChatContent(promptChatService, repairPrompt);
            try {
                JSONObject repairedParsed = parseJsonObject(repairedContent);
                log.info("[PROMPT_CHAT_JSON] stage=repair-pass content={}", repairedParsed.toJSONString());
                return repairedParsed;
            } catch (JeecgBootException ignored) {
                log.error("[PROMPT_CHAT_RAW] stage=repair-pass-parse-fail raw={} repaired={}", rawContent, repairedContent);
                throw new JeecgBootException("AI回复解析失败，非有效JSON");
            }
        }
    }

    /**
     * 兼容代码块包裹等情况，提取并解析 JSON 对象。
     */
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

    /**
     * 构建角色设定 Prompt 变量。
     */
    public static Map<String, String> buildSettingVars(String roleName, String gender, String occupation, String backgroundStory,
                                                       String styleHint, String keywords) {
        Map<String, String> variables = new HashMap<>();
        variables.put("role_name", nullableToken(roleName));
        variables.put("gender", nullableToken(gender));
        variables.put("occupation", nullableToken(occupation));
        variables.put("background_story", nullableToken(backgroundStory));
        variables.put("style_hint", nullableToken(styleHint));
        variables.put("keywords", nullableToken(keywords));
        return variables;
    }

    /**
     * 构建角色形象 Prompt 变量。
     */
    public static Map<String, String> buildImageVars(String roleName, String gender, String occupation, String backgroundStory,
                                                     String styleName, String aspectRatio, String referenceImageUrl) {
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

    /**
     * 构建角色声音 Prompt 变量。
     */
    public static Map<String, String> buildVoiceVars(String roleName, String gender, String occupation, String backgroundStory,
                                                     String preferredVoiceName, String targetTone, String previewText) {
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

    /**
     * 构建完整角色生成 Prompt 变量。
     */
    public static Map<String, String> buildGenerateRoleVars(String storySetting, String storyBackground) {
        Map<String, String> variables = new HashMap<>();
        variables.put("story_setting", nullableToken(storySetting));
        variables.put("story_background", nullableToken(storyBackground));
        return variables;
    }

    /**
     * 将空值转换为字面量 null，供模板显式判断。
     */
    public static String nullableToken(String value) {
        String trimmed = trimToNull(value);
        return trimmed == null ? "null" : trimmed;
    }

    /**
     * 去空白并转换为 null。
     */
    public static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    /**
     * 返回第一个非空白字符串。
     */
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

    /**
     * 性别标准化，仅保留 male/female/unknown。
     */
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

    private static String buildJsonRepairPrompt(String rawContent) {
        return "你是JSON修复器。请把下面文本修复为一个合法的JSON对象，只输出JSON对象本身，不要解释、不要Markdown代码块。\n"
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
