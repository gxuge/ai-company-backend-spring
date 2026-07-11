package org.jeecg.modules.system.dto.tsrole;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class TsRoleGenerateImagePromptByTemplateDto {
    /**
     * 用户已有的角色形象提示词。
     */
    @NotBlank(message = "提示词不能为空")
    private String promptText;

    /**
     * 风格参数，例如：写实、二次元、国风、赛博朋克、3D、油画。
     */
    private String styleName;

    public void normalize() {
        this.promptText = trimToNull(this.promptText);
        this.styleName = trimToNull(this.styleName);
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
