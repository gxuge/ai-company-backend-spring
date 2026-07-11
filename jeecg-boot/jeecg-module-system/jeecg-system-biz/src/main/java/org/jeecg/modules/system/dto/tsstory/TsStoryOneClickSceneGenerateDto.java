package org.jeecg.modules.system.dto.tsstory;

import lombok.Data;
import org.springframework.util.StringUtils;

@Data
public class TsStoryOneClickSceneGenerateDto {
    public static final String TEMPLATE_MODE_CORE = "core";
    public static final String TEMPLATE_MODE_SITE_SETTING_OPTIMIZE = "site_setting_optimize";

    /** Story title for prompt context. */
    private String title;
    /** Story mode: normal/chapter. */
    private String storyMode;
    /** Story setting text. */
    private String storySetting;
    /** Story intro text. */
    private String storyIntro;
    /** Story background text. */
    private String storyBackground;
    /** Existing scene input. */
    private String sceneSetting;
    /** Existing plot outline text. */
    private String plotOutline;
    /** Optional style hint. */
    private String styleHint;
    /** 模板模式：core（默认）/site_setting_optimize（仅优化场景设定） */
    private String templateMode;

    public void normalize() {
        this.title = trimToNull(this.title);
        this.storyMode = normalizeStoryMode(this.storyMode);
        this.storySetting = trimToNull(this.storySetting);
        this.storyIntro = trimToNull(this.storyIntro);
        this.storyBackground = trimToNull(this.storyBackground);
        this.sceneSetting = trimToNull(this.sceneSetting);
        this.plotOutline = trimToNull(this.plotOutline);
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
        if (TEMPLATE_MODE_SITE_SETTING_OPTIMIZE.equals(lower)) {
            return TEMPLATE_MODE_SITE_SETTING_OPTIMIZE;
        }
        return TEMPLATE_MODE_CORE;
    }

    public boolean isSiteSettingOptimizeMode() {
        return TEMPLATE_MODE_SITE_SETTING_OPTIMIZE.equals(this.templateMode);
    }
}
