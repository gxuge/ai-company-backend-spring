package org.jeecg.modules.system.dto.tsstory;

import lombok.Data;
import org.springframework.util.StringUtils;

@Data
public class TsStoryFullGenerateDto {
    /** 可选故事ID，仅用于调用上下文。 */
    private Long storyId;
    /** 故事模式：normal/chapter。 */
    private String storyMode;
    /** 用户提供的模板文本（包含 {{ value }} 占位符）。 */
    private String templateText;
    /** 额外要求。 */
    private String extraRequirements;
    /** chapter 模式下是否跳过大纲生成，默认 true。 */
    private Boolean skipOutlineWhenChapter;

    public void normalize() {
        this.storyMode = normalizeStoryMode(this.storyMode);
        this.templateText = trimToNull(this.templateText);
        this.extraRequirements = trimToNull(this.extraRequirements);
        if (this.skipOutlineWhenChapter == null) {
            this.skipOutlineWhenChapter = Boolean.TRUE;
        }
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

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
