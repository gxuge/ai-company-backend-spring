package org.jeecg.modules.system.dto.tsstory;

import lombok.Data;
import org.springframework.util.StringUtils;

/**
 * 故事场景背景图片生成请求。
 */
@Data
public class TsStoryOneClickSceneImageGenerateDto {
    /** 故事标题。 */
    private String title;
    /** 故事世界观与整体设定。 */
    private String storySetting;
    /** 故事主要地点或场景设定。 */
    private String siteSetting;
    /** 剧情大纲，用于补充场景所处阶段。 */
    private String plotOutline;
    /** 视觉风格名称，默认写实影视级场景概念图。 */
    private String styleName;
    /** 图片宽高比，默认9:16。 */
    private String aspectRatio;
    /** 参考图片地址，可选。 */
    private String referenceImageUrl;

    /**
     * 清理输入并补充生图默认值。
     */
    public void normalize() {
        this.title = trimToNull(this.title);
        this.storySetting = trimToNull(this.storySetting);
        this.siteSetting = trimToNull(this.siteSetting);
        this.plotOutline = trimToNull(this.plotOutline);
        this.styleName = defaultIfBlank(this.styleName, "写实影视级场景概念图");
        this.aspectRatio = defaultIfBlank(this.aspectRatio, "9:16");
        this.referenceImageUrl = trimToNull(this.referenceImageUrl);
    }

    /**
     * 判断是否提供了可用于生成场景的核心设定。
     */
    public boolean hasSceneContext() {
        return StringUtils.hasText(this.storySetting) || StringUtils.hasText(this.siteSetting);
    }

    private static String defaultIfBlank(String value, String defaultValue) {
        String normalized = trimToNull(value);
        return StringUtils.hasText(normalized) ? normalized : defaultValue;
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
