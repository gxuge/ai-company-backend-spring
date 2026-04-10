package org.jeecg.modules.system.dto.tsstory;

import lombok.Data;
import org.springframework.util.StringUtils;

@Data
public class TsStoryOneClickSettingGenerateDto {
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

    public void normalize() {
        this.title = trimToNull(this.title);
        this.storyMode = normalizeStoryMode(this.storyMode);
        this.storyIntro = trimToNull(this.storyIntro);
        this.storySetting = trimToNull(this.storySetting);
        this.storyBackground = trimToNull(this.storyBackground);
        this.ideaInput = trimToNull(this.ideaInput);
        this.styleHint = trimToNull(this.styleHint);
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
}
