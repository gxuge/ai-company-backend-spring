package org.jeecg.modules.system.dto.tsrole;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.util.StringUtils;

@Data
public class TsRoleGenerateImageByPromptDto {
    @NotBlank(message = "promptText不能为空")
    private String promptText;

    /**
     * 画风参数，可选。
     */
    private String styleName;

    /**
     * 参考图地址，可选。
     */
    private String referenceImageUrl;

    public void normalize() {
        this.promptText = trimToNull(this.promptText);
        this.styleName = trimToNull(this.styleName);
        this.referenceImageUrl = trimToNull(this.referenceImageUrl);
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return StringUtils.hasText(trimmed) ? trimmed : null;
    }
}
