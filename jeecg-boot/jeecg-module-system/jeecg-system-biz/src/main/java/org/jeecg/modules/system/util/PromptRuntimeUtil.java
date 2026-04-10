package org.jeecg.modules.system.util;

import com.alibaba.fastjson.JSONObject;
import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.modules.airag.prompts.service.IAiragPromptTemplateService;
import org.jeecg.modules.airag.prompts.vo.AiragPromptTemplateVo;
import org.jeecg.modules.openapi.service.IMiniMaxDemoService;
import org.jeecg.modules.openapi.dto.MiniMaxChatRequestDto;
import org.jeecg.modules.openapi.vo.MiniMaxChatResponseVo;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Prompt 运行时工具类。
 * 用途：统一封装“模板渲染 + LLM 调用 + JSON 解析”流程，避免业务代码重复。
 */
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
     * 调用 MiniMax 文本模型并解析为 JSON。
     */
    public static JSONObject callPromptChat(IMiniMaxDemoService miniMaxDemoService, String prompt) {
        MiniMaxChatRequestDto request = new MiniMaxChatRequestDto();
        request.setPrompt(prompt);
        MiniMaxChatResponseVo response = miniMaxDemoService.chat(request);
        String rawContent = response == null ? null : trimToNull(response.getContent());
        if (!StringUtils.hasText(rawContent)) {
            throw new JeecgBootException("AI回复为空");
        }
        return parseJsonObject(rawContent);
    }

    /**
     * 兼容代码块包裹等情况，提取并解析 JSON 对象。
     */
    public static JSONObject parseJsonObject(String rawContent) {
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
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    /**
     * 返回第一个非空白字符串。
     */
    public static String firstNonBlank(String... values) {
        if (values == null) return null;
        for (String value : values) {
            if (StringUtils.hasText(value)) return value.trim();
        }
        return null;
    }

    /**
     * 性别标准化，仅保留 male/female/unknown。
     */
    public static String normalizeGender(String value) {
        String normalized = trimToNull(value);
        if (!StringUtils.hasText(normalized)) return null;
        String lower = normalized.toLowerCase();
        if (Arrays.asList("male", "female", "unknown").contains(lower)) return lower;
        if ("random".equals(lower)) return null;
        return null;
    }

    private static String trimToEmpty(String value) {
        String trimmed = trimToNull(value);
        return trimmed == null ? "" : trimmed;
    }
}
