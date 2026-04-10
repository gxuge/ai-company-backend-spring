package org.jeecg.modules.system.dto.tsstory;

import lombok.Data;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

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
    /** Story background, can be null. */
    private String storyBackground;
    /** Expected chapter count, default 3. */
    private Integer chapterCount;
    /** Optional involved role names. */
    private List<String> roleNames;
    /** Extra requirements from caller. */
    private String extraRequirements;

    public void normalize() {
        this.title = trimToNull(this.title);
        this.storyMode = normalizeStoryMode(this.storyMode);
        this.storySetting = trimToNull(this.storySetting);
        this.sceneSetting = trimToNull(this.sceneSetting);
        this.storyBackground = trimToNull(this.storyBackground);
        this.extraRequirements = trimToNull(this.extraRequirements);
        if (this.chapterCount == null || this.chapterCount <= 0) {
            this.chapterCount = 3;
        } else if (this.chapterCount > 12) {
            this.chapterCount = 12;
        }
        this.roleNames = normalizeRoleNames(this.roleNames);
    }

    private static List<String> normalizeRoleNames(List<String> values) {
        if (values == null || values.isEmpty()) {
            return new ArrayList<>();
        }
        Set<String> unique = new LinkedHashSet<>();
        for (String value : values) {
            String trimmed = trimToNull(value);
            if (trimmed != null) {
                unique.add(trimmed);
            }
        }
        return new ArrayList<>(unique);
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
