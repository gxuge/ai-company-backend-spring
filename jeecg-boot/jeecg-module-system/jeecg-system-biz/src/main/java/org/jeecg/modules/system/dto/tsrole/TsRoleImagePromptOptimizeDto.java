package org.jeecg.modules.system.dto.tsrole;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class TsRoleImagePromptOptimizeDto {
    @NotBlank(message = "提示词不能为空")
    private String promptText;

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
