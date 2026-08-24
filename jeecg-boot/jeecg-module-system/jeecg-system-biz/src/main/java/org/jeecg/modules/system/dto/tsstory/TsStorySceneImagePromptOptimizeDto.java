package org.jeecg.modules.system.dto.tsstory;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 故事场景图片提示词润色请求。
 */
@Data
public class TsStorySceneImagePromptOptimizeDto {
    /** 当前故事场景描述或生图提示词。 */
    @NotBlank(message = "提示词不能为空")
    private String promptText;
    /** 时间选项及其英文视觉描述。 */
    private TsStorySceneOptionDto time;
    /** 天气选项及其英文视觉描述。 */
    private TsStorySceneOptionDto weather;
    /** 气氛选项及其英文视觉描述。 */
    private TsStorySceneOptionDto mood;

    /**
     * 清理提示词输入。
     */
    public void normalize() {
        this.promptText = trimToNull(this.promptText);
        this.time = TsStorySceneOptionDto.normalize(this.time, TsStorySceneOptionDto.TIME_KEYS);
        this.weather = TsStorySceneOptionDto.normalize(
                this.weather, TsStorySceneOptionDto.WEATHER_KEYS);
        this.mood = TsStorySceneOptionDto.normalize(this.mood, TsStorySceneOptionDto.MOOD_KEYS);
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
