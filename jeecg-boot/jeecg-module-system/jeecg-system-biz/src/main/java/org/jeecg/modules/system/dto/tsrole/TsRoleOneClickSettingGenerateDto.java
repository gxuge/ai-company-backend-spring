package org.jeecg.modules.system.dto.tsrole;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;
import org.springframework.util.StringUtils;

@Data
public class TsRoleOneClickSettingGenerateDto {
    public static final String TEMPLATE_MODE_CORE = "core";
    public static final String TEMPLATE_MODE_BACKGROUND_OPTIMIZE = "background_optimize";
    public static final String TEMPLATE_MODE_GREETING_OPTIMIZE = "greeting_optimize";

    /** 角色ID（可选，传入时会校验归属） */
    private Long roleId;
    /** 角色名称 */
    private String roleName;
    /** 性别（male/female/unknown/random） */
    private String gender;
    /** 职业设定 */
    private String occupation;
    /** 角色背景故事 */
    private String backgroundStory;
    /** 角色开场白 */
    private String greeting;
    /** 风格提示词（可选） */
    private String styleHint;
    /** 关键词（可选） */
    private String keywords;
    /** 额外信息（可选，用于补充角色信息） */
    @JsonAlias("extra_info")
    private String extraInfo;
    /** 模板模式：core（默认）/background_optimize/greeting_optimize */
    private String templateMode;

    public void normalize() {
        this.roleName = trimToNull(this.roleName);
        this.gender = normalizeGender(this.gender);
        this.occupation = trimToNull(this.occupation);
        this.backgroundStory = trimToNull(this.backgroundStory);
        this.greeting = trimToNull(this.greeting);
        this.styleHint = trimToNull(this.styleHint);
        this.keywords = trimToNull(this.keywords);
        this.extraInfo = trimToNull(this.extraInfo);
        this.templateMode = normalizeTemplateMode(this.templateMode);
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static String normalizeGender(String value) {
        String normalized = trimToNull(value);
        if (!StringUtils.hasText(normalized)) {
            return null;
        }
        String lower = normalized.toLowerCase();
        if ("random".equals(lower)) {
            return null;
        }
        if ("male".equals(lower) || "female".equals(lower) || "unknown".equals(lower)) {
            return lower;
        }
        return null;
    }

    private static String normalizeTemplateMode(String value) {
        String normalized = trimToNull(value);
        if (!StringUtils.hasText(normalized)) {
            return TEMPLATE_MODE_CORE;
        }
        String lower = normalized.toLowerCase();
        if (TEMPLATE_MODE_BACKGROUND_OPTIMIZE.equals(lower)) {
            return TEMPLATE_MODE_BACKGROUND_OPTIMIZE;
        }
        if (TEMPLATE_MODE_GREETING_OPTIMIZE.equals(lower)) {
            return TEMPLATE_MODE_GREETING_OPTIMIZE;
        }
        return TEMPLATE_MODE_CORE;
    }

    public boolean isBackgroundOptimizeMode() {
        return TEMPLATE_MODE_BACKGROUND_OPTIMIZE.equals(this.templateMode);
    }

    public boolean isGreetingOptimizeMode() {
        return TEMPLATE_MODE_GREETING_OPTIMIZE.equals(this.templateMode);
    }
}
