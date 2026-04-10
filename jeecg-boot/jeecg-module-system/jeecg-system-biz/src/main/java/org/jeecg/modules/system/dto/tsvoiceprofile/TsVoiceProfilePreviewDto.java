package org.jeecg.modules.system.dto.tsvoiceprofile;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class TsVoiceProfilePreviewDto {
    @NotNull(message = "voiceProfileId不能为空")
    private Long voiceProfileId;
    private String previewText;

    public void normalize() {
        this.previewText = trimToNull(this.previewText);
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
