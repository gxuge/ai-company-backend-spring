package org.jeecg.modules.system.dto.tsstory;

import lombok.Data;
import org.springframework.util.StringUtils;

@Data
public class TsStoryOneClickOutlineGenerateDto {
    /** Optional story id for caller context. */
    private Long storyId;
    /** Story title for outline context. */
    private String title;
    /** Story mode: normal/chapter. */
    private String storyMode;
    /** Story setting, can be null. */
    private String storySetting;
    /** Scene setting, can be null. */
    private String sceneSetting;
    /** Story intro text. */
    private String storyIntro;
    /** Existing plot outline text. */
    private String plotOutline;
    /** Extra requirements from caller. */
    private String extraRequirements;

    public void normalize() {
        this.title = trimToNull(this.title);
        this.storyMode = normalizeStoryMode(this.storyMode);
        this.storySetting = trimToNull(this.storySetting);
        this.sceneSetting = trimToNull(this.sceneSetting);
        this.storyIntro = trimToNull(this.storyIntro);
        this.plotOutline = trimToNull(this.plotOutline);
        this.extraRequirements = trimToNull(this.extraRequirements);
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
