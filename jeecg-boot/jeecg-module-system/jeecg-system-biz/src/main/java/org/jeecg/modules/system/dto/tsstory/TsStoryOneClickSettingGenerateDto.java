package org.jeecg.modules.system.dto.tsstory;

import lombok.Data;
import org.springframework.util.StringUtils;

@Data
public class TsStoryOneClickSettingGenerateDto {
    public static final String TEMPLATE_MODE_CORE = "core";
    public static final String TEMPLATE_MODE_SETTING_OPTIMIZE = "setting_optimize";

    /** Optional existing story id, currently only for caller context. */
    private Long storyId;
    /** Story title hint. */
    private String title;
    /** Story mode: normal/chapter. */
    private String storyMode;
    /** Existing intro text. */
    private String storyIntro;
    /** Existing story setting text. */
    private String storySetting;
    /** Existing story background text. */
    private String storyBackground;
    /** User free-form idea input. */
    private String ideaInput;
    /** Optional style hint. */
    private String styleHint;
    /** 模板模式：core（默认）/setting_optimize（仅优化故事设定） */
    private String templateMode;

    public void normalize() {
        this.title = trimToNull(this.title);
        this.storyMode = normalizeStoryMode(this.storyMode);
        this.storyIntro = trimToNull(this.storyIntro);
        this.storySetting = trimToNull(this.storySetting);
        this.storyBackground = trimToNull(this.storyBackground);
        this.ideaInput = trimToNull(this.ideaInput);
        this.styleHint = trimToNull(this.styleHint);
        this.templateMode = normalizeTemplateMode(this.templateMode);
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static String normalizeStoryMode(String value) {
        String normalized = trimToNull(value);
        if (!StringUtils.hasText(normalized)) {
            return null;
        }
        String lower = normalized.toLowerCase();
        if ("chapter".equals(lower) || "normal".equals(lower)) {
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
        if (TEMPLATE_MODE_SETTING_OPTIMIZE.equals(lower)) {
            return TEMPLATE_MODE_SETTING_OPTIMIZE;
        }
        return TEMPLATE_MODE_CORE;
    }

    public boolean isSettingOptimizeMode() {
        return TEMPLATE_MODE_SETTING_OPTIMIZE.equals(this.templateMode);
    }
}
