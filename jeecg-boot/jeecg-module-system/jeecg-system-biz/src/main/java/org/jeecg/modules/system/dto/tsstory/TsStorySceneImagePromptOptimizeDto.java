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
    /**
     * 清理提示词输入。
     */
    public void normalize() {
        this.promptText = trimToNull(this.promptText);
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
