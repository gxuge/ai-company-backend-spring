package org.jeecg.modules.system.dto.tsstory;

import lombok.Data;
import org.springframework.util.StringUtils;

@Data
public class TsStoryOneClickSceneGenerateDto {
    /** Story title for prompt context. */
    private String title;
    /** Story mode: normal/chapter. */
    private String storyMode;
    /** Story setting text. */
    private String storySetting;
    /** Story background text. */
    private String storyBackground;
    /** Existing scene input. */
    private String sceneSetting;
    /** Optional style hint. */
    private String styleHint;

    public void normalize() {
        this.title = trimToNull(this.title);
        this.storyMode = normalizeStoryMode(this.storyMode);
        this.storySetting = trimToNull(this.storySetting);
        this.storyBackground = trimToNull(this.storyBackground);
        this.sceneSetting = trimToNull(this.sceneSetting);
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
