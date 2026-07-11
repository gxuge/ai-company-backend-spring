package org.jeecg.modules.system.dto.tsstory;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import org.jeecg.modules.system.util.StoryPromptGenerateUtil;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class TsStoryFullGenerateDto {
    /** 故事标题。 */
    private String title;
    /** 故事模式。 */
    private String storyMode;
    /** 故事简介。 */
    private String storyIntro;
    /** 故事设定。 */
    private String storySetting;
    /** 场景设定。 */
    private String siteSetting;
    /** 剧情大纲。 */
    private String plotOutline;

    public void normalize() {
        this.title = trimToNull(this.title);
        this.storyMode = StoryPromptGenerateUtil.normalizeStoryMode(this.storyMode);
        this.storyIntro = trimToNull(this.storyIntro);
        this.storySetting = trimToNull(this.storySetting);
        this.siteSetting = trimToNull(this.siteSetting);
        this.plotOutline = trimToNull(this.plotOutline);
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
